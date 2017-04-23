package edu.susu.crypto;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.text.html.HTML;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.susu.database.DatabaseConnector;
import edu.susu.database.User;
import edu.susu.exception.*;

/**
 * Служба, принимающая первичные REST-запросы от пользователя и возвращающая веб-страницы или файлы
 * @author Carolus
 *
 */
@Path("/")
public class WebInterfaceService {

    static DatabaseConnector db; // инициализируется в ServiceContextListener
    private Set<User> currentUsers = new HashSet<User>();

    /**
     * Генерирует главную страницу сервиса
     * @return html-страница в строковом формате
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getHomePage() {
        return HTMLFactory.createHomePage();
    }

    /**
     * Генерирует личную страницу авторизованного пользователя
     * @param usr имя пользователя
     * @return html-страница в строковом формате
     */
    @Path("/{usr}")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getPersonalPage(@PathParam("usr") String usr) {
        return HTMLFactory.createUserPage(usr);
    }

    /**
     * Осуществляет процесс аутентификации пользователя
     * @return http-ответ с GET-перенаправлением на новую страницу
     * @throws URISyntaxException
     */
    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("username") String username, @FormParam("password")String password) throws URISyntaxException {
        try {
            authUser(username, password);
            return Response.seeOther(new URI(Routes.HOME + "/" + username)).build();
        } catch (UserDoesNotExistException usrEx) {
            return Response.seeOther(new URI(Routes.LOGIN + "?cause=nullUser")).build();
        } catch (PasswordMismatchException pswdEx) {
            return Response.seeOther(new URI(Routes.LOGIN + "?cause=passwordMismatch")).build();
        } catch (DatabaseUnreachableException e) {
            return Response.serverError().build();
        }
    }

    /**
     * Возвращает домашнюю страницу с указанием причины отказа во входе в сервис
     * @param errorCause
     * @return html-страница в строковом формате
     * @throws FailureCauseNotSpecifiedException если в запросе указана несуществующая причина отказа
     */
    @Path("/login")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String login(@QueryParam("cause") String errorCause) throws FailureCauseNotSpecifiedException {
        switch(errorCause) {
            case "nullUser": return HTMLFactory.createHomePage("Account with the given username does not exist.");
            case "passwordMismatch" : return HTMLFactory.createHomePage("Incorrect password.");
            default: throw new FailureCauseNotSpecifiedException();
        }
    }

    /**
     * Осуществляет процесс регистрации пользователя
     * @return http-ответ с GET-перенаправлением на страницу результата или ошибкой 500
     * @throws URISyntaxException
     */
    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@FormParam("username") String username, @FormParam("password")String password) throws URISyntaxException {
        try {
            registerUser(username, password);
            return Response.seeOther(new URI(Routes.REGISTER + "?cause=successful")).build();
        } catch (AlreadyExistsException ex) {
            return Response.seeOther(new URI(Routes.REGISTER + "?cause=alreadyExists")).build();
        } catch (DatabaseUnreachableException dbEx) {
            return Response.serverError().build();
        }
    }

    /**
     * Возвращает страницу регистрации
     * Если указана причина отказа в регистрации, помещает её в страницу
     * Если регистрация прошла успешно, возвращает домашнюю страницу с сообщением об успехе
     * @param errorCause
     * @return html-страница в строковом формате
     * @throws FailureCauseNotSpecifiedException если в запросе указана несуществующая причина отказа
     */
    @Path("/register")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String register(@QueryParam("cause") String errorCause) throws FailureCauseNotSpecifiedException {
        if (errorCause == null || errorCause.isEmpty())
            return HTMLFactory.createRegistrationPage();
        switch (errorCause) {
            case "alreadyExists":
                return HTMLFactory.createRegistrationPage("User with specified username already registered");
            case "successful":
                return HTMLFactory.createHomePage("Registration successful. You can now sign in");
            default:
                throw new FailureCauseNotSpecifiedException();
        }
    }

    /**
     * Проводит регистрацию пользователя в сервисе
     * @param name имя пользователя
     * @param password пароль
     * @throws AlreadyExistsException если пользователь с заданным именем уже существует
     * @throws DatabaseUnreachableException если не удалось подключиться к базе данных
     */
    private void registerUser(String name, String password) throws AlreadyExistsException, DatabaseUnreachableException {
        byte[] hash = getPasswordHash(password);
        if (db.isConnected()) {
            try {
                User entry = db.getUser(name);
                if (entry != null)
                    throw new AlreadyExistsException();
                else
                    db.addUser(name, hash);
            } catch (SQLException e) {
                throw new DatabaseUnreachableException();
            }
        } else throw new DatabaseUnreachableException();
    }

    /**
     * Производит авторизацию пользователя в сервисе
     * @param name имя пользователя
     * @param password пароль
     * @throws UserDoesNotExistException если пользователь не найден в базе данных
     * @throws PasswordMismatchException если пользователь найден, но введен неверный пароль
     * @throws DatabaseUnreachableException если не удалось подключиться к базе данных
     */
    private void authUser(String name, String password) throws UserDoesNotExistException, PasswordMismatchException, DatabaseUnreachableException {
        if (db.isConnected()) {
            try {
                User entry = db.getUser(name);
                if (entry == null)
                    throw new UserDoesNotExistException();
                if (currentUsers.contains(entry))
                    return; // already authorized;
                byte[] hash = getPasswordHash(password);
                boolean passwordMatch = entry.assertPassword(hash);
                if (!passwordMatch)
                    throw new PasswordMismatchException();
                else
                    currentUsers.add(entry);
            } catch (SQLException e) {
                throw new DatabaseUnreachableException();
            }
        } else throw new DatabaseUnreachableException();
    }

    /**
     * Возвращает хеш-функцию (SHA-256) от строкового пароля
     * @param password пароль
     * @return массив из 32 байтов (хеш)
     */
    private byte[] getPasswordHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return hash;
        } catch (NoSuchAlgorithmException e) {
            // probably never fires
            e.printStackTrace();
            return new byte[32];
        }
    }
}