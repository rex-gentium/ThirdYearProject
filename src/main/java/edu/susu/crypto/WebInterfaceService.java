package edu.susu.crypto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Date;

import javax.ws.rs.*;
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
    static SessionPool sessions; // инициализаируется в ServiceContextListener

    /**
     * Генерирует главную страницу сервиса (она же страница логина)
     * @return html-страница в строковом формате
     */
    @GET
    @Path("/home")
    @Produces(MediaType.TEXT_HTML)
    public Response serveHomePage(@QueryParam("cause") String errorCause) throws URISyntaxException {
        if (errorCause == null || errorCause.isEmpty())
            return Response.ok(HTMLFactory.createLoginPage()).build();
        String message = null;
        switch(errorCause) {
            case "nullUser": message = "Account with the given username does not exist."; break;
            case "passwordMismatch" : message = "Incorrect password."; break;
            case "notAuthorized" : message = "Look's like you're not authenticated on server"; break;
            case "alreadyExists": message = "User with specified username already registered"; break;
            case "registrationSuccessful": message = "Registration successful. You can now sign in"; break;
            case "sessionExpired": message = "Session time expired. Please sign in again."; break;
            default: return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(HTMLFactory.createLoginPage(message)).build();
    }

    /**
     * Осуществляет процесс регистрации пользователя через POST-запрос
     * @return http-ответ с GET-перенаправлением на страницу результата;
     * код 500, если серверу не удалось соединиться с базой данных
     */
    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@FormParam("username") String username, @FormParam("password")String password) throws URISyntaxException {
        try {
            registerUser(username, password);
            return Response.seeOther(Routes.loginPage("registrationSuccessful")).build();
        } catch (AlreadyExistsException ex) {
            return Response.seeOther(Routes.loginPage("alreadyExists")).build();
        } catch (DatabaseUnreachableException dbEx) {
            return Response.serverError().build();
        }
    }

    /**
     * Осуществляет процесс аутентификации пользователя через POST-запрос
     * @return http-ответ с GET-перенаправлением на страницу результата;
     * код 500, если серверу не удалось соединиться с базой данных
     */
    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("username") String username, @FormParam("password")String password) {
        try {
            String userSessionKey = authUser(username, password);
            String token = sessions.getSession(userSessionKey).getToken();
            NewCookie[] cookies = formCookies(userSessionKey, token);
            return Response.seeOther(Routes.personalPage(username)).cookie(cookies).build();
        } catch (UserDoesNotExistException usrEx) {
            return Response.seeOther(Routes.loginPage("nullUser")).build();
        } catch (PasswordMismatchException pswdEx) {
            return Response.seeOther(Routes.loginPage("passwordMismatch")).build();
        } catch (DatabaseUnreachableException e) {
            return Response.serverError().build();
        }
    }

    /**
     * Генерирует личную страницу авторизованного пользователя
     * @param usr имя пользователя
     * @return html-ответ с личной страницей в общем случае;
     * со страницей логина, если указаны недействительные ключи;
     * код 401, если ключи не указаны вовсе
     */
    @Path("/{usr}")
    @GET
    public Response servePersonalPage(@PathParam("usr") String usr, @CookieParam("session") Cookie sessionCookie, @CookieParam("token") Cookie tokenCookie) throws URISyntaxException {
        if (isNullOrEmpty(sessionCookie) || isNullOrEmpty(tokenCookie))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        String sessionKey = sessionCookie.getValue(), token = tokenCookie.getValue();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(usr) || !session.getToken().equals(token))
            return Response.seeOther(Routes.loginPage("sessionExpired")).cookie(expireCookies(sessionCookie, tokenCookie)).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = (!session.getToken().equals(token)) ? reformTokenCookie(tokenCookie, session.getToken()) : null;
        boolean firstLogin = sessions.getSession(sessionKey).getUser().getStoragePath() == null;
        return (firstLogin)
                ? Response.seeOther(Routes.trainingPage(usr)).cookie(cookies).build()
                : Response.ok(HTMLFactory.createUserPage(usr), MediaType.TEXT_HTML).cookie(cookies).build();
    }

    private boolean isNullOrEmpty(Cookie cookie) {
        return cookie == null || cookie.getValue().isEmpty();
    }

    /**
     * Генерирует страницу инициализации криптографических средств пользователя
     * @param usr имя пользователя
     * @param sessionCookie ключ сессии
     * @param tokenCookie действительный токен сессии
     * @return код 200 со страницей инициализации в общем случае;
     * код 200 со страницей логина, если указаны недействительные ключи;
     * код 401, если ключи не указаны вовсе
     */
    @Path("/{usr}/init")
    @GET
    public Response serveFirstLoginPage(@PathParam("usr") String usr, @CookieParam("session") Cookie sessionCookie, @CookieParam("token") Cookie tokenCookie) throws URISyntaxException {
        if (isNullOrEmpty(sessionCookie) || isNullOrEmpty(tokenCookie))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        String sessionKey = sessionCookie.getValue(), token = tokenCookie.getValue();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(usr) || !session.getToken().equals(token))
            return Response.seeOther(Routes.loginPage("sessionExpired")).cookie(expireCookies(sessionCookie, tokenCookie)).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = (!session.getToken().equals(token)) ? reformTokenCookie(tokenCookie, session.getToken()) : null;
        return Response.ok(HTMLFactory.createANNInitPage(usr), MediaType.TEXT_HTML).cookie(cookies).build();
    }

    /**
     * Обрабатывает файл, отправленный пользователем и перенаправляет
     * @param username имя пользователя
     * @param mode режим нейронной сети: train, encrypt, decrypt
     * @param sessionCookie ключ сессии
     * @param tokenCookie токен сессии
     * @param uploadedInputStream файл из формы
     * @param fileDetail файл из формы
     * @return http-ответ с перенаправлением на страницу результата в общем случае;
     * код 200 со страницей логина, если указаны недейстивтельные ключи;
     * код 401, если ключи не указаны вовсе;
     * код 400, если mode невозможно обработать;
     * код 500, если возникла ошибка при передаче файла
     */
    @Path("/{usr}/upload")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processFile(@PathParam("usr") String username,
                                                   @QueryParam("mode") String mode,
                                                   @CookieParam("session") Cookie sessionCookie,
                                                   @CookieParam("token") Cookie tokenCookie,
                                                   @FormDataParam("file") InputStream uploadedInputStream,
                                                   @FormDataParam("file") FormDataContentDisposition fileDetail)
            throws URISyntaxException, UnsupportedEncodingException
    {
        User user = db.getUser(username);
        if (isNullOrEmpty(sessionCookie) || isNullOrEmpty(tokenCookie) || user == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        String sessionKey = sessionCookie.getValue(), token = tokenCookie.getValue();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(username) || !session.getToken().equals(token))
            return Response.seeOther(Routes.loginPage("sessionExpired")).cookie(expireCookies(sessionCookie, tokenCookie)).build();
        session.extend(30);
        session.tokenUsageCount++;
        NewCookie[] cookies = (!session.getToken().equals(token)) ? reformTokenCookie(tokenCookie, session.getToken()) : null;
        if (user.getStoragePath() == null)
            try {
                FileProcessor.createUserDirectory(user);
            } catch (IOException e) {
                e.printStackTrace();
                return Response.serverError().build();
            }
        String fileName = new String(fileDetail.getFileName().getBytes ("iso-8859-1"), "UTF-8");
        fileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(fileName)) {
            String extension = fileName.substring(fileName.lastIndexOf('.'), fileName.length());
            fileName = "fileNonAscii" + extension;
        }
        java.nio.file.Path filePath = FileProcessor.saveFileInStorage(user, uploadedInputStream, fileName);
        if (filePath == null) return Response.serverError().build();
        String directoryPath = user.getStoragePath();
        String savedFileName = filePath.getFileName().toString();
        Response result = null;
        switch (mode) {
            case "train":
                try {
                    boolean succ = FileProcessor.trainNeuralNetwork(directoryPath, savedFileName);
                    if (succ)
                        result = Response.seeOther(Routes.personalPage(username)).build();
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
                        ? FileProcessor.encryptFile(directoryPath, savedFileName)
                        : FileProcessor.decryptFile(directoryPath, savedFileName);
                if (output != null) {
                    String storedFileName = URLEncoder.encode(output.getFileName().toString(), "UTF-8");
                    result = Response.seeOther(Routes.downloadLink(username, storedFileName, fileName, mode)).build();
                } else result = Response.serverError().build();
                break;
            default: result = Response.status(Response.Status.BAD_REQUEST).build();
        }
        return result;
    }

    /**
     *
     * @param username
     * @param storageFileName
     * @param outputFileName
     * @param sessionCookie
     * @param tokenCookie
     * @return
     * @throws UnsupportedEncodingException
     */
    @Path("/{usr}/download")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response serveDownload(@PathParam("usr") String username,
                                                   @QueryParam("file") String storageFileName,
                                                   @QueryParam("name") String outputFileName,
                                                   @CookieParam("session") Cookie sessionCookie,
                                                   @CookieParam("token") Cookie tokenCookie)
            throws UnsupportedEncodingException
    {
        User user = db.getUser(username);
        if (isNullOrEmpty(sessionCookie) || isNullOrEmpty(tokenCookie) || user == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        String sessionKey = sessionCookie.getValue(), token = tokenCookie.getValue();
        Session session = sessions.getSession(sessionKey);
        if (session == null || !session.getUser().getName().equalsIgnoreCase(username) || !session.getToken().equals(token))
            return Response.ok(HTMLFactory.createLoginPage("Session time expired. Please sign in again.")).cookie(expireCookies(sessionCookie, tokenCookie)).build();
        session.extend(30);
        session.tokenUsageCount++;
        storageFileName = URLDecoder.decode(storageFileName, "UTF-8");
        java.nio.file.Path filePath = Paths.get(user.getStoragePath(), storageFileName);
        File file = filePath.toFile();
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition", "attachment; filename=" + outputFileName);
        return response.build();
    }

    /**
     * Осуществляет перкращение сеанса пользователя
     * @param sessionCookie ключ текущей сессии пользователя
     * @param tokenCookie действительный токен сессии
     * @return bad request code если сессия уже закрыта, иначе перенаправление на домашнюю страницу
     */
    @Path("/logout")
    @GET
    public Response logout(@CookieParam("session") Cookie sessionCookie, @CookieParam("token") Cookie tokenCookie) {
        if (isNullOrEmpty(sessionCookie) || isNullOrEmpty(tokenCookie))
            return Response.status(Response.Status.BAD_REQUEST).build();
        String sessionKey = sessionCookie.getValue();
        Session session = sessions.getSession(sessionKey);
        if (session != null)
            sessions.closeSession(sessionKey);
        return Response.seeOther(Routes.loginPage()).cookie(expireCookies(sessionCookie, tokenCookie)).build();
    }

    /**
     * Проводит регистрацию пользователя в сервисе (делает запись в базе данных)
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
        /*return new NewCookie[]{new NewCookie(new Cookie("session", session), null, -1, null, true, false),
                new NewCookie(new Cookie("token", token), null, -1, null, true, false)};*/
        return new NewCookie[]{new NewCookie("session", session),
                new NewCookie("token", token)};
    }

    private NewCookie[] reformTokenCookie(Cookie oldToken, String token) {
        return new NewCookie[]{new NewCookie(oldToken, "The cookie is dead...", 0, new Date(0), true, false),
                new NewCookie("token", token)};
    }

    private NewCookie[] expireCookies(Cookie session, Cookie token) {
        return new NewCookie[]{new NewCookie(new Cookie("session", session.getValue()), "The cookie is dead...", 0, new Date(0), true, false),
                new NewCookie(new Cookie("token", token.getValue()), "The cookie is dead...", 0, new Date(0), true, false)};
    }
}