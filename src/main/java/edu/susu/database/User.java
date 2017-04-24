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
	private String neuralNetworkConfigFilePath;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	/*public byte[] getPasswordHash() {
		return passwordHash;
	}*/
	public void setPasswordHash(byte[] passwordHash) {
		this.passwordHash = passwordHash;
	}
	public String getNeuralNetworkConfigFilePath() {
		return neuralNetworkConfigFilePath;
	}
	public void setNeuralNetworkConfigFilePath(String neuralNetworkConfigFilePath) {
		this.neuralNetworkConfigFilePath = neuralNetworkConfigFilePath;
	}

	public boolean assertPassword(byte[] passwordHash) {
		return Arrays.equals(passwordHash, this.passwordHash);
	}

	public boolean equals(User that) {
		return this.username.equals(that.username);
	}
}
