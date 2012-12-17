package com.tolmms.simpleim.exceptions;

public class UserToChatWithIsNotRecognizedException extends Exception {
	
	private static final long serialVersionUID = -1006737598821844770L;

	public UserToChatWithIsNotRecognizedException() {
	}

	public UserToChatWithIsNotRecognizedException(String detailMessage) {
		super(detailMessage);
	}

	public UserToChatWithIsNotRecognizedException(Throwable throwable) {
		super(throwable);
	}

	public UserToChatWithIsNotRecognizedException(String detailMessage,
			Throwable throwable) {
		super(detailMessage, throwable);
	}

}
