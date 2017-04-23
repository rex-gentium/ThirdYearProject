package edu.susu.crypto;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.susu.database.DatabaseConnector;
import edu.susu.exception.FailureCauseNotSpecifiedException;
import edu.susu.exception.PasswordMismatchException;
import edu.susu.exception.UserNotExistsException;

/**
 * Служба, принимающая первичные REST-запросы от пользователя и возвращающая веб-страницы или файлы
 * @author Carolus
 *
 */
@Path("/")
public class WebInterfaceService {

    //static DatabaseConnector db; // инициализируется в ServiceContextListener

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
     * @return html-страница в строковом формате
     * @throws URISyntaxException
     */
    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("username") String username, @FormParam("password")String password) throws URISyntaxException {
        try {
            authUser(username, password);
            return Response.seeOther(new URI(Routes.HOME + "/" + username)).build();
        } catch (UserNotExistsException usrEx) {
            return Response.seeOther(new URI(Routes.LOGIN + "?cause=nullUser")).build();
        } catch (PasswordMismatchException PswdEx) {
            return Response.seeOther(new URI(Routes.LOGIN + "?cause=passwordMismatch")).build();
        }
    }

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
     * @return html-страница в строковом формате
     * @throws URISyntaxException
     */
    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@FormParam("username") String username, @FormParam("password")String password) throws URISyntaxException {
        if (!existsUser(username)) {
            URI location = new URI(Routes.HOME + "/" + username);
            return Response.seeOther(location).build();
        }
        else
            return Response.seeOther(new URI(Routes.REGISTER + "?cause=alreadyExists")).build();
    }

    @Path("/register")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String register(@QueryParam("cause") String errorCause) throws FailureCauseNotSpecifiedException {
        if (errorCause == null || errorCause.isEmpty())
            return HTMLFactory.createRegistrationPage();
        else if (errorCause.equals("alreadyExists"))
            return HTMLFactory.createRegistrationPage("User with specified username already registered");
        else throw new FailureCauseNotSpecifiedException();
    }

    /**
     * Проводит авторизацию пользователя в сервисе
     * @param name имя пользователя
     * @param password пароль
     * @throws UserNotExistsException если пользователь не найден в базе данных
     * @throws PasswordMismatchException если пользователь найден, но введен неверный пароль
     */
    private void authUser(String name, String password) throws UserNotExistsException, PasswordMismatchException {
        return;
    }

    /**
     * Проверка регистрации пользователя
     * @param username имя пользователя
     * @return true, если пользователь найден в базе данных, иначе false
     */
    private boolean existsUser(String username) {
        return false;
    }
}