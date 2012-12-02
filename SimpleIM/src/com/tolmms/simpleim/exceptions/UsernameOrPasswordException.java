package com.tolmms.simpleim.exceptions;

public class UsernameOrPasswordException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5900772622434677709L;

	public UsernameOrPasswordException() {
	}

	public UsernameOrPasswordException(String detailMessage) {
		super(detailMessage);
	}

	public UsernameOrPasswordException(Throwable throwable) {
		super(throwable);
	}

	public UsernameOrPasswordException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
