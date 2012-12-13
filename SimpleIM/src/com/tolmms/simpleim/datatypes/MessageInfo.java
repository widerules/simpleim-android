package com.tolmms.simpleim.datatypes;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class MessageInfo {
	protected UserInfo src;
	protected UserInfo dst;
	protected String message;
	protected Date sentTime;
	
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY);
	
	public MessageInfo(UserInfo src, UserInfo dst, String message, Date sentTime) 
			throws InvalidDataException {
		this.src = src;
		this.dst = dst;
		this.message = message;
		this.sentTime = sentTime;
		
		if (message.isEmpty())
			throw new InvalidDataException();
	}
	
	public MessageInfo(UserInfo src, UserInfo dst, String message) 
			throws InvalidDataException {
		this(src, dst, message, Calendar.getInstance().getTime());
	}
	
	public UserInfo getSource() {
		return src;
	}
	
	public UserInfo getDestination() {
		return dst;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Date getSentTime() {
		return sentTime;
	}
	
	public String getSentTimeString() {
		return DATE_FORMAT.format(sentTime);
	}
	
	protected void toXML(Element rootElement, Document doc) {
		Element e_src = doc.createElement(MessageXMLTags.SOURCE_TAG);
		src.toXML(e_src, doc);
		
		Element e_dst = doc.createElement(MessageXMLTags.DESTINATION_TAG);
		dst.toXML(e_dst, doc);
		
		Element e_message = doc.createElement(MessageXMLTags.COMMUNICATION_MESSAGE_SENT_MESSAGE_TAG);
		e_message.setTextContent(message);
		
		Element e_sent_time = doc.createElement(MessageXMLTags.COMMUNICATION_MESSAGE_SENT_TIME_TAG);
		e_sent_time.setTextContent(getSentTimeString());
				
		rootElement.appendChild(e_src);
		rootElement.appendChild(e_dst);
		rootElement.appendChild(e_message);
		rootElement.appendChild(e_sent_time);
		
	}
	
	protected static MessageInfo fromXML(Element rootElement) throws XmlMessageReprException, InvalidDataException {
		UserInfo src;
		UserInfo dst;
		String message;
		Date sentTime;
		
		
		Element e_src = (Element) rootElement.getElementsByTagName(MessageXMLTags.SOURCE_TAG).item(0);
		Element e_dst = (Element) rootElement.getElementsByTagName(MessageXMLTags.DESTINATION_TAG).item(0);
		Element e_message = (Element) rootElement.getElementsByTagName(MessageXMLTags.COMMUNICATION_MESSAGE_SENT_MESSAGE_TAG).item(0);
		Element e_sent_time = (Element) rootElement.getElementsByTagName(MessageXMLTags.COMMUNICATION_MESSAGE_SENT_TIME_TAG).item(0);
		
		
		if (e_src == null || e_dst == null || e_message == null || e_sent_time == null) 
			throw new XmlMessageReprException("src or dest or msg are null");
		
		
		try {
			src = UserInfo.fromXML(e_src);
			dst = UserInfo.fromXML(e_dst);
		} catch (NumberFormatException e) {
			throw new XmlMessageReprException();
		} catch (InvalidDataException e) {
			throw new XmlMessageReprException();
		}
		
		message = e_message.getTextContent();
		
		try {
			sentTime = DATE_FORMAT.parse(e_sent_time.getTextContent());
		} catch (ParseException e) {
			throw new InvalidDataException();
		}
		
		return new MessageInfo(src, dst, message, sentTime);
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dst == null) ? 0 : dst.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result
				+ ((sentTime == null) ? 0 : sentTime.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageInfo other = (MessageInfo) obj;
		if (dst == null) {
			if (other.dst != null)
				return false;
		} else if (!dst.equals(other.dst))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (sentTime == null) {
			if (other.sentTime != null)
				return false;
		} else if (!sentTime.equals(other.sentTime))
			return false;
		if (src == null) {
			if (other.src != null)
				return false;
		} else if (!src.equals(other.src))
			return false;
		return true;
	}
	
	
	
	
}
