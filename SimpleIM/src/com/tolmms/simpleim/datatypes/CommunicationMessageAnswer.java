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

public class CommunicationMessageAnswer {
	protected UserInfo u;
	protected int message_hash;
	
	public CommunicationMessageAnswer(UserInfo u, int message_hash) {
		this.u = u;
		this.message_hash = message_hash;
	}
	
	public static CommunicationMessageAnswer fromXML(String xml) throws XmlMessageReprException {		
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

		
		NodeList nodes = rootEl.getElementsByTagName(MessageXMLTags.COMMUNICATION_ANSWER_HASH_TAG);
		
		Element e_hash_message = null;
		
		if (nodes.getLength() != 1 || (e_hash_message = (Element) nodes.item(0)) == null)
			throw new XmlMessageReprException();
		
		UserInfo u;
		
		try {
			u = UserInfo.fromXML(rootEl);
		} catch (NumberFormatException e) {
			throw new XmlMessageReprException();
		} catch (InvalidDataException e) {
			throw new XmlMessageReprException();
		}
		
		int hash;
		try {
			hash = Integer.valueOf(e_hash_message.getTextContent());
		} catch (NumberFormatException e) {
			throw new XmlMessageReprException();
		}
		
		return new CommunicationMessageAnswer(u, hash);
		
	}
	
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_COMMUNICATION_MESSAGE_ANSWER);
		

		Element e_message_hash = doc.createElement(MessageXMLTags.COMMUNICATION_ANSWER_HASH_TAG);
		e_message_hash.setTextContent(String.valueOf(message_hash));
		
		u.toXML(rootElement, doc);
		
		rootElement.appendChild(e_message_hash);
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}

	public int getMessageHashAck() {
		return message_hash;
	}
	
	public UserInfo getUser() {
		return u;
	}

}
