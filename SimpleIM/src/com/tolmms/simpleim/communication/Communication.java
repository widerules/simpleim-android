package com.tolmms.simpleim.communication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.EmptyStackException;
import java.util.Stack;

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

public class Communication implements ICommunication {
	UdpListenerThread udpListener = null;
	
	String serverIpString = "10.0.2.2";
	private int serverTcpPort = 4445;
	private int serverUdpPort = 4445;
	private int serverTcpTimeout = 1 * 1000; //miliseconds
	private int serverUDPTimeout = 5 * 1000; //miliseconds
	
	int serviceUdpPort = 50000;
	int UDP_BUFFER_LEN = 10 * 1024; // 10 Kbytes
	
	UserInfo myinfo = null;
	
	IAppManagerForComm service = null;

	

	public Communication(IAppManagerForComm service) {
		
		this.service = service;
		
	}

	@Override
	public UserInfo login(String username, String password) 
			throws UsernameOrPasswordException, CommunicationException, UnknownHostException {
		DatagramSocket s = null;
		
		DatagramPacket packet;
		byte[] buf;
		
		String msg = "";
		String answer = "";
		LoginMessage loginMessage = null;
		ListMessage listMessage = null;
		
		LoginMessageAnswer login_message_answer;
		
		if (myinfo != null)
			return myinfo;		
		
		try {
			s = new DatagramSocket(serviceUdpPort);
		} catch (SocketException e1) {
			throw new CommunicationException("opening the datagram socket");
		}
		
		s.connect(InetAddress.getByName(serverIpString), serverUdpPort);
		
		UserInfo tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), String.valueOf(serviceUdpPort));
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
		
		
		service.setUserList(listMessage.getUserList());
		
		tempInfo.setPort(login_message_answer.getUser().getPort());
		tempInfo.setIP(login_message_answer.getUser().getIp());
		
		myinfo = tempInfo;
		
		/*
		Socket s = null;		
		BufferedReader reader = null;
		PrintWriter writer = null;
		
		String msg = "";
		String answer = "";
		LoginMessage loginMessage = null;
		ListMessage listMessage = null;
		
		if (myinfo != null)
			return myinfo;		
		
		
		try {
			s = new Socket(InetAddress.getByName(serverIpString), serverTcpPort);
			reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
		} catch (UnknownHostException e) {
			throw e;
		} catch (IOException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("creating the socket");
		}
		
		
		UserInfo tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), String.valueOf(serviceUdpPort));
		loginMessage = new LoginMessage(tempInfo, password);
		
		try {
			msg = loginMessage.toXML();
			writer.println(msg);
		}  catch (ParserConfigurationException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("sending login message (ParserConfigurationException)");
		} catch (TransformerException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("sending login message (TransformerException)");
		}
		
		try {
			s.setSoTimeout(serverTcpTimeout);
		} catch (SocketException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("setting timeout on socket");
		}
		
		try {
			answer = reader.readLine();
			
			if (answer == null)
				throw new IOException();
	
			if (!LoginMessageAnswer.fromXML(answer).accepted()) {
				if (MainActivity.DEBUG)
					Log.d("Login - got the first answer", "REFUSED");
				tryClose(reader, writer, s);
				throw new UsernameOrPasswordException();
			}
		} catch (XmlMessageReprException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("reciving the answer of login XmlMessageReprException - " + answer);
		} catch (IOException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("reciving the answer of login IOException - "+answer);
		}
		
		answer = null;
		
		try {
			answer = reader.readLine();
			
			if (answer == null)
				throw new IOException();
			
			listMessage = ListMessage.fromXML(answer);
			if (MainActivity.DEBUG) {
				Log.d("Login - got the second answer", "got the users list");
				Log.d("Login - got the second answer", answer);
			}
			
		} catch (XmlMessageReprException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("reciving the second answer of login XmlMessageReprException");
		} catch (IOException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException("reciving the second answer of login IOException");
		}
		
		try {
			reader.close();
			writer.close();
			s.close();
		} catch (IOException e) {
			throw new CommunicationException("closing socket IOException");
		}
		
		service.setUserList(listMessage.getUserList());
		myinfo = tempInfo;
		*/
		
//		announceIAmOnline(listMessage.getUserList());
//		startListeningForMessages();
		
		return myinfo;
	}
	
//	private void announceIAmOnline(Vector<UserInfo> userList) {
//		// TODO Auto-generated method stub
//		
//	}
	
	
	class UdpListenerThread extends Thread {
		DatagramSocket listener = null;
		DatagramPacket d_packet = null;
		HandleRecievedDatagrams handleRecievedDatagrams = null;
		
		public UdpListenerThread(int port) throws SocketException {
			listener = new DatagramSocket(port);
			handleRecievedDatagrams = new HandleRecievedDatagrams();
			
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					byte[] buffer = new byte[UDP_BUFFER_LEN];
					d_packet = new DatagramPacket(buffer, buffer.length);
					
					listener.receive(d_packet);
					
					recieved_datagrams.push(d_packet);
					handleRecievedDatagrams.notify();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		
		
		public void stopListening() {
			listener.close();
			handleRecievedDatagrams.stopHandleMessages();
		}
	}
	
	Stack<DatagramPacket> recieved_datagrams = new Stack<DatagramPacket>();
	
	class HandleRecievedDatagrams extends Thread {
		private boolean canRun = true;
		@Override
		public void run() {
			while (canRun) {
				DatagramPacket dp = null;
				if (recieved_datagrams.isEmpty())
					try {
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				try {
					dp = recieved_datagrams.pop();
				} catch (EmptyStackException e) {
					continue; // the thread was notified but the recieved_datagrams does not contain anything.. so i must stop
				}
				
				String the_msg = new String(dp.getData(), 0, dp.getLength());
				
				
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
			notify();
		}
	}
	


	private void startListeningForMessages() {
		
		
		try {
			udpListener = new UdpListenerThread(serviceUdpPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		udpListener.start();
	}

	private static void tryClose(BufferedReader r, PrintWriter writer, Socket s) {
		try {
			if (r != null) r.close();
			if (writer != null) writer.close();
			if (s != null) s.close();
		} catch (IOException e2) {}
	}
	
	public boolean logout() {
		if (myinfo == null && udpListener == null)
			return true;
		
		//send a logout message;
		
//		udpListener.stopListening();
		udpListener = null;
		myinfo = null;
		udpListener = null;
		return true;
	}

	@Override
	public void register(String username, String password) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException {
		DatagramSocket s = null;
		
		DatagramPacket packet;
		byte[] buf;
		
		String msg = "";
		String answer = "";
		RegisterMessage registerMessage = null;
		
		RegisterMessageAnswer register_message_answer;
		
		//TODO DA RIVEDERE QUI
		if (myinfo != null)
			return;		
		
		try {
			s = new DatagramSocket(serviceUdpPort);
		} catch (SocketException e1) {
			throw new CommunicationException("opening the datagram socket");
		}

		// TODO da mettere al posto di s.connect(,) mettere quella con un solo paramentro...
		s.connect(InetAddress.getByName(serverIpString), serverUdpPort);
		
		UserInfo tempInfo = new UserInfo(username, s.getLocalAddress().getHostAddress(), String.valueOf(serviceUdpPort));
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
		
		/*
		Socket s = null;		
		BufferedReader reader = null;
		PrintWriter writer = null;
		
		String msg = "";
		String answer = "";
		RegisterMessage regMessage = null;
		
		if (myinfo != null)
			return;
		
		try {
			s = new Socket(InetAddress.getByName(serverIpString), serverTcpPort);
			reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
		} catch (UnknownHostException e) {
			throw e;
		} catch (IOException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException();
		}
		
		
		regMessage = new RegisterMessage(new UserInfo(username, 
													s.getLocalAddress().getHostAddress(), 
													String.valueOf(serviceUdpPort)), password);
		
		try {
			msg = regMessage.toXML();
			writer.println(msg);
		}  catch (ParserConfigurationException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException();
		} catch (TransformerException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException();
//		} catch (IOException e) {
//			tryClose(reader, writer, s);
//			throw new CommunicationException();
		}
		
		try {
			s.setSoTimeout(serverTcpTimeout);
		} catch (SocketException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException();
		}
		
		try {
			answer = reader.readLine();
			if (!RegisterMessageAnswer.fromXML(answer).accepted()) {
				tryClose(reader, writer, s);
				throw new UsernameAlreadyExistsException();
			}
		} catch (XmlMessageReprException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException();
		} catch (IOException e) {
			tryClose(reader, writer, s);
			throw new CommunicationException();
		}
		
		try {
			reader.close();
			writer.close();
			s.close();
		} catch (IOException e) {
			throw new CommunicationException();
		}
		*/
	}

	@Override
	public void sendMessage(UserInfo myInfo, UserInfo user_to_chat, String the_message) {
		// TODO da fare
		
	}

}
