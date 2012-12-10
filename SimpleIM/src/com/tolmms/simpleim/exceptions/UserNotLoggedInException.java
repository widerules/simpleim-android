package com.tolmms.simpleim.exceptions;

public class UserNotLoggedInException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8108540954665018307L;

	public UserNotLoggedInException() {
		// TODO Auto-generated constructor stub
	}

	public UserNotLoggedInException(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public UserNotLoggedInException(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	public UserNotLoggedInException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}

}
