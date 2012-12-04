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
	int serverTcpPort = 4445;
	int serverTcpTimeout = 1 * 1000; //miliseconds
	
	int serviceUdpPort = 50000;
	int UDP_BUFFER_LEN = 4096; // bytes
	
	UserInfo myinfo = null;
	
	IAppManagerForComm service = null;

	public Communication(IAppManagerForComm service) {
		
		this.service = service;
		
	}

	@Override
	public boolean login(String username, String password) 
			throws UsernameOrPasswordException, CommunicationException, UnknownHostException {
		Socket s = null;		
		BufferedReader reader = null;
		PrintWriter writer = null;
		
		String msg = "";
		String answer = "";
		LoginMessage loginMessage = null;
		ListMessage listMessage = null;
		
		if (myinfo != null)
			return true;		
		
		
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
		
		
//		announceIAmOnline(listMessage.getUserList());
//		startListeningForMessages();
		
		return true;
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
		
		udpListener.stopListening();
		udpListener = null;
		myinfo = null;
		udpListener = null;
		return true;
	}

	@Override
	public void register(String username, String password, String email) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException {
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
	}

}
