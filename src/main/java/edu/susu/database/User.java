package edu.susu.database;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Представление пользователя
 * @author Carolus
 *
 */
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private String username;
	private byte[] passwordHash;
	private String storagePath;
	
	public String getName() {
		return username;
	}
	public void setName(String username) {
		this.username = username;
	}

	public void setPasswordHash(byte[] passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getStoragePath() {
		return storagePath;
	}
	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	public boolean assertPassword(byte[] passwordHash) {
		return Arrays.equals(passwordHash, this.passwordHash);
	}

	public boolean equals(User that) {
		return this.username.equals(that.username);
	}
}
