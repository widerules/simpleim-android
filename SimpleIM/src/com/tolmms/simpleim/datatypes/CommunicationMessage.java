package com.tolmms.simpleim.datatypes;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
 *
*/

public class CommunicationMessage {
	protected UserInfo src;
	protected UserInfo dst;
	protected String message;
	protected Date sendDate;

	public CommunicationMessage(UserInfo src, UserInfo dst, String message, Date sendDate) {
		this.src = src;
		this.dst = dst;
		this.message = message;
		this.sendDate = sendDate;
	}
	
	
	
	public static CommunicationMessage fromXML(String xml) throws XmlMessageReprException {
		UserInfo srcInfo = null;
		UserInfo dstInfo = null;
		String currentMessage = "";
		
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
		
		Element srcElement = (Element) rootEl.getElementsByTagName(MessageXMLTags.SOURCE_TAG).item(0);
		Element dstElement = (Element) rootEl.getElementsByTagName(MessageXMLTags.DESTINATION_TAG).item(0);
		Element msgElement = (Element) rootEl.getElementsByTagName(MessageXMLTags.COMMUNICATION_MESSAGE_SENT_MESSAGE_TAG).item(0);
		
		if (srcElement == null || dstElement == null || msgElement == null) 
			throw new XmlMessageReprException("src or dest or msg are null");
		
		srcInfo = new UserInfo(Procedures.getTheStringAndCheckIfNullorEmpty(srcElement.getElementsByTagName(MessageXMLTags.USERNAME_TAG)),
				Procedures.getTheStringAndCheckIfNullorEmpty(srcElement.getElementsByTagName(MessageXMLTags.IP_TAG)),
				Procedures.getTheStringAndCheckIfNullorEmpty(srcElement.getElementsByTagName(MessageXMLTags.PORT_TAG)),
				Procedures.getTheStringAndCheckIfNullorEmpty(srcElement.getElementsByTagName(MessageXMLTags.STATUS_TAG)));
		
		dstInfo = new UserInfo(Procedures.getTheStringAndCheckIfNullorEmpty(dstElement.getElementsByTagName(MessageXMLTags.USERNAME_TAG)),
				Procedures.getTheStringAndCheckIfNullorEmpty(dstElement.getElementsByTagName(MessageXMLTags.IP_TAG)),
				Procedures.getTheStringAndCheckIfNullorEmpty(dstElement.getElementsByTagName(MessageXMLTags.PORT_TAG)),
				Procedures.getTheStringAndCheckIfNullorEmpty(dstElement.getElementsByTagName(MessageXMLTags.STATUS_TAG)));
		
		currentMessage = msgElement.getTextContent();
		
		// TODO la data
		
		if (currentMessage.length() == 0)
			throw new XmlMessageReprException("the message is empty");
		
		return new CommunicationMessage(srcInfo, dstInfo, currentMessage, null);
	}
	
	public String toXML() throws ParserConfigurationException, TransformerException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element rootElement = doc.createElement(MessageXMLTags.MESSAGE_TAG);
		rootElement.setAttribute(MessageXMLTags.MESSAGE_TYPE_ATTRIBUTE, MessageXMLTags.MESSAGE_TYPE_COMMUNICATION_MESSAGE);
		
		Element src_user = doc.createElement(MessageXMLTags.SOURCE_TAG);
		Element src_user_username = doc.createElement(MessageXMLTags.USERNAME_TAG);
		src_user_username.setTextContent(src.username);
		Element src_user_ip = doc.createElement(MessageXMLTags.IP_TAG);
		src_user_ip.setTextContent(src.ip);
		Element src_user_port = doc.createElement(MessageXMLTags.PORT_TAG);
		src_user_port.setTextContent(src.port);
		Element src_user_status = doc.createElement(MessageXMLTags.STATUS_TAG);
		src_user_status.setTextContent(src.status);
		src_user.appendChild(src_user_username);
		src_user.appendChild(src_user_ip);
		src_user.appendChild(src_user_port);
		src_user.appendChild(src_user_status);
		
		
		
			
		Element dst_user = doc.createElement(MessageXMLTags.DESTINATION_TAG);
		Element dst_user_username = doc.createElement(MessageXMLTags.USERNAME_TAG);
		dst_user_username.setTextContent(dst.username);
		Element dst_user_ip = doc.createElement(MessageXMLTags.IP_TAG);
		dst_user_ip.setTextContent(dst.ip);
		Element dst_user_port = doc.createElement(MessageXMLTags.PORT_TAG);
		dst_user_port.setTextContent(dst.port);
		Element dst_user_status = doc.createElement(MessageXMLTags.STATUS_TAG);
		dst_user_status.setTextContent(dst.status);
		dst_user.appendChild(dst_user_username);
		dst_user.appendChild(dst_user_ip);
		dst_user.appendChild(dst_user_port);
		dst_user.appendChild(dst_user_status);

		
		
		Element msg = doc.createElement(MessageXMLTags.COMMUNICATION_MESSAGE_SENT_MESSAGE_TAG);
		msg.appendChild(doc.createTextNode(message));
		
		
		rootElement.appendChild(src_user);
		rootElement.appendChild(dst_user);
		rootElement.appendChild(msg);
		
		//TODO la data
		
		doc.appendChild(rootElement);
		
		return Procedures.getXmlString(doc);
	}
	
	@Override
	public String toString() {
		return "src: " + src.toString() + "\n" +
			   "dst: " + dst.toString() + "\n" +
			   "msg: " + message;
		
	}



	public UserInfo getSource() {
		return src;
	}
	
	public UserInfo getDestination() {
		return dst;
	}
	
	public Date getSendingTime() {
		return sendDate;
	}

	public String getMessage() {
		return message;
	}

}
