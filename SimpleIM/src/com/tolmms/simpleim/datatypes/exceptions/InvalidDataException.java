package com.tolmms.simpleim.datatypes.exceptions;

public class InvalidDataException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 594279059525376058L;

	public InvalidDataException() {
	}

	public InvalidDataException(String detailMessage) {
		super(detailMessage);
	}

	public InvalidDataException(Throwable throwable) {
		super(throwable);
	}

	public InvalidDataException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
