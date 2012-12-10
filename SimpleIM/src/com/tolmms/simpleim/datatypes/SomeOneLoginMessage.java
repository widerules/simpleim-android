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

public class SomeOneLoginMessage {
	UserInfo user;
	
	public SomeOneLoginMessage(UserInfo user) {
		this.user = user;
	}
	
	public static SomeOneLoginMessage fromXML(String xml) throws XmlMessageReprException {
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

		
		int port;
		
		try {
			port = Integer.valueOf(Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.PORT_TAG)));
		} catch (NumberFormatException e) {
			throw new XmlMessageReprException("the port is not a number");
		}
		
		String status = Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.STATUS_TAG));
		
		if (!UserInfo.OFFLINE_STATUS.equals(status) && UserInfo.ONLINE_STATUS.equals(status))
			throw new XmlMessageReprException("the status is not in allowed status");
		
		if (port <= 0 || port > 65535)
			throw new XmlMessageReprException("the port is not between 0 and 65535");
		
		UserInfo thisUserInfo = new UserInfo(Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.USERNAME_TAG)), 
											 Procedures.getTheStringAndCheckIfNullorEmpty(rootEl.getElementsByTagName(MessageXMLTags.IP_TAG)),
											 port,
											 status);
		return new SomeOneLoginMessage(thisUserInfo);
	}

	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_SOME_ONE_LOGIN);
		

		Element e_username = doc.createElement(MessageXMLTags.USERNAME_TAG);
		e_username.setTextContent(user.username);
		Element e_ip = doc.createElement(MessageXMLTags.IP_TAG);
		e_ip.setTextContent(user.ip);
		Element e_port = doc.createElement(MessageXMLTags.PORT_TAG);
		e_port.setTextContent(String.valueOf(user.port));
		Element e_status = doc.createElement(MessageXMLTags.STATUS_TAG);
		e_status.setTextContent(user.status);
		
		rootElement.appendChild(e_username);
		rootElement.appendChild(e_ip);
		rootElement.appendChild(e_port);
		rootElement.appendChild(e_status);
			
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}

	public UserInfo getSource() {
		return user;
	}
}
