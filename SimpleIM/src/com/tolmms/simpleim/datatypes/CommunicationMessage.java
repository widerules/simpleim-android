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

/*
 * <roottag>
 * <entry>
 * <username> </username>
 * ........
 * </entry>
 * <entry>
 * ......
 * </entry>
 * ......
 * </roottag>
 *
*/

public class CommunicationMessage {
	protected MessageInfo mi;

	public CommunicationMessage(MessageInfo mi) {
		this.mi = mi;
	}
	
	public MessageInfo getMessageInfo() {
		return mi;
	}
	
	
	
	public static CommunicationMessage fromXML(String xml) throws XmlMessageReprException {
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
		
		MessageInfo mi;
		
		try {
			mi = MessageInfo.fromXML(rootEl);
		} catch (InvalidDataException e) {
			throw new XmlMessageReprException();
		}
		
		return new CommunicationMessage(mi);
	}
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_COMMUNICATION_MESSAGE);
		
		mi.toXML(rootElement, doc);
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}
}
