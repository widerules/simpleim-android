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
import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.LoginMessage;
import com.tolmms.simpleim.datatypes.LoginMessageAnswer;
import com.tolmms.simpleim.datatypes.LogoutMessage;
import com.tolmms.simpleim.datatypes.Procedures;
import com.tolmms.simpleim.datatypes.RegisterMessage;
import com.tolmms.simpleim.datatypes.RegisterMessageAnswer;
import com.tolmms.simpleim.datatypes.SomeOneLoginMessage;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;
import com.tolmms.simpleim.interfaces.IAppManagerForComm;
import com.tolmms.simpleim.interfaces.ICommunication;
import com.tolmms.simpleim.storage.TemporaryStorage;

public class Communication implements ICommunication {
	HandleIncomingPackets udpListener = null;
	
	String serverIpString = "10.0.2.2";
	private int serverUdpPort = 4445;
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

	@Override
	public UserInfo login(String username, String password) throws UsernameOrPasswordException, CommunicationException, UnknownHostException, UnableToStartSockets {
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
		
//		s.connect(InetAddress.getByName(serverIpString), serverUdpPort);
		
		UserInfo tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), serviceUdpPort);
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
			
			//TODO chiudere tutte le comunicazioni
			throw new UnableToStartSockets("cannot start the datagram sockets...");
		}
		
		//TODO dire a tutti gli altri??? qui o dopo?
		
//		announceIAmOnline(listMessage.getUserList());
		if (MainActivity.DEBUG)
			Log.d("Login - communication - tempInfo", String.valueOf(tempInfo.getIp()) + ":" + String.valueOf(tempInfo.getPort()));
		
		
		
		service.setUserList(listMessage.getUserList());
		
		tempInfo.setPort(login_message_answer.getUser().getPort());
		tempInfo.setIP(login_message_answer.getUser().getIp());
		
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
				port = Integer.valueOf(userInfo.getPort());
			} catch (UnknownHostException e) {
				continue;
			} catch (NumberFormatException e) {
				continue;
			}
			
			outgoingPackets.add(new DatagramPacket(msgString.getBytes(), 
													msgString.getBytes().length,
													address, port));
		}		
	}
	
	//TODO - devo assicurarmi che con chi comunico sia veramente il server... lol
	@Override
	public void register(String username, String password) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException {
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

		// TODO da mettere al posto di s.connect(,) mettere quella con un solo paramentro...
//		s.connect(InetAddress.getByName(serverIpString), serverUdpPort);
		
		UserInfo tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), serviceUdpPort);
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

	public boolean logout() {
		if (!service.isUserLoggedIn())
			return true;		
		
		stopListeningForMessages();
		
		//TODO send a logout message;
		
		return true;
	}

	@Override
	public void sendMessage(UserInfo myInfo, UserInfo user_to_chat, String the_message) throws CannotSendBecauseOfWrongUserInfo {
		CommunicationMessage m = new CommunicationMessage(TemporaryStorage.myInfo, user_to_chat, the_message, null);
		
		String mXml = m.toString();
		
		DatagramPacket p;
		try {
			p = new DatagramPacket(mXml.getBytes(), mXml.getBytes().length, 
												InetAddress.getByName(user_to_chat.getIp()),
												Integer.valueOf(user_to_chat.getPort()));
		} catch (NumberFormatException e) {
			throw new CannotSendBecauseOfWrongUserInfo();
		} catch (UnknownHostException e) {
			throw new CannotSendBecauseOfWrongUserInfo();
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

				if (MainActivity.DEBUG)
					Log.d("Communication - ", "wayting for a packet");

				if (inSocket.isClosed())
					return;
				
				try {
					inSocket.receive(dp);
				} catch (IOException e) { }

				if (MainActivity.DEBUG)
					Log.d("Communication - ", "recieved a packet");

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
				
				if (MainActivity.DEBUG) {
					Log.d("HandleRequests - message received", the_msg);
				}
				
				String message_type = null;
				try {
					message_type = Procedures.getMessageType(the_msg);
				} catch (XmlMessageReprException e1) {
					continue;
				}
				
				if (Procedures.isCommunicationMessage(message_type)) {
					CommunicationMessage cm = null;
					try {
						 cm = CommunicationMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) { 
						continue;
					}
					
					service.recievedMessage(cm.getSource(), cm.getMessage());
					
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
					
				}
				
				
				//do stuff
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
		outgoingPackets = new LinkedBlockingQueue<DatagramPacket>();
		incomingPackets = new LinkedBlockingQueue<DatagramPacket>();
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
