package com.tolmms.simpleim.communication;

public class CannotSendBecauseOfWrongUserInfo extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3077520281832885683L;

	public CannotSendBecauseOfWrongUserInfo() {
		// TODO Auto-generated constructor stub
	}

	public CannotSendBecauseOfWrongUserInfo(String detailMessage) {
		super(detailMessage);
		// TODO Auto-generated constructor stub
	}

	public CannotSendBecauseOfWrongUserInfo(Throwable throwable) {
		super(throwable);
		// TODO Auto-generated constructor stub
	}

	public CannotSendBecauseOfWrongUserInfo(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
		// TODO Auto-generated constructor stub
	}
}
