package edu.susu.crypto;

abstract class HTMLFactory {
	
	private static final String template = "<!DOCTYPE html>"
		+ "<html>"
		+ "<head>"
		+ "<meta charset=\"UTF-8\">"
		+ "<title>$title</title>"
		+ "</head>"
		+ "<body>"
		+ "$content"
		+ "</body>"
		+ "</html>";

	private static final String loginForm = "<form action=\""+ Routes.LOGIN +"\" method=\"post\">"
			+ "Username:<br>"
			+ "<input type=\"text\" name=\"username\"><br>"
			+ "Password:<br>"
			+ "<input type=\"password\" name=\"password\"><br>"
			+ "<input type=\"submit\" value=\"Sign in\">"
			+ "</form>";

	private static final String registrationForm = "<form action=\""+ Routes.REGISTER +"\" method=\"post\">"
			+ "Username:<br>"
			+ "<input type=\"text\" name=\"username\"><br>"
			+ "Password:<br>"
			+ "<input type=\"password\" name=\"password\"><br>"
			+ "<input type=\"submit\" value=\"Register\">"
			+ "</form>";

    private static final String fileUploadFormTemplate = "<form action=\"$action\" method=\"post\" enctype=\"multipart/form-data\">"
            + "<input type=\"file\" name=\"file\"><br>"
            + "<input type=\"submit\" value=\"Upload\">"
            + "</form>";
	
	public static String createHomePage() {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Hello, stranger!</h1>"
				+ "<p>Please sign in to access Web Service</p>"
				+ loginForm
				+ "Or you can try to <a href=\""+ Routes.REGISTER +"\">register</a>");
		return page;
	}

	public static String createHomePage(String errorExplanation) {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Hello, stranger!</h1>"
				+ "<p>" + errorExplanation + "</p>"
				+ loginForm
				+ "Or you can try to <a href=\""+ Routes.REGISTER +"\">register</a>");
		return page;
	}

	public static String createUserPage(String username) {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Hello, " + username + "</h1>"
				+ "<p>This is your private page.</p>"
				+ "<p>We assume jumping <a href=\"" + Routes.HOME + "/" + username
                + "\">this exact page</a> could be done only once per token.</p>"
				+ "<a href=\"" + Routes.LOGOUT + "\">Sign out</a>"
				+ "</form>");
		return page;
	}
	
	public static String createRegistrationPage() {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Registration</h1>"
				+ "<p>Please fill the form below to create your account</p>"
				+ registrationForm);
		return page;
	}
	
	public static String createRegistrationPage(String errorCause) {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Registration</h1>"
				+ "<p>" + errorCause + "</p>"
				+ "<p>Please fill the form below to create your account</p>"
				+ registrationForm);
		return page;
	}

	public static String createANNInitPage(String username) {
	    String page = template.replace("$title", "Crypto ANN");
	    String fileUploadForm = fileUploadFormTemplate
				.replace("$action", Routes.HOME + "/" + username + "/upload?mode=train");
        page = page.replace("$content", "<h1>ANN Initialization</h1>"
                + "<h2>Hello, " + username + "</h2>"
                + "<p>Since this is the first time you use our WebService, your personal Artificial Neural Network Encryptor should now be configured.</p>"
                + "<p>Upload any file, some book for example, and we shall use it as a training set for your future Encryptor."
                + " Once we're done, the Neral Network will be bound to your account, ready to encrypt and decrypt any file you will give to it.</p>"
                + "<p>Please choose the file you want to use as a training set: </p>"
                + fileUploadForm);
        return page;
    }

}
