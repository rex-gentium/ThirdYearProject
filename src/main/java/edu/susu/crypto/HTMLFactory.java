package edu.susu.crypto;

abstract class HTMLFactory {
	
	private static final String template = "<!DOCTYPE html>\n" +
			"<html>" +
			"<head>" +
			"<meta charset=\"UTF-8\">" +
			"<title>CryptoANN</title>" +
			"<link rel=\"stylesheet\" type=\"text/css\" href=\"/styles.css\">" +
			"</head>" +
			"<body>" +
			"<header><center><img src=\"/logo2.png\" height=\"150\" width=\"auto\"></img></center></header>\n" +
			"$content" +
			"<script src=\"/file-inputs.js\"></script>" +
			"<script src=\"/loginForm.js\"></script>" +
			"</body>" +
			"</html>";

	private static final String loginFormTemplate = "<div class=\"form-wrapper\">" +
			"<form action=\"\" method=\"post\" align=\"center\" id=\"loginForm\">" +
			"<div class=\"imgcontainer\"><img src=\"/loginicondblue.png\" alt=\"Avatar\" class=\"avatar\"></div>" +
			"<div class=\"message-line-red\"><font face=\"Arial\" color=\"white\"><b>$message</b></font></div>" +
			"<div style=\"padding:16px\">\n" +
			"<input type=\"text\" placeholder=\"Username\" name=\"username\" required>" +
			"<input type=\"password\" placeholder=\"Password\" name=\"password\" required>" +
			"<button type=\"button\" onclick=\"submitWithAction(\'$registerAction\');\">Register</button>" +
			"<button type=\"button\" onclick=\"submitWithAction(\'$loginAction\');\">Sign in</button>" +
			"</div>" +
			"</form>" +
			"</div>";

	private static final String fileUploadFormTemplate = "<div class=\"$class\">" +
			"<form action=\"$action\" enctype=\"multipart/form-data\" method=\"post\" align=\"center\">" +
			"<div class=\"imgcontainer\"><img src=\"$avatar\" alt=\"Avatar\" class=\"avatar\"></div>" +
			"<div class=\"message-line-$messageColor\">" +
			"<font face=\"Arial\" color=\"white\"><b>$header</b></font>" +
			"</div>" +
			"<div style=\"padding:16px\">" +
			"<label class=\"file-name-holder\">No file chosen</label>" +
			"<label class=\"file-container\">Choose a file" +
			"<input type=\"file\" name=\"file\" class=\"inputfile\" required>" +
			"</label>" +
			"<button type=\"submit\" style=\"width:100%\">$button</button>" +
			"</div>" +
			"</form>" +
			"</div>";

	private static final String fileUploadFormsTemplate = "<div align=\"center\">" +
			fileUploadFormTemplate.replace("$action", "$encryptAction")
					.replace("$class", "file-form-wrapper-inline")
					.replace("$avatar", "/encLogo.png")
					.replace("$messageColor", "blue")
					.replace("$header", "File encryption")
					.replace("$button", "Encrypt file") +
			fileUploadFormTemplate.replace("$action", "$decryptAction")
					.replace("$class", "file-form-wrapper-inline")
					.replace("$avatar", "/decLogo.png")
					.replace("$messageColor", "blue")
					.replace("$header", "File decryption")
					.replace("$button", "Decrypt file") +
			"</div>";

	private static final String trainUploadFormTemplate = fileUploadFormTemplate
			.replace("$class", "train-form-wrapper")
			.replace("$avatar", "/trainLogo.png")
			.replace("$messageColor", "red")
			.replace("$header", "Training set")
			.replace("$button", "Create Artificial Neural Network");

	private static final String logoutButton = "<button class=\"logout-button\" onclick=\"location.href='/rest/logout';\">Sign out</button>";
	
	public static String createLoginPage() {
		return createLoginPage("Please fill the fields below to sign in or register");
	}

	public static String createLoginPage(String errorExplanation) {
		String loginForm = loginFormTemplate
				.replace("$loginAction", Routes.LOGIN)
				.replace("$registerAction", Routes.REGISTER)
				.replace("$message", errorExplanation);
		return template.replace("$content", loginForm);
	}

	public static String createUserPage(String username) {
		String encUploadForm = fileUploadFormsTemplate
				.replace("$encryptAction", Routes.ROOT + "/" + username + Routes.UPLOAD_POSTFIX + "?mode=encrypt")
				.replace("$decryptAction", Routes.ROOT + "/" + username + Routes.UPLOAD_POSTFIX + "?mode=decrypt");
		String page = template.replace("$content", encUploadForm + logoutButton);
		return page;
	}
	
	public static String createANNInitPage(String username) {
	    String fileUploadForm = trainUploadFormTemplate
				.replace("$action", Routes.ROOT + "/" + username + Routes.UPLOAD_POSTFIX + "?mode=train");
	    String content = ("<div class=\"text-wrapper\">" + "$inner-content" + "</div>")
				.replace("$inner-content", "<h2>Welcome, " + username + "</h2>" +
				"<p>Since this is the first time you use our WebService, your personal Artificial Neural Network Encryptor should now be configured.</p>" +
				"<p>Upload any file, some book perhaps, and we shall use it as a training set for your future Encryptor. " +
				"Once we're done, the Neural Network will be bound to your account, ready to encrypt and decrypt any file you will give to it.</p>" +
				"<p>Please choose the file you want to use as a training set:</p>" +
				fileUploadForm +
				"<p><b>Note:</b> Depending on file size, network training could take a few minutes. Files under 10 MB are processed relatively fast.</p>" +
				logoutButton);
		return template.replace("$content", content);
    }

}
