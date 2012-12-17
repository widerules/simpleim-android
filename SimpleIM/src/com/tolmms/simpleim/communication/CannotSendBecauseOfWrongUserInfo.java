package com.tolmms.simpleim.communication;

public class CannotSendBecauseOfWrongUserInfo extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3077520281832885683L;

	public CannotSendBecauseOfWrongUserInfo() {
	}

	public CannotSendBecauseOfWrongUserInfo(String detailMessage) {
		super(detailMessage);
	}

	public CannotSendBecauseOfWrongUserInfo(Throwable throwable) {
		super(throwable);
	}

	public CannotSendBecauseOfWrongUserInfo(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}
}
