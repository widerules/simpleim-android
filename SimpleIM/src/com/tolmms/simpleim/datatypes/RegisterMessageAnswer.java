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

import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class RegisterMessageAnswer {
	public static final String ACCEPTED = "ok";
	public static final String REFUSED = "ko";
	
	protected String answer;
	
	public RegisterMessageAnswer(String answer) {
		this.answer = answer;
	}
	
	public boolean accepted() {
		return ACCEPTED.equals(answer);
	}

	public static RegisterMessageAnswer fromXML(String xml) throws XmlMessageReprException {
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

		
		NodeList nodes = rootEl.getElementsByTagName(MessageXMLTags.REGISTER_USER_ANSWER_TAG);
		
		Element e = null;
		
		if (nodes.getLength() != 1 || (e = (Element) nodes.item(0)) == null)
			throw new XmlMessageReprException();
		
		return new RegisterMessageAnswer(e.getTextContent());
	}
	
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_REGISTER_ANSWER);
		

		Element e_answer = doc.createElement(MessageXMLTags.REGISTER_USER_ANSWER_TAG);
		e_answer.setTextContent(answer);
		
		rootElement.appendChild(e_answer);
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}
	
}
