package com.tolmms.simpleim.exceptions;

public class NotEnoughResourcesException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -787230440125616604L;

	public NotEnoughResourcesException() {
	}

	public NotEnoughResourcesException(String detailMessage) {
		super(detailMessage);
	}

	public NotEnoughResourcesException(Throwable throwable) {
		super(throwable);
	}

	public NotEnoughResourcesException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
