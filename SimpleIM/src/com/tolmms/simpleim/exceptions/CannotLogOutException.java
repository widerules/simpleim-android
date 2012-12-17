package com.tolmms.simpleim.exceptions;

public class CannotLogOutException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3917433602294081831L;

	public CannotLogOutException() {
		super();
	}

	public CannotLogOutException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public CannotLogOutException(String detailMessage) {
		super(detailMessage);
	}

	public CannotLogOutException(Throwable throwable) {
		super(throwable);
	}

}
