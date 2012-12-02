package com.tolmms.simpleim.datatypes;

public class UserInfo {
	public static final String ONLINE_STATUS = "online";
	public static final String OFFLINE_STATUS = "offline";
	
	protected String username;
	protected String ip;
	protected String port;
	
	protected String status;

	public UserInfo(String username, String ip, String port) {
		this.username = username;
		this.ip = ip;
		this.port = port;
		status = ONLINE_STATUS;
		//TODO da mettere nel costruttore il status ???
	}
	
	public UserInfo(String username, String ip, String port, String status) {
		this(username, ip, port);
		this.status = status;
	}
	
	
	public boolean isOnline() {
		return !status.equals(OFFLINE_STATUS);
	}
	
	public void setOnline() {
		status = ONLINE_STATUS;
	}
	
	public void setOffline() {
		status = OFFLINE_STATUS;
	}
	
	
	@Override
	public String toString() {
		return "username: " + username + "; ip: " + ip + "; port: " + port + "; status: " + status;
	}


	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
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
		UserInfo other = (UserInfo) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	public String getUsername() {
		return username;
	}

}
