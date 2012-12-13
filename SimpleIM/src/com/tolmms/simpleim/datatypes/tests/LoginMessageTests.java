package com.tolmms.simpleim.datatypes.tests;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import com.tolmms.simpleim.datatypes.LoginMessage;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

import junit.framework.TestCase;

public class LoginMessageTests extends TestCase {
	
	@Test
	public void LoginMessageWithUserInfoSerializedDeserializedContainsTheCorrectUserInfo() {
		UserInfo u = null;
		LoginMessage lm = null;
		String lmXml = null;
		LoginMessage lmFromLmXml = null;
		UserInfo uFromLmFromLmXml = null;
		
//		try {
//			u = new UserInfo("pippo", "10.10.0.1", 50000, UserInfo.ONLINE_STATUS, 12.33, 12.33, 12.33);
//		} catch (InvalidDataException e) {
//			fail();
//		}
//		
//		lm = new LoginMessage(u, "dummy password");
//		
//		try {
//			lmXml = lm.toXML();
//		} catch (ParserConfigurationException e) {
//			fail();
//		} catch (TransformerException e) {
//			fail();
//		}
//		
//		try {
//			lmFromLmXml = LoginMessage.fromXML(lmXml);
//		} catch (XmlMessageReprException e) {
//			fail();
//		}
//		
//		uFromLmFromLmXml = lmFromLmXml.getUser();
//		
//		assertEquals(lmFromLmXml.getPassword(), lm.getPassword());
//		assertEquals(uFromLmFromLmXml.getUsername(), u.getUsername());
//		assertEquals(uFromLmFromLmXml.getIp(), u.getIp());
//		assertEquals(uFromLmFromLmXml.getPort(), u.getPort());
//		assertEquals(uFromLmFromLmXml.getPortString(), u.getPortString());
//		assertEquals(uFromLmFromLmXml.getStatus(), u.getStatus());
//		assertEquals(uFromLmFromLmXml.getLatitude(), u.getLatitude());
//		assertEquals(uFromLmFromLmXml.getLongitude(), u.getLongitude());
//		assertEquals(uFromLmFromLmXml.getAltitude(), u.getAltitude());		
	}
}
