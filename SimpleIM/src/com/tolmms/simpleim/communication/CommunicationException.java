package com.tolmms.simpleim.communication;

public class CommunicationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8616384016816934659L;

	public CommunicationException() {
		super();
	}

	public CommunicationException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public CommunicationException(String detailMessage) {
		super(detailMessage);
	}

	public CommunicationException(Throwable throwable) {
		super(throwable);
	}

}
