package com.tolmms.simpleim.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import android.util.Log;

import com.tolmms.simpleim.MainActivity;
import com.tolmms.simpleim.datatypes.CommunicationMessage;
import com.tolmms.simpleim.datatypes.CommunicationMessageAnswer;
import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.LoginMessage;
import com.tolmms.simpleim.datatypes.LoginMessageAnswer;
import com.tolmms.simpleim.datatypes.LogoutMessage;
import com.tolmms.simpleim.datatypes.MessageInfo;
import com.tolmms.simpleim.datatypes.Procedures;
import com.tolmms.simpleim.datatypes.RegisterMessage;
import com.tolmms.simpleim.datatypes.RegisterMessageAnswer;
import com.tolmms.simpleim.datatypes.SomeOneLoginMessage;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.UserInfoAnswerMessage;
import com.tolmms.simpleim.datatypes.UserInfoRequestMessage;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;
import com.tolmms.simpleim.interfaces.IAppManagerForComm;
import com.tolmms.simpleim.interfaces.ICommunication;

public class Communication implements ICommunication {
/*
 * be aware that with loopback net interface it does not sends/recieves messages :(
 */
//	String serverIpString = "10.0.2.2";
	String serverIpString = "192.168.1.6"; /* this is my home ip - the home internet */
										   /* now I have another ip - note that the loopback don't work as it should */
//	String serverIpString = "192.168.0.184";
	
	private int serverUdpPort = 4445; /* the server port - default */
	private int serverUDPTimeout = 5 * 1000; //miliseconds
	
	int serviceUdpPort = 50000;
	int UDP_BUFFER_LEN = 10 * 1024; // 10 Kbytes
	
	IAppManagerForComm service = null;
	
	BlockingQueue<DatagramPacket> outgoingPackets = null;
	BlockingQueue<DatagramPacket> incomingPackets = null;
	
	HandleIncomingPackets handleIncomingPackets = null;
	HandleRequests handleRequests = null;
	HandleOutgoingPackets handlingOutgoingPackets = null;

	public Communication(IAppManagerForComm service) {
		this.service = service;
	}
	
	
	/* security: must be secure that I communicate only with server! */
	@Override
	public UserInfo login(String username, String password) 
			throws UsernameOrPasswordException, 
					CommunicationException, 
					UnknownHostException, 
					UnableToStartSockets {
		DatagramSocket s = null;
		DatagramPacket packet = null;
		byte[] buf = null;
		
		
		LoginMessage loginMessage = null;
		ListMessage listMessage = null;
		LoginMessageAnswer login_message_answer = null;
		
		String msg = "";
		String answer = "";
		
		try {
			s = new DatagramSocket(serviceUdpPort);
		} catch (SocketException e1) {
			throw new CommunicationException("opening the datagram socket");
		}
		
		UserInfo tempInfo= null;
		try {
			tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), serviceUdpPort);
		} catch (InvalidDataException e1) { /* cannot be here */ }
		
		
		loginMessage = new LoginMessage(tempInfo, password);
		
		try {
			msg = loginMessage.toXML();
			
			packet = new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(serverIpString), serverUdpPort);
			
			s.send(packet);
		
		}  catch (ParserConfigurationException e) {
			s.close();
			throw new CommunicationException("sending login message (ParserConfigurationException)");
		} catch (TransformerException e) {
			s.close();
			throw new CommunicationException("sending login message (TransformerException)");
		} catch (IOException e) {
			s.close();
			throw new CommunicationException("sending login message (IOException)");
		}
		
		try {
			s.setSoTimeout(serverUDPTimeout);
		} catch (SocketException e) {
			s.close();
			throw new CommunicationException("setting timeout on socket");
		}
		
		try {
			buf = new byte[UDP_BUFFER_LEN];
			packet = new DatagramPacket(buf, buf.length);
			
			s.receive(packet);
			answer = new String(buf).trim();
			
			if (answer == null)
				throw new IOException();
	
			login_message_answer = LoginMessageAnswer.fromXML(answer);
			if (!login_message_answer.accepted()) {
				if (MainActivity.DEBUG)
					Log.d("Login - got the first answer", "REFUSED");
				s.close();
				throw new UsernameOrPasswordException();
			}
			
			if (!login_message_answer.getUser().getUsername().equals(username)) {
				if (MainActivity.DEBUG)
					Log.d("Login - got the first answer", "Username is not mine!!");
				s.close();
				throw new CommunicationException("Username is not mine!!");
			}
				
		} catch (XmlMessageReprException e) {
			s.close();
			throw new CommunicationException("reciving the answer of login XmlMessageReprException - " + answer);
		} catch (IOException e) {
			s.close();
			throw new CommunicationException("reciving the answer of login IOException - "+answer);
		}
		
		answer = null;
		
		try {
			buf = new byte[UDP_BUFFER_LEN];
			packet = new DatagramPacket(buf, buf.length);
			s.receive(packet);
			answer = new String(buf).trim();
			
			if (answer == null)
				throw new IOException();
			
			listMessage = ListMessage.fromXML(answer);
			if (MainActivity.DEBUG) {
				Log.d("Login - got the second answer", "got the users list");
				Log.d("Login - got the second answer", answer);
			}
			
		} catch (XmlMessageReprException e) {
			s.close();
			throw new CommunicationException("reciving the second answer of login XmlMessageReprException");
		} catch (IOException e) {
			s.close();
			throw new CommunicationException("reciving the second answer of login IOException");
		}
		
		s.close();
			
		
		try {
			startListeningForMessages();
		} catch (SocketException e) {
			if (MainActivity.DEBUG)
				Log.d("Login - communication - starting sockets", "errore in communication");
			
			stopListeningForMessages();
			throw new UnableToStartSockets("cannot start the datagram sockets...");
		}
		
		
		service.setUserList(listMessage.getUserList());
		
		tempInfo.setPort(login_message_answer.getUser().getPort());
		tempInfo.setIP(login_message_answer.getUser().getIp());
		
		
		if (MainActivity.DEBUG)
			Log.d("Login - communication - tempInfo", String.valueOf(tempInfo.getIp()) + ":" + String.valueOf(tempInfo.getPort()));
		
		
		return tempInfo;
	}
	
	public void announceIAmOnline(UserInfo me, List<UserInfo> userList) {
		SomeOneLoginMessage msg = new SomeOneLoginMessage(me);
		String msgString;
		
		try {
			msgString = msg.toXML();
		} catch (ParserConfigurationException e1) {
			//should never come here
			return;
		} catch (TransformerException e1) {
			//should never come here
			return;
		}
		
		for (UserInfo userInfo : userList) {
			if (!userInfo.isOnline())
				continue;
			
			InetAddress address;
			int port;
			
			try {
				address = InetAddress.getByName(userInfo.getIp());
				port = userInfo.getPort();
			} catch (UnknownHostException e) {
				continue;
			}
			
			outgoingPackets.add(new DatagramPacket(msgString.getBytes(), 
													msgString.getBytes().length,
													address, port));
		}		
	}
	
	/* security: must be secure that I communicate only with server! */
	@Override
	public void register(String username, String password) 
			throws CommunicationException, 
					UsernameAlreadyExistsException, 
					UnknownHostException {
		DatagramSocket s = null;
		
		DatagramPacket packet;
		byte[] buf;
		
		String msg = "";
		String answer = "";
		RegisterMessage registerMessage = null;
		
		RegisterMessageAnswer register_message_answer = null;
		
		try {
			s = new DatagramSocket(serviceUdpPort);
		} catch (SocketException e1) {
			throw new CommunicationException("opening the datagram socket");
		}

		UserInfo tempInfo= null;
		try {
			tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), serviceUdpPort);
		} catch (InvalidDataException e1) { /* cannot be here */ }
		
		registerMessage = new RegisterMessage(tempInfo, password);
		
		try {
			msg = registerMessage.toXML();
			
			packet = new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName(serverIpString), serverUdpPort);
			
			s.send(packet);
		
		}  catch (ParserConfigurationException e) {
			s.close();
			throw new CommunicationException("sending register message (ParserConfigurationException)");
		} catch (TransformerException e) {
			s.close();
			throw new CommunicationException("sending register message (TransformerException)");
		} catch (IOException e) {
			s.close();
			throw new CommunicationException("sending register message (IOException)");
		}
		
		try {
			s.setSoTimeout(serverUDPTimeout);
		} catch (SocketException e) {
			s.close();
			throw new CommunicationException("setting timeout on socket");
		}
		
		try {
			buf = new byte[UDP_BUFFER_LEN];
			packet = new DatagramPacket(buf, buf.length);
			
			s.receive(packet);
			answer = new String(buf).trim();
			
			if (answer == null)
				throw new IOException();
	
			register_message_answer = RegisterMessageAnswer.fromXML(answer);
			if (!register_message_answer.accepted()) {
				if (MainActivity.DEBUG)
					Log.d("register - got the answer", "REFUSED");
				s.close();
				throw new UsernameAlreadyExistsException();
			}
		} catch (XmlMessageReprException e) {
			s.close();
			throw new CommunicationException("reciving the answer of register XmlMessageReprException - " + answer);
		} catch (IOException e) {
			s.close();
			throw new CommunicationException("reciving the answer of register IOException - "+answer);
		}
		
		s.close();
	}

	public void logout(UserInfo source, List<UserInfo> userList) {		
		stopListeningForMessages();
		
		LogoutMessage lm = new LogoutMessage(source);
		String lmXml = null;
		
		try {
			lmXml = lm.toXML();
		} catch (ParserConfigurationException e) {
		} catch (TransformerException e) {
		}
		
		if (lmXml == null)
			return; //OOPS
		
		DatagramSocket s;
		
		try {
			s = new DatagramSocket();
		} catch (SocketException e1) {
			return;
		}
		
		for (UserInfo ui : userList) {
			if (!ui.isOnline())
				continue;
			DatagramPacket p;
			try {
				p = new DatagramPacket(lmXml.getBytes(), lmXml.getBytes().length, 
													InetAddress.getByName(ui.getIp()),
													ui.getPort());
			} catch (UnknownHostException e) {
				/* if there's error... nothing to do */
				continue;
			}
			
			try {
				s.send(p);
			} catch (IOException e) {
				/* if there's error... nothing to do */
				continue;
			}
			
		}
		
		/* send logout message also to server! */
		DatagramPacket p = null;
		try {
			p = new DatagramPacket(lmXml.getBytes(), lmXml.getBytes().length, 
												InetAddress.getByName(serverIpString),
												serverUdpPort);
		} catch (UnknownHostException e) {
			s.close(); 
			return;
		
		}
		
		try {
			s.send(p);
		} catch (IOException e) {
			/* if there's error... nothing to do */
			s.close(); 
			return;
		}
		
		s.close();
	}

	@Override
	public void sendMessage(MessageInfo mi) 
			throws CannotSendBecauseOfWrongUserInfo {
		CommunicationMessage m = new CommunicationMessage(mi);
		
		String mXml = null;
		try {
			mXml = m.toXML();
		} catch (ParserConfigurationException e1) {
			/* cannot be here */
			throw new CannotSendBecauseOfWrongUserInfo();
		} catch (TransformerException e1) {
			/* cannot be here */
			throw new CannotSendBecauseOfWrongUserInfo();
		}
		
		DatagramPacket p;
		try {
			p = new DatagramPacket(mXml.getBytes(), mXml.getBytes().length, 
												InetAddress.getByName(mi.getDestination().getIp()),
												mi.getDestination().getPort());
		} catch (UnknownHostException e) {
			throw new CannotSendBecauseOfWrongUserInfo();
		}
		
		outgoingPackets.add(p);
	}
	
	@Override
	public void sendMessageAck(UserInfo source, UserInfo destination, int hashCode) {
		CommunicationMessageAnswer cma = new CommunicationMessageAnswer(source, hashCode);
		String cmaXml = null;
		try {
			cmaXml = cma.toXML();
		} catch (ParserConfigurationException e1) {
			/* unlikely to be here */
			return;
		} catch (TransformerException e1) {
			/* unlikely to be here */
			return;
		}
		
		DatagramPacket p;
		try {
			p = new DatagramPacket(cmaXml.getBytes(), cmaXml.getBytes().length, 
												InetAddress.getByName(destination.getIp()),
												destination.getPort());
		} catch (UnknownHostException e) {
			/* if there's error... nothing to do */
			return;
		}
		
		outgoingPackets.add(p);
	}

	@Override
	public void sendUserInfoRequest(UserInfo source, UserInfo destination) {
		UserInfoRequestMessage uirm = new UserInfoRequestMessage(source);
		
		String uirmXml = null;
		try {
			uirmXml = uirm.toXML();
		} catch (ParserConfigurationException e1) {
			/* unlikely to be here */
			return;
		} catch (TransformerException e1) {
			/* unlikely to be here */
			return;
		}
		
		DatagramPacket p;
		try {
			p = new DatagramPacket(uirmXml.getBytes(), uirmXml.getBytes().length, 
												InetAddress.getByName(destination.getIp()),
												destination.getPort());
		} catch (UnknownHostException e) {
			/* if there's error... nothing to do */
			return;
		}
		
		outgoingPackets.add(p);
	}

	@Override
	public void sendUserInfoAnswer(UserInfo source, UserInfo destination) {
		UserInfoAnswerMessage uiam = new UserInfoAnswerMessage(source);
		
		String uiamXml = null;
		try {
			uiamXml = uiam.toXML();
		} catch (ParserConfigurationException e1) {
			/* unlikely to be here */
			return;
		} catch (TransformerException e1) {
			/* unlikely to be here */
			return;
		}
		
		DatagramPacket p;
		
		String address;
		int port;
		
		/* ATTENTION:
		 * if the userinfo destination is the server... then I must put the 
		 * server's ip and port 
		 */
		if (destination.getUsername().equals(UserInfo.SERVER_USERNAME)) {
			address = serverIpString;
			port = serverUdpPort;
		} else {
			address = destination.getIp();
			port = destination.getPort();
		}
		
		try {
			p = new DatagramPacket(uiamXml.getBytes(), uiamXml.getBytes().length, 
												InetAddress.getByName(address),
												port);
		} catch (UnknownHostException e) {
			/* if there's error... nothing to do */
			return;
		}
		
		outgoingPackets.add(p);
	}
	
	
	class HandleIncomingPackets extends Thread {
		private boolean canRun = true;
		private DatagramSocket inSocket = null;
		private DatagramPacket dp = null;
		
		public HandleIncomingPackets(int port) throws SocketException {
			inSocket = new DatagramSocket(port);
		}
		
		@Override
		public void run() {
			while (canRun) {
				byte[] buffer = new byte[UDP_BUFFER_LEN];
				dp = new DatagramPacket(buffer, buffer.length);

				if (inSocket.isClosed())
					return;
				
				try {
					inSocket.receive(dp);
				} catch (IOException e) { }

				if (MainActivity.DEBUG)
					Log.d("Communication - HandleIncomingPackets", "recieved a packet");

				incomingPackets.add(dp);
			}
		}
		
		public void stopHandleIncomingPackets() {
			canRun = false;
			inSocket.close();
		}
	}
	
	class HandleOutgoingPackets extends Thread {
		private boolean canRun = true;
		private DatagramSocket outSocket = null;
		private DatagramPacket dp = null;
		
		public HandleOutgoingPackets() throws SocketException {
			outSocket = new DatagramSocket();
		}
		public HandleOutgoingPackets(int port) throws SocketException {
			outSocket = new DatagramSocket(port);
		}
		
		@Override
		public void run() {
			while (canRun) {
				dp = null;
				
				try {
					dp = outgoingPackets.take();
				} catch (InterruptedException e1) { }
				
				if (dp == null)
					continue;

				if (outSocket.isClosed())
					return;
				
				try {
					outSocket.send(dp);
					
					if (MainActivity.DEBUG)
						Log.d("HandleOutgoingPackets", "sent a packet to " + dp.getAddress().getHostAddress() + ":" + dp.getPort());
					
				} catch (IOException e) { }
			}
		}
		
		public void stopHandleOutgoingPackets() {
			canRun = false;
			outSocket.close();
		}
	}	
	
	class HandleRequests extends Thread {
		private boolean canRun = true;
		
		@Override
		public void run() {
			while (canRun) {
				DatagramPacket dp = null;
				
				try {
					dp = incomingPackets.take();
				} catch (InterruptedException e2) {
					continue;
				}
				
				String the_msg = new String(dp.getData(), 0, dp.getLength());
				
				String message_type = null;
				try {
					message_type = Procedures.getMessageType(the_msg);
				} catch (XmlMessageReprException e1) {
					continue;
				}
				
				if (message_type == null)
					continue;
				
				if (Procedures.isCommunicationMessage(message_type)) {
					CommunicationMessage cm = null;
					try {
						 cm = CommunicationMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) { 
						continue;
					}
					
					service.recievedMessage(cm.getMessageInfo());
				} else if (Procedures.isLogoutMessage(message_type)) {
					LogoutMessage solm = null;
					try {
						solm = LogoutMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}
					
					service.userLoggedOut(solm.getSource());
				} else if (Procedures.isSomeOneLoginMessage(message_type)) {
					SomeOneLoginMessage solm = null;
					
					try {
						solm = SomeOneLoginMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}
					
					service.userLoggedIn(solm.getSource());
				} else if (Procedures.isCommunicationMessageAnswer(message_type)) {
					CommunicationMessageAnswer cma = null;
					
					try {
						cma = CommunicationMessageAnswer.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}
					
					service.receivedMessageAnswer(cma.getUser(), cma.getMessageHashAck());
				} else if (Procedures.isUserInfoRequestMessage(message_type)) {
					UserInfoRequestMessage uirm = null;
					
					try {
						uirm = UserInfoRequestMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}
					
					service.receivedUserInfoRequest(uirm.getSource());
					
				} else if (Procedures.isUserInfoAnswerMessage(message_type)) {
					UserInfoAnswerMessage uiam = null;
					
					try {
						uiam = UserInfoAnswerMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}
					service.receivedUserInfoAnswer(uiam.getSource());
					
				}
			}
		}
		public void stopHandleMessages() {
			canRun = false;
		}
	}
	
	/*
	 * private methods
	 */
	private void startListeningForMessages() throws SocketException {
		incomingPackets = new LinkedBlockingQueue<DatagramPacket>();
		outgoingPackets = new LinkedBlockingQueue<DatagramPacket>();
		handleIncomingPackets = new HandleIncomingPackets(serviceUdpPort);
		handleRequests = new HandleRequests();
		handlingOutgoingPackets = new HandleOutgoingPackets();
		
		handleIncomingPackets.start();
		handleRequests.start();
		handlingOutgoingPackets.start();
	}
	
	private void stopListeningForMessages() {
		if (handleIncomingPackets != null)
			handleIncomingPackets.stopHandleIncomingPackets();
		handleIncomingPackets = null;
		
		if (handlingOutgoingPackets != null)
			handlingOutgoingPackets.stopHandleOutgoingPackets();
		handlingOutgoingPackets = null;
		
		if (handleRequests != null)
			handleRequests.stopHandleMessages();
		handleRequests = null;
		
		outgoingPackets = null;
		incomingPackets = null;
	}
	

}
