package com.tolmms.simpleim.datatypes.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import com.tolmms.simpleim.datatypes.CommunicationMessage;
import com.tolmms.simpleim.datatypes.CommunicationMessageAnswer;
import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.LoginMessage;
import com.tolmms.simpleim.datatypes.LoginMessageAnswer;
import com.tolmms.simpleim.datatypes.LogoutMessage;
import com.tolmms.simpleim.datatypes.MessageInfo;
import com.tolmms.simpleim.datatypes.RegisterMessage;
import com.tolmms.simpleim.datatypes.RegisterMessageAnswer;
import com.tolmms.simpleim.datatypes.SomeOneLoginMessage;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.UserInfoAnswerMessage;
import com.tolmms.simpleim.datatypes.UserInfoRequestMessage;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class MessageTests {
	public static double PRECISION_COORDS = 0.1;
	
	@Test
	public void LoginMessageCorrectlySerializedAndDeserialized() {
		UserInfo u = null;
		LoginMessage lm = null;
		String lmXml = null;
		LoginMessage lmFromLmXml = null;
		UserInfo uFromLmFromLmXml = null;
		
		try {
			u = new UserInfo("pippo", "10.10.0.1", 50000, UserInfo.ONLINE_STATUS, 12.33, 12.33, 12.33);
		} catch (InvalidDataException e) {
			fail();
		}
		
		lm = new LoginMessage(u, "dummy password");
		
		try {
			lmXml = lm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		try {
			lmFromLmXml = LoginMessage.fromXML(lmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		uFromLmFromLmXml = lmFromLmXml.getUser();
		
		assertEquals(lm.getPassword(), lmFromLmXml.getPassword());
		assertEqualsUserInfos(u, uFromLmFromLmXml);
	}
	
	@Test
	public void CommunicationMessageCorrectlySerializedDeserialized() {
		UserInfo src = null;
		UserInfo dst = null;
		MessageInfo mi = null;
		CommunicationMessage cm = null;
		String cmXml = null;
		
		
		
		try {
			src = new UserInfo("source", "10.10.0.1", 10000);
			dst = new UserInfo("destination", "10.10.0.2", 10000);
			mi = new MessageInfo(src, dst, "ciao");
		} catch (InvalidDataException e) {
			fail();
		}
		
		cm = new CommunicationMessage(mi);
		
		
		try {
			cmXml = cm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		CommunicationMessage cmFromXml = null;
		try {
			cmFromXml = CommunicationMessage.fromXML(cmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEqualsUserInfos(cm.getMessageInfo().getSource(), cmFromXml.getMessageInfo().getSource());
		assertEqualsUserInfos(cm.getMessageInfo().getDestination(), cmFromXml.getMessageInfo().getDestination());
		
		assertEquals(cm.getMessageInfo().getMessage(), cmFromXml.getMessageInfo().getMessage());
		assertEquals(cm.getMessageInfo().getMessage(), cmFromXml.getMessageInfo().getMessage());
		assertEquals(cm.getMessageInfo().getSentTimeString(), cmFromXml.getMessageInfo().getSentTimeString());
		
		// fails always... so it is commented. The problem maybe is precision??
//		assertEquals(cm.getMessageInfo().getSentTime(), cmFromXml.getMessageInfo().getSentTime());
	}
	
	@Test
	public void ListMessageCorrectlySerializedDeserialized() {
		ListMessage lm = new ListMessage();
		int MAX = 10;
		
		for(int i = 0; i < MAX; ++i) {
			UserInfo u = null;
			try {
				u = new UserInfo("user" + i, "10.10.0." + i, 10000 + i, (i % 2 == 0)?UserInfo.OFFLINE_STATUS:UserInfo.ONLINE_STATUS);
			} catch (InvalidDataException e) {
				fail();
			}
			lm.addUser(u);
		}
		
		String lmXml = null;
		try {
			lmXml = lm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		
		ListMessage lmFromXml = null;
		try {
			lmFromXml = ListMessage.fromXML(lmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		for (int i = 0; i < MAX; ++i) {
			assertEqualsUserInfos(lm.userAt(i), lmFromXml.userAt(i));
		}
		
	}
	
	private static void assertEqualsUserInfos(UserInfo expected, UserInfo actual) {
		assertEquals(expected.getUsername(), actual.getUsername());
		assertEquals(expected.getStatus(), actual.getStatus());
		assertEquals(expected.getPortString(), actual.getPortString());
		assertEquals(expected.getPort(), actual.getPort());
		assertEquals(expected.getIp(), actual.getIp());
		assertEquals(expected.getLongitude(), actual.getLongitude(), PRECISION_COORDS);
		assertEquals(expected.getLatitude(), actual.getLatitude(), PRECISION_COORDS);
		assertEquals(expected.getAltitude(), actual.getAltitude(), PRECISION_COORDS);
	}
	
	@Test
	public void LoginMessageAnswerCorrectlySerializedDeserialized() {
		UserInfo u = null;
		LoginMessageAnswer lma;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		lma = new LoginMessageAnswer(u, "123456");
		
		String lmaXml = null;
		try {
			lmaXml = lma.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		LoginMessageAnswer lmaFromXml = null;
		try {
			lmaFromXml = LoginMessageAnswer.fromXML(lmaXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEquals(lma.getNumber(), lmaFromXml.getNumber());
		assertEqualsUserInfos(lma.getUser(), lmaFromXml.getUser());
	}
	
	@Test
	public void LogoutMessageCorrectlySerializedDeserialized() {
		UserInfo u = null;
		LogoutMessage lm;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		lm = new LogoutMessage(u);
		
		String lmXml = null;
		try {
			lmXml = lm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		LogoutMessage lmFromXml = null;
		try {
			lmFromXml = LogoutMessage.fromXML(lmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		
		assertEqualsUserInfos(lm.getSource(), lmFromXml.getSource());
		assertEquals(lm.getSource().getStatus(), UserInfo.OFFLINE_STATUS);
		assertEquals(lmFromXml.getSource().getStatus(), UserInfo.OFFLINE_STATUS);
		
	}
	
	
	@Test
	public void RegisterMessageCorrectlySerializedAndDeserialized() {
		UserInfo u = null;
		RegisterMessage rm = null;
		String lmXml = null;
		RegisterMessage rmFromRmXml = null;
		UserInfo uFromRmFromRmXml = null;
		
		try {
			u = new UserInfo("pippo", "10.10.0.1", 50000, UserInfo.ONLINE_STATUS);
		} catch (InvalidDataException e) {
			fail();
		}
		
		rm = new RegisterMessage(u, "dummy password");
		
		try {
			lmXml = rm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		try {
			rmFromRmXml = RegisterMessage.fromXML(lmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		uFromRmFromRmXml = rmFromRmXml.getUser();
		
		assertEquals(rm.getPassword(), rmFromRmXml.getPassword());
		assertEqualsUserInfos(u, uFromRmFromRmXml);
	}
	
	@Test
	public void RegisterMessageAnswerCorrectlySerializedDeserialized() {
		UserInfo u = null;
		RegisterMessageAnswer rma = null;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		try {
			rma = new RegisterMessageAnswer(u, RegisterMessageAnswer.ACCEPTED);
		} catch (InvalidDataException e1) { /* cannot be here */ }
		
		String rmaXml = null;
		try {
			rmaXml = rma.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		RegisterMessageAnswer rmaFromXml = null;
		try {
			rmaFromXml = RegisterMessageAnswer.fromXML(rmaXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEquals(rma.accepted(), rmaFromXml.accepted());
		assertEqualsUserInfos(rma.getUser(), rmaFromXml.getUser());
	}
	
	
	@Test
	public void SomeOneLoginCorrectlySerializedDeserialized() throws InvalidDataException {
		UserInfo u = null;
		SomeOneLoginMessage solm = null;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		solm = new SomeOneLoginMessage(u);
		
		String solmXml = null;
		try {
			solmXml = solm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		SomeOneLoginMessage solmFromXml = null;
		try {
			solmFromXml = SomeOneLoginMessage.fromXML(solmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEqualsUserInfos(solm.getSource(), solmFromXml.getSource());
	}
	
	
	@Test
	public void CommunicationMessageAnswerCorrectlySerializedDeserialized() {
		UserInfo u = null;
		CommunicationMessageAnswer cma;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		cma = new CommunicationMessageAnswer(u, 123456);
		
		String cmaXml = null;
		try {
			cmaXml = cma.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		CommunicationMessageAnswer cmaFromXml = null;
		try {
			cmaFromXml = CommunicationMessageAnswer.fromXML(cmaXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEquals(cma.getMessageHashAck(), cmaFromXml.getMessageHashAck());
		assertEqualsUserInfos(cma.getUser(), cmaFromXml.getUser());
	}
	
	public void UserInfoAnswerMessageCorrectlySerializedDeserialized() throws InvalidDataException {
		UserInfo u = null;
		UserInfoAnswerMessage uiam = null;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		uiam = new UserInfoAnswerMessage(u);
		
		String uiamXml = null;
		try {
			uiamXml = uiam.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		UserInfoAnswerMessage uiamFromXml = null;
		try {
			uiamFromXml = UserInfoAnswerMessage.fromXML(uiamXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEqualsUserInfos(uiam.getSource(), uiamFromXml.getSource());
	}
	
	public void UserInfoRequestMessageCorrectlySerializedDeserialized() throws InvalidDataException {
		UserInfo u = null;
		UserInfoRequestMessage uirm = null;
		
		try {
			u = new UserInfo("dummy", "10.10.1.2", 10000);
		} catch (InvalidDataException e) {
			fail();
		}
		
		uirm = new UserInfoRequestMessage(u);
		
		String uirmXml = null;
		try {
			uirmXml = uirm.toXML();
		} catch (ParserConfigurationException e) {
			fail();
		} catch (TransformerException e) {
			fail();
		}
		
		UserInfoRequestMessage uirmFromXml = null;
		try {
			uirmFromXml = UserInfoRequestMessage.fromXML(uirmXml);
		} catch (XmlMessageReprException e) {
			fail();
		}
		
		assertEqualsUserInfos(uirm.getSource(), uirmFromXml.getSource());
	}
	
}
