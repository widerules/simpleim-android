package com.tolmms.simpleim.datatypes;

public class UserInfo {
	public static final String ONLINE_STATUS = "online";
	public static final String OFFLINE_STATUS = "offline";
	
	protected String username;
	protected String ip;
	protected String port;
	
	protected double latitude;
	protected double longitude;
	protected double altitude;
	
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
		return !status.equals(OFFLINE_STATUS) && status.equals(ONLINE_STATUS);
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

	public String getStatus() {
		return status;
	}

	public void set(String username, String ip, String port) {
		this.username = username;
		this.ip = ip;
		this.port = port;
	}
	
	public void clearInfos() {
		this.username = "";
		this.ip = "";
		this.port = "";
		setOffline();
	}

	public String getIp() {
		return ip;
	}

	public String getPort() {
		return port;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
		
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;		
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public void setIP(String ip) {
		this.ip = ip;
	}

	public void setPort(String port) {
		this.port = port;
	}

}
