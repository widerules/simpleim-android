package com.tolmms.simpleim.datatypes;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class LoginMessage {
	UserInfo user;
	private String password;

	
	public LoginMessage(UserInfo user, String password) {
		this.user = user;
		this.password = password;
	}

	public static LoginMessage fromXML(String xml) throws XmlMessageReprException {		
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new XmlMessageReprException(e);
		}
		
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xml));
		Document doc = null;
		
		try {
			doc = docBuilder.parse(is);
		} catch (SAXException e) {
			throw new XmlMessageReprException(e);
		} catch (IOException e) {
			throw new XmlMessageReprException(e);
		}
		
		Element rootEl = doc.getDocumentElement();
		
		if (rootEl == null || !rootEl.getNodeName().equals(MessageXMLTags.MESSAGE_TAG))
			throw new XmlMessageReprException("root element is null or not an " + MessageXMLTags.MESSAGE_TAG + " message");

		UserInfo thisUserInfo = new UserInfo(Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.USERNAME_TAG)), 
											 Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.IP_TAG)),
											 Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.PORT_TAG)),
											 Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.STATUS_TAG)));
		return new LoginMessage(thisUserInfo, Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.PASSWORD_TAG)));
	}
	
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_LOGIN);
		

		Element e_username = doc.createElement(MessageXMLTags.USERNAME_TAG);
		e_username.setTextContent(user.username);
		Element e_password = doc.createElement(MessageXMLTags.PASSWORD_TAG);
		e_password.setTextContent(password);
		Element e_ip = doc.createElement(MessageXMLTags.IP_TAG);
		e_ip.setTextContent(user.ip);
		Element e_port = doc.createElement(MessageXMLTags.PORT_TAG);
		e_port.setTextContent(user.port);
		Element e_status = doc.createElement(MessageXMLTags.STATUS_TAG);
		e_status.setTextContent(user.status);
		
		rootElement.appendChild(e_username);
		rootElement.appendChild(e_password);
		rootElement.appendChild(e_ip);
		rootElement.appendChild(e_port);
		rootElement.appendChild(e_status);
			
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}

	public UserInfo getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}


	
//	@Override
//	public String toString() {
//		return "username: " + username + "password: " + password + "; ip: " + ip + "; port: " + port;
//	}
//	
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = super.hashCode();
//		result = prime * result
//				+ ((password == null) ? 0 : password.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (!super.equals(obj))
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		LoginMessage other = (LoginMessage) obj;
//		if (password == null) {
//			if (other.password != null)
//				return false;
//		} else if (!password.equals(other.password))
//			return false;
//		return true;
//	}
//	
	
}
