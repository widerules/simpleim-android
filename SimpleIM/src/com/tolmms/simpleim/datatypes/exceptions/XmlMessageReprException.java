package com.tolmms.simpleim.datatypes.exceptions;

public class XmlMessageReprException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4204461510023655154L;

	public XmlMessageReprException() {
	}

	public XmlMessageReprException(String detailMessage) {
		super(detailMessage);
	}

	public XmlMessageReprException(Throwable throwable) {
		super(throwable);
	}

	public XmlMessageReprException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
