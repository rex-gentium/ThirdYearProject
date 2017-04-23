package edu.susu.exception;

public class DatabaseUnreachableException extends Exception {

	private static final long serialVersionUID = 1L;

	public DatabaseUnreachableException() {
		super();
	}

	public DatabaseUnreachableException(Throwable cause) {
		super(cause);
	}
}
