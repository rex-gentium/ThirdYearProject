package edu.susu.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

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
    public Response getPersonalPage(@PathParam("usr") String usr, @CookieParam("session") String sessionKey, @CookieParam("token") String token) throws URISyntaxException {
        if (sessionKey == null || sessionKey.isEmpty() || token == null || token.isEmpty())
            return Response.status(Response.Status.UNAUTHORIZED).build();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(usr) || !session.getToken().equals(token))
            return Response.accepted(HTMLFactory.createHomePage("Session time expired. Please sign in again.")).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = formCookies(sessionKey, session.getToken());
        return Response.ok(HTMLFactory.createUserPage(usr), MediaType.TEXT_HTML).cookie(cookies).build();
    }

    /**
     * Генерирует страницу инициализации криптографических средств пользователя
     * @param usr имя пользователя
     * @param sessionKey ключ сессии
     * @param token действительный токен сессии
     * @return unauthorized, ключ сессии или токен не заполнены;
     * перенаправление на домашнюю страницу, если ключ сессии или токен неверны;
     * иначе html-страницу
     * @throws URISyntaxException
     */
    @Path("/{usr}/init")
    @GET
    public Response getNetworkInitializationPage(@PathParam("usr") String usr, @CookieParam("session") String sessionKey, @CookieParam("token") String token) throws URISyntaxException {
        if (sessionKey == null || sessionKey.isEmpty() || token == null || token.isEmpty())
            return Response.status(Response.Status.UNAUTHORIZED).build();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(usr) || !session.getToken().equals(token))
            return Response.accepted(HTMLFactory.createHomePage("Session time expired. Please sign in again.")).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = formCookies(sessionKey, session.getToken());
        return Response.ok(HTMLFactory.createANNInitPage(usr), MediaType.TEXT_HTML).cookie(cookies).build();
    }

    @Path("/{usr}/upload")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response getNetworkInitializationResult(@PathParam("usr") String username,
                                                   @QueryParam("mode") String mode,
                                                   @CookieParam("session") String sessionKey,
                                                   @CookieParam("token") String token,
                                                   @FormDataParam("file") InputStream uploadedInputStream,
                                                   @FormDataParam("file") FormDataContentDisposition fileDetail)
            throws URISyntaxException, UnsupportedEncodingException
    {
        User user = db.getUser(username);
        if (sessionKey == null || sessionKey.isEmpty() || token == null || token.isEmpty() || user == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(username) || !session.getToken().equals(token))
            return Response.accepted(HTMLFactory.createHomePage("Session time expired. Please sign in again.")).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = formCookies(sessionKey, session.getToken());
        if (user.getStoragePath() == null)
            try {
                FileProcessor.createUserDirectory(user);
            } catch (IOException e) {
                e.printStackTrace();
                return Response.serverError().build();
            }
        java.nio.file.Path filePath = FileProcessor.saveFileInStorage(user, uploadedInputStream, fileDetail.getName());
        if (filePath == null) return Response.serverError().build();
        String directoryPath = user.getStoragePath();
        String fileName = fileDetail.getName();
        Response result = null;
        switch (mode) {
            case "train":
                try {
                    boolean succ = FileProcessor.trainNeuralNetwork(directoryPath, fileName);
                    if (succ)
                        result = Response.seeOther(new URI(Routes.HOME + "/" + username)).build();
                    else {
                        db.updateUserStoragePath(user.getName(), null);
                        result = Response.serverError().build();
                    }
                } catch (IdleUpdateException e) {
                    e.printStackTrace();
                }
                break;
            case "encrypt" :
            case "decrypt" :
                java.nio.file.Path output = (mode.equals("encrypt"))
                        ? FileProcessor.encryptFile(directoryPath, fileName)
                        : FileProcessor.decryptFile(directoryPath, fileName);
                if (output != null) {
                    String queryParam = output.getFileName().toString();
                    queryParam = URLEncoder.encode(queryParam, "UTF-8");
                    result = Response.seeOther(new URI(Routes.HOME + "/" + username + "/download?file=" + queryParam)).build();
                } else result = Response.serverError().build();
                break;
            default: result = Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Осуществляет процесс аутентификации пользователя
     * @return http-ответ с GET-перенаправлением на новую страницу
     */
    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("username") String username, @FormParam("password")String password) throws URISyntaxException {
        try {
            String userSessionKey = authUser(username, password);
            String token = sessions.getSession(userSessionKey).getToken();
            NewCookie[] cookies = formCookies(userSessionKey, token);
            if (sessions.getSession(userSessionKey).getUser().getStoragePath() == null)
                // еще нет хранилища - первый логин
                return Response.seeOther(new URI(username + Routes.NETWORK_INIT)).cookie(cookies).build();
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
     * @param errorCause причина отказа: nullUser, passwordMismatch, notAuthorized
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
     * @param errorCause причина перенаправления: alreadyExists, successful
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
            case "alreadyExists": return HTMLFactory.createRegistrationPage("User with specified username already registered");
            case "successful": return HTMLFactory.createHomePage("Registration successful. You can now sign in");
            default: throw new FailureCauseNotSpecifiedException();
        }
    }

    /**
     * Осуществляет перкращение сеанса пользователя
     * @param sessionKey ключ текущей сессии пользователя
     * @return bad request code если сессия уже закрыта, иначе перенаправление на домашнюю страницу
     */
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
            User entry = db.getUser(name);
            if (entry != null)
                throw new AlreadyExistsException();
            else
                db.addUser(name, hash);
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
        } else throw new DatabaseUnreachableException();
    }

    /**
     * Возвращает хеш-функцию (SHA-256) от строкового пароля
     * @param password пароль
     * @return массив из 32 байтов (хеш)
     */
    private byte[] getPasswordHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // probably never fires
            e.printStackTrace();
            return new byte[32];
        }
    }

    /**
     * Формирует куки, содержащие информацию о сессии и токен
     * @param session ключ сессии
     * @param token действительный токен сессии
     * @return массив из двух куки
     */
    private NewCookie[] formCookies(String session, String token) {
        return new NewCookie[]{ new NewCookie("session", session),
                new NewCookie("token", token)};
    }
}