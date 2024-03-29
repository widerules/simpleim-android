package com.tolmms.simpleim.datatypes;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class LoginMessageAnswer {
	protected UserInfo u;
	protected String number;
	
	protected String INVALID_NUMBER = "";

	public LoginMessageAnswer(UserInfo u) {
		this(u, "");
	}
	
	public LoginMessageAnswer(UserInfo u, String number) {
		this.number = number;
		this.u = u;
	}
	
	public boolean accepted() {
		return !number.equals(INVALID_NUMBER);
	}
	
	public static LoginMessageAnswer fromXML(String xml) throws XmlMessageReprException {		
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

		
		NodeList nodes = rootEl.getElementsByTagName(MessageXMLTags.LOGIN_USER_NUMBER_TAG);
		
		Element e_number = null;
		
		if (nodes.getLength() != 1 || (e_number = (Element) nodes.item(0)) == null)
			throw new XmlMessageReprException();
		
		UserInfo u;
		
		try {
			u = UserInfo.fromXML(rootEl);
		} catch (NumberFormatException e) {
			throw new XmlMessageReprException();
		} catch (InvalidDataException e) {
			throw new XmlMessageReprException();
		}
		return new LoginMessageAnswer(u, e_number.getTextContent());
		
	}
	
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_LOGIN_ANSWER);
		

		Element e_number = doc.createElement(MessageXMLTags.LOGIN_USER_NUMBER_TAG);
		e_number.setTextContent(number);
		
		u.toXML(rootElement, doc);
		
		rootElement.appendChild(e_number);
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}

	public String getNumber() {
		return number;
	}
	
	public UserInfo getUser() {
		return u;
	}

}
