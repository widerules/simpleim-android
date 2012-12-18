package com.tolmms.simpleim.datatypes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class UserInfo {
	public static final String SERVER_USERNAME = "server";			
			
	public static final String ONLINE_STATUS = "online";
	public static final String OFFLINE_STATUS = "offline";
	
	public static final double GENOVA_LATITUDE = 44.411111;
	public static final double GENOVA_LONGITUDE = 8.932778;
	
	public static final int MIN_ALLOWED_PORT = 1;
	public static final int MAX_ALLOWED_PORT = 65535;
	
	protected String username;
	protected String ip;
	protected int port;
	
	protected double latitude;
	protected double longitude;
	protected double altitude;
	
	protected String status;

	private boolean locationData;

	public UserInfo(String username, String ip, int port) throws InvalidDataException {
		this.username = username;
		this.ip = ip;
		this.port = port;
		status = OFFLINE_STATUS;
		
		if (port < MIN_ALLOWED_PORT || port > MAX_ALLOWED_PORT)
			throw new InvalidDataException("port number is not allowed");
		
		latitude = GENOVA_LATITUDE;
		longitude = GENOVA_LONGITUDE;
		altitude = 0;
		
		locationData = false;
	}
	
	public UserInfo(String username, String ip, int port, String status) 
			throws InvalidDataException {
		this(username, ip, port);
		this.status = status;
		
		if (!OFFLINE_STATUS.equals(status) && !ONLINE_STATUS.equals(status))
			throw new InvalidDataException();
	}
	
	public UserInfo(String username, String ip, int port, String status, double latitude, double longitude, double altitude, boolean locationData) 
			throws InvalidDataException {
		this(username, ip, port, status);
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		
		this.locationData = locationData;
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
		return "username: " + username + "; ip: " + ip + "; port: " + port + "; status: " + status + "; hasLocation: " + locationData;
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

	/* getters and setters */
	public String getUsername() {
		return username;
	}

	public String getStatus() {
		return status;
	}
	
	public void clearInfos() {
		this.username = "";
		this.ip = "";
		this.port = UserInfo.MIN_ALLOWED_PORT;
		setOffline();
		this.locationData = false;
		this.longitude = GENOVA_LONGITUDE;
		this.latitude = GENOVA_LATITUDE;
		this.altitude = 0;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	
	public String getPortString() {
		return String.valueOf(port);
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

	public void setPort(int port) {
		this.port = port;
	}
	
	public void set(String username, String ip, int port) {
		this.username = username;
		this.ip = ip;
		this.port = port;
	}
	
	/*******************************/
	
	protected static UserInfo fromXML(Element rootElement) 
			throws NumberFormatException, XmlMessageReprException, InvalidDataException {
		
		String username = Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.USERNAME_TAG));
		String ip = Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.IP_TAG));
		int port = Integer.valueOf(Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.PORT_TAG)));
		String status = Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.STATUS_TAG));
		double latitude = Double.valueOf(Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.LATITUDE_TAG)));
		double longitude = Double.valueOf(Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.LONGITUDE_TAG)));
		double altitude = Double.valueOf(Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.ALTITUDE_TAG)));
		boolean locationData = Boolean.valueOf(Procedures.getTheStringAndCheckIfNullorEmpty(rootElement.getElementsByTagName(MessageXMLTags.LOCATION_DATA_TAG)));
		
		return new UserInfo(username, ip, port, status, latitude, longitude, altitude, locationData);
	}
	
	
	protected void toXML(Element rootElement, Document doc) {
		Element e_username = doc.createElement(MessageXMLTags.USERNAME_TAG);
		Element e_ip = doc.createElement(MessageXMLTags.IP_TAG);
		Element e_port = doc.createElement(MessageXMLTags.PORT_TAG);
		Element e_status = doc.createElement(MessageXMLTags.STATUS_TAG);
		Element e_latitude = doc.createElement(MessageXMLTags.LATITUDE_TAG);
		Element e_longitude = doc.createElement(MessageXMLTags.LONGITUDE_TAG);
		Element e_altitude = doc.createElement(MessageXMLTags.ALTITUDE_TAG);
		Element e_locationData = doc.createElement(MessageXMLTags.LOCATION_DATA_TAG);
		
		e_username.setTextContent(this.username);
		e_ip.setTextContent(this.ip);
		e_port.setTextContent(String.valueOf(this.port));
		e_status.setTextContent(this.status);
		e_latitude.setTextContent(String.valueOf(this.latitude));
		e_longitude.setTextContent(String.valueOf(this.longitude));
		e_altitude.setTextContent(String.valueOf(this.altitude));
		e_locationData.setTextContent(String.valueOf(this.locationData));
		
		rootElement.appendChild(e_username);
		rootElement.appendChild(e_ip);
		rootElement.appendChild(e_port);
		rootElement.appendChild(e_status);
		rootElement.appendChild(e_latitude);
		rootElement.appendChild(e_longitude);
		rootElement.appendChild(e_altitude);
		rootElement.appendChild(e_locationData);
		
//		return rootElement;
	}

	public void locationData(boolean locationData) {
		this.locationData = locationData;
	}
	
	public boolean hasLocationData() {
		return locationData;
	}

}
