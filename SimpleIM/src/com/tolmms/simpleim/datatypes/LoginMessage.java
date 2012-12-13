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

import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class LoginMessage {
	UserInfo user;
	private String password;

	
	public LoginMessage(UserInfo user, String password) {
		this.user = user;
		this.password = password;
	}
	
	public UserInfo getUser() {
		return user;
	}

	public String getPassword() {
		return password;
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

		UserInfo u;
		
		try {
			u = UserInfo.fromXML(rootEl);
		} catch (NumberFormatException e) {
			throw new XmlMessageReprException();
		} catch (InvalidDataException e) {
			throw new XmlMessageReprException();
		}
		
		return new LoginMessage(u, Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.PASSWORD_TAG)));
	}
	
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_LOGIN);
		
		user.toXML(rootElement, doc);
		
		Element e_password = doc.createElement(MessageXMLTags.PASSWORD_TAG);
		e_password.setTextContent(password);
		
		rootElement.appendChild(e_password);
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}
	
}
