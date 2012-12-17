package com.tolmms.simpleim.exceptions;

public class UserNotLoggedInException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8108540954665018307L;

	public UserNotLoggedInException() {
	}

	public UserNotLoggedInException(String detailMessage) {
		super(detailMessage);
	}

	public UserNotLoggedInException(Throwable throwable) {
		super(throwable);
	}

	public UserNotLoggedInException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
