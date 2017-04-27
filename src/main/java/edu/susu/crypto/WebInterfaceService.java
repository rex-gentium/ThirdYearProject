package edu.susu.crypto;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import edu.susu.database.*;
import edu.susu.exception.*;

/**
 * Служба, принимающая первичные REST-запросы от пользователя и возвращающая веб-страницы или файлы
 * @author Carolus
 *
 */
@Path("/")
public class WebInterfaceService {

    static DatabaseConnector db; // инициализируется в ServiceContextListener
    private static SessionPool sessions = new SessionPool();

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
    //@Produces(MediaType.TEXT_HTML)
    public Response getPersonalPage(@PathParam("usr") String usr, @CookieParam("session") String sessionKey, @CookieParam("token") String token) throws URISyntaxException {
        if (sessionKey == null || sessionKey.isEmpty() || token == null || token.isEmpty())
            return Response.status(Response.Status.UNAUTHORIZED).build();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getUsername().equalsIgnoreCase(usr) || !session.getToken().equals(token))
            return Response.accepted(HTMLFactory.createHomePage("Session time expired. Please sign in again.")).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = formCookies(sessionKey, session.getToken());
        return Response.ok(HTMLFactory.createUserPage(usr), MediaType.TEXT_HTML).cookie(cookies).build();
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
            String userSessionKey = authUser(username, password);
            String token = sessions.getSession(userSessionKey).getToken();
            if (sessions.getSession(userSessionKey).getUser().getNeuralNetworkConfigFilePath() == null);
                // первый логин
                //return Response.seeOther(new URI(Routes.NETWORK_INIT)).;
            NewCookie[] cookies = formCookies(userSessionKey, token);
            return Response.seeOther(new URI(Routes.HOME + username)).cookie(cookies).build();
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
            case "notAuthorized" : return HTMLFactory.createHomePage("Look's like you're not authenticated on server");
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

    @Path("/logout")
    @GET
    public Response logout(@CookieParam("session") String sessionKey) throws URISyntaxException {
        if (sessionKey == null || sessionKey.isEmpty())
            return Response.status(Response.Status.BAD_REQUEST).build();
        Session session = sessions.getSession(sessionKey);
        if (session != null)
            sessions.closeSession(sessionKey);
        return Response.seeOther(new URI(Routes.HOME)).build();
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
     * @return ключ новой сессии пользователя
     * @throws UserDoesNotExistException если пользователь не найден в базе данных
     * @throws PasswordMismatchException если пользователь найден, но введен неверный пароль
     * @throws DatabaseUnreachableException если не удалось подключиться к базе данных
     */
    private String authUser(String name, String password) throws UserDoesNotExistException, PasswordMismatchException, DatabaseUnreachableException {
        if (db.isConnected()) {
            try {
                User entry = db.getUser(name);
                if (entry == null)
                    throw new UserDoesNotExistException();
                //if (sessions.containsValue(entry))
                //    return; // already authorized;
                byte[] hash = getPasswordHash(password);
                boolean passwordMatch = entry.assertPassword(hash);
                if (!passwordMatch)
                    throw new PasswordMismatchException();
                else
                    return sessions.openSession(entry, 30);
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

    private NewCookie[] formCookies(String session, String token) {
        NewCookie[] cookies = { new NewCookie("session", session),
                new NewCookie("token", token)};
        return cookies;
    }
}