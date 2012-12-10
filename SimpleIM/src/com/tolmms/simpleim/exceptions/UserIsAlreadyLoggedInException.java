package com.tolmms.simpleim.exceptions;

public class UserIsAlreadyLoggedInException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1466251776692911249L;

	public UserIsAlreadyLoggedInException() {
		super();
	}

	public UserIsAlreadyLoggedInException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public UserIsAlreadyLoggedInException(String detailMessage) {
		super(detailMessage);
	}

	public UserIsAlreadyLoggedInException(Throwable throwable) {
		super(throwable);
	}
}
