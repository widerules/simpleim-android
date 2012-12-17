package com.tolmms.simpleim.datatypes;

public class MessageRepresentation {
	private MessageInfo mi;
	public int sentRetries;
	public boolean ackRecieved;

	public MessageRepresentation(MessageInfo mi) {
		this.mi = mi;
		sentRetries = 1;
		ackRecieved = false;
	}
	
	public MessageInfo getMessageInfo() {
		return mi;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mi == null) ? 0 : mi.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageRepresentation other = (MessageRepresentation) obj;
		if (mi == null) {
			if (other.mi != null)
				return false;
		} else if (!mi.equals(other.mi))
			return false;
		return true;
	}
	
}
