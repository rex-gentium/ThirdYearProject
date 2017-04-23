package edu.susu.crypto;

public abstract class HTMLFactory {
	
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
	
	public static String createHomePage() {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Hello, stranger!</h1>" 
				+ "You can try to register <a href=\""+ Routes.REGISTER +"\">here</a>");
		return page;
	}
	
	public static String createUserPage(String username) {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Hello, " + username + "</h1>");
		return page;
	}
	
	public static String createHomePage(String errorExplanation) {
		String page = template.replace("$title", "Crypto ANN");
		page = page.replace("$content", "<h1>Hello, stranger!</h1>"
				+ errorExplanation + "<br>"
				+ "You can try to register <a href=\""+ Routes.REGISTER +"\">here</a>.");
		return page;
	}
	
	public static String createRegistrationPage() {
		String page = template.replace("$title", "Crypto ANN");
		String form = "<form action=\""+ Routes.REGISTER +"\" method=\"post\">"
		+ "Username:<br>"
		+ "<input type=\"text\" name=\"username\"><br>"
		+ "Password:<br>"
		+ "<input type=\"password\" name=\"password\"><br>"
		+ "<input type=\"submit\" value=\"Register\">"
		+ "</form>";
		page = page.replace("$content", "<h1>Register, stranger!</h1>" + form);
		return page;
	}
	
	public static String createRegistrationPage(String errorCause) {
		String page = template.replace("$title", "Crypto ANN");
		String form = "<form action=\""+ Routes.REGISTER +"\" method=\"post\">"
		+ "Username:<br>"
		+ "<input type=\"text\" name=\"username\"><br>"
		+ "Password:<br>"
		+ "<input type=\"password\" name=\"password\"><br>"
		+ "<input type=\"submit\" value=\"Register\">"
		+ "</form>";
		page = page.replace("$content", "<h1>Register, stranger!</h1>" + errorCause + "<br>" + form);
		return page;
	}
}
