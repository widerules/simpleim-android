package com.tolmms.simpleim.datatypes;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class Procedures {

	protected static String getTheStringAndCheckIfNullorEmpty(NodeList nl) throws XmlMessageReprException {
		if (nl == null || nl.getLength() != 1)
			throw new XmlMessageReprException("is null or there are more elements :(");
		
		String toRet = nl.item(0).getTextContent();
		
		if (toRet.length() == 0)
			throw new XmlMessageReprException("is empty");
		
		return toRet;
	}
	
	
	protected static String getXmlString(Document doc) throws TransformerException {
		Transformer trans = TransformerFactory.newInstance().newTransformer();
		StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        
        
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        
        
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        
        
        return sw.toString();
	}
	
	public Procedures() {
		// TODO Auto-generated constructor stub
	}


	public static String getMessageType(String the_msg) throws XmlMessageReprException {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new XmlMessageReprException(e);
		}
		
		if (docBuilder == null || the_msg == null)
			return null;
		
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(the_msg));
		Document doc = null;
		
		try {
			doc = docBuilder.parse(is);
		} catch (SAXException e) {
			throw new XmlMessageReprException(e);
		} catch (IOException e) {
			throw new XmlMessageReprException(e);
		}
		
		if (doc == null)
			return null;
		Element rootEl = doc.getDocumentElement();
		
		if (rootEl == null || !rootEl.getNodeName().equals(MessageXMLTags.MESSAGE_TAG))
			return null;
//			throw new XmlMessageReprException("root element is null or not an " + MessageXMLTags.MESSAGE_TAG + " message");

		String toRet = rootEl.getAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE);
		toRet.trim();
		
		if (toRet != null && toRet.equals(""))
			return null;
		
		return toRet;
	}
	
	public static boolean isCommunicationMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_COMMUNICATION_MESSAGE.equals(type);
	}

	public static boolean isLogoutMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_LOGOUT.equals(type);
	}

	public static boolean isSomeOneLoginMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_SOME_ONE_LOGIN.equals(type);
	}


	public static boolean isLoginMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_LOGIN.equals(type);
	}


	public static boolean isRegisterMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_REGISTER.equals(type);
	}


	public static boolean isCommunicationMessageAnswer(String type) {
		return MessageXMLTags.MESSAGE_TYPE_COMMUNICATION_MESSAGE_ANSWER.equals(type);
	}

	public static boolean isUserInfoRequestMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_USER_INFO_REQUEST.equals(type);
	}
	
	public static boolean isUserInfoAnswerMessage(String type) {
		return MessageXMLTags.MESSAGE_TYPE_USER_INFO_ANSWER.equals(type);
	}
	
	

}
