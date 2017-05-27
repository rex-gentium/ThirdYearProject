package edu.susu.crypto;

import edu.susu.database.User;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public interface Routes {
	public static final String ROOT = "/rest";
	public static final String HOME = ROOT + "/home";
	public static final String NETWORK_INIT_POSTFIX = "/init";
	public static final String LOGIN = ROOT + "/login";
	public static final String REGISTER = ROOT + "/register";
	public static final String LOGOUT_POSTFIX = "/logout";
	public static final String UPLOAD_POSTFIX = "/upload";
	public static final String DOWNLOAD_POSTFIX = "/download";

	public static URI personalPage(String userName) {
		try {
			return new URI(ROOT + "/" + URLEncoder.encode(userName, "UTF-8"));
		} catch (UnsupportedEncodingException | URISyntaxException e) {
			e.printStackTrace(); // never fires
			return null;
		}
	}

	public static URI trainingPage(String userName) {
		try {
			return new URI(ROOT + "/" + URLEncoder.encode(userName, "UTF-8") + NETWORK_INIT_POSTFIX);
		} catch (URISyntaxException | UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URI loginPage() {
		try {
			return new URI(HOME);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URI loginPage(String causeQueryParam) {
		try {
			return new URI(HOME + "?cause=" + causeQueryParam);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URI downloadLink(String username, String storedFileName, String initialFileName, String annMode) {
		String niceFileName = initialFileName;
		if (annMode.equals("encrypt"))
			niceFileName += ".crypto";
		else if (initialFileName.endsWith(".crypto"))
			niceFileName = niceFileName.substring(0, niceFileName.length() - 7);
		else niceFileName += ".decrypted";
		try {
			niceFileName = URLEncoder.encode(niceFileName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// never fires
			e.printStackTrace();
			return null;
		}
		try {
			return new URI(Routes.ROOT + "/" + username + "/download?file=" + storedFileName + "&name=" + niceFileName);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
