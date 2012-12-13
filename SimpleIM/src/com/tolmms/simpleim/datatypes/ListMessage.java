package com.tolmms.simpleim.datatypes;

import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;

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
 */

public class ListMessage {
	protected Vector<UserInfo> users;

	public ListMessage() {
		users = new Vector<UserInfo>();
	}
	
	public boolean addUser(UserInfo ui) {
		return users.add(ui);
	}
	
	public int size() {
		return users.size();
	}
	
	public UserInfo userAt(int i) {
		return users.elementAt(i);
	}
	
	public Vector<UserInfo> getUserList() {
		return users;
	}
	
	public static ListMessage fromXML(String xml) throws XmlMessageReprException {
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
		
		NodeList entries = rootEl.getElementsByTagName(MessageXMLTags.LIST_MESSAGE_ENTRY_TAG);
		
//		if (entries == null)
//			throw new XmlMessageReprException("null entries");
		
		ListMessage toRet = new ListMessage();
		
		for (int i = 0; i < entries.getLength(); ++i) {
			Element currentEntry = (Element) entries.item(i);
			
			UserInfo u;
			
			try {
				u = UserInfo.fromXML(currentEntry);
			} catch (NumberFormatException e) {
				throw new XmlMessageReprException();
			} catch (InvalidDataException e) {
				throw new XmlMessageReprException();
			}
			
			toRet.addUser(u);
		}
		
		return toRet;
	}
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_USER_LIST);
		
		for (UserInfo u : users) {
			Element entry = doc.createElement(MessageXMLTags.LIST_MESSAGE_ENTRY_TAG);
			
			u.toXML(entry, doc);
			
			rootElement.appendChild(entry);			
		}
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}
	
}
