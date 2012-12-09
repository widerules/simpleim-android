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
			
			UserInfo currentUser = null;
			
			currentUser = new UserInfo(Procedures.getTheStringAndCheckIfNullorEmpty(currentEntry.getElementsByTagName(MessageXMLTags.USERNAME_TAG)),
									   Procedures.getTheStringAndCheckIfNullorEmpty(currentEntry.getElementsByTagName(MessageXMLTags.IP_TAG)), 
									   Procedures.getTheStringAndCheckIfNullorEmpty(currentEntry.getElementsByTagName(MessageXMLTags.PORT_TAG)),
									   Procedures.getTheStringAndCheckIfNullorEmpty(currentEntry.getElementsByTagName(MessageXMLTags.STATUS_TAG)));
			
			toRet.addUser(currentUser);
		}
		
		return toRet;
	}
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_USER_LIST);
		
		for (UserInfo u : users) {
			Element entry = doc.createElement(MessageXMLTags.LIST_MESSAGE_ENTRY_TAG);
			
			Element u_username = doc.createElement(MessageXMLTags.USERNAME_TAG);
			u_username.setTextContent(u.username);
			Element u_ip = doc.createElement(MessageXMLTags.IP_TAG);
			u_ip.setTextContent(u.ip);
			Element u_port = doc.createElement(MessageXMLTags.PORT_TAG);
			u_port.setTextContent(u.port);
			Element u_status = doc.createElement(MessageXMLTags.STATUS_TAG);
			u_status.setTextContent(u.status);
			
			entry.appendChild(u_username);
			entry.appendChild(u_ip);
			entry.appendChild(u_port);	
			entry.appendChild(u_status);
			
			rootElement.appendChild(entry);			
		}
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}

	public Vector<UserInfo> getUserList() {
		return users;
	}

}
