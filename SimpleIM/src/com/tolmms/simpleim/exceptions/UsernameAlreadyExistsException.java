package com.tolmms.simpleim.exceptions;

public class UsernameAlreadyExistsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8770744271566429859L;

	public UsernameAlreadyExistsException() {
		super();
	}

	public UsernameAlreadyExistsException(String detailMessage,
			Throwable throwable) {
		super(detailMessage, throwable);
	}

	public UsernameAlreadyExistsException(String detailMessage) {
		super(detailMessage);
	}

	public UsernameAlreadyExistsException(Throwable throwable) {
		super(throwable);
	}

}
