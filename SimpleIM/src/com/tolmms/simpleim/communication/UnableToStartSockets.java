package com.tolmms.simpleim.communication;

public class UnableToStartSockets extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4595994012109414070L;

	public UnableToStartSockets() {
	}

	public UnableToStartSockets(String detailMessage) {
		super(detailMessage);
	}

	public UnableToStartSockets(Throwable throwable) {
		super(throwable);
	}

	public UnableToStartSockets(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
