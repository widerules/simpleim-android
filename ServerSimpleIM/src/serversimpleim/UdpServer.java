package serversimpleim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import serversimpleim.datatypes.SimpleIMUser;

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

public class UdpServer extends BaseServer {
    public static final int UDP_BUFFER = 10 * 1024; // 10Kb
    
    DatagramSocket inSocket = null;
    DatagramSocket outSocket = null;
    
    BlockingQueue<DatagramPacket> incomingRequests = null;
    BlockingQueue<DatagramPacket> outgoingRequests = null;

    HandleIncomingPackets handleIncomingPackets = null;
    HandleRequests handleRequests = null;
    HandleOutgoingPackets handleOutgoingPackets = null;



    public UdpServer() throws IOException {
    	this(4445);
    }


    public UdpServer(int port) throws IOException {
        super();
        inSocket = new DatagramSocket(port);
        outSocket = new DatagramSocket();
        
        incomingRequests = new LinkedBlockingQueue<>();
        outgoingRequests = new LinkedBlockingQueue<>();
    }

    public void run() {	    	  
    	handleIncomingPackets = new HandleIncomingPackets();
    	handleRequests = new HandleRequests();
        handleOutgoingPackets = new HandleOutgoingPackets();
        

        handleIncomingPackets.start();
        handleRequests.start();
        handleOutgoingPackets.start();
        
    }
	    
	private class HandleIncomingPackets extends Thread {
		private boolean canRun = true;
		private byte[] buf;
		private DatagramPacket packet;

		@Override
		public void run() {
			while (canRun) {
				
				buf = new byte[UDP_BUFFER];
				packet = new DatagramPacket(buf, buf.length);

				try {
					inSocket.receive(packet);
				} catch (IOException e2) {
					if (DEBUG)
						System.out.println("ops... receiving a packet from socket");
					continue;
				}

				incomingRequests.add(packet);
			}
		}
		
		public void stopHandleMessages() {
			canRun = false;
//			notify();
		}
	}
	    
	private class HandleOutgoingPackets extends Thread {
		private boolean canRun = true;

		@Override
		public void run() {
			while (canRun) {
				DatagramPacket dp = null;
				
				try {
					dp = outgoingRequests.take();
				} catch (InterruptedException e1) { }
				
				if (dp == null)
					continue;

				try {
					outSocket.send(dp);
				} catch (IOException e) {
					if (DEBUG)
						System.out.println("could not send a message: " + dp.toString());
				}
			}
		}

		public void stopHandleMessages() {
			canRun = false;
//			notify();
		}

	}
	    
	private class HandleRequests extends Thread {
		private boolean canRun = true;

		@Override
		public void run() {
			while (canRun) {
				DatagramPacket dp = null;
				
				try {
					dp = incomingRequests.take();
				} catch (InterruptedException e1) { }
				
				if (dp == null)
					continue;

				String the_msg = new String(dp.getData(), 0, dp.getLength());
				the_msg = the_msg.trim();

				String message_type = null;
				try {
					message_type = Procedures.getMessageType(the_msg);
				} catch (XmlMessageReprException e) {
					if (DEBUG)
						System.out.println("Cannot get the message type :( " + e.getMessage());
					continue;
				}

				if (message_type == null) {
					if (DEBUG)
						System.out.println("message type is null");
					continue;
				}

				if (Procedures.isLoginMessage(message_type)) {
					manageLoginRequest(dp);
				} else if (Procedures.isRegisterMessage(message_type)) {
					manageRegisterRequest(dp);
				} else if (Procedures.isCommunicationMessage(message_type)) {
					CommunicationMessage cm = null;
					try {
						cm = CommunicationMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}

				} else if (Procedures.isLogoutMessage(message_type)) {
					LogoutMessage solm = null;
					try {
						solm = LogoutMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}

				} else if (Procedures.isSomeOneLoginMessage(message_type)) {
					SomeOneLoginMessage solm = null;

					try {
						solm = SomeOneLoginMessage.fromXML(the_msg);
					} catch (XmlMessageReprException e) {
						continue;
					}

				}

				// do stuff
			}
		}

		public void stopHandleMessages() {
			canRun = false;
//			interrupt();
		}
	}

	private void manageLoginRequest(DatagramPacket packet) {
    	String request = new String(packet.getData(), 0, packet.getLength());
    	
    	LoginMessage lm = null;
    	UserInfo userOfMessage = null;
    	String password = null;
    	SimpleIMUser s;
    	String answer = null;
    	
    	InetAddress address;
		int port;
    	
    	try {
			lm = LoginMessage.fromXML(request);
		} catch (XmlMessageReprException e1) {
			if (DEBUG) 
        		System.out.println("Login message cannot be serialized :(");
		}
    	
    	userOfMessage = lm.getUser();
    	password = lm.getPassword();
    	
    	if (DEBUG) 
    		System.out.println("Login message recieved from \"" + userOfMessage.getUsername() +"\"");
    	
    	s = getTheUserFromRegistered(userOfMessage);
    	
    	if (s == null || (s != null && !s.samePassword(password))) {
    		if (DEBUG) 
        		System.out.println("Sending REFUSE login to \"" + userOfMessage.getUsername() +"\"");
        	
    		try {
				answer = new LoginMessageAnswer(userOfMessage).toXML();
			} catch (ParserConfigurationException | TransformerException e) {
				if (DEBUG) 
            		System.out.println("ops... should not be here :(");
			}
    		
    		address = packet.getAddress();
    		port = packet.getPort();

    		outgoingRequests.add(new DatagramPacket(answer.getBytes(), answer.getBytes().length, address, port));
    	} else {
    		if (DEBUG) 
        		System.out.println("Sending ACCEPT login to \"" + userOfMessage.getUsername() +"\"");
    		
    		address = packet.getAddress();
    		port = packet.getPort();
    		
    		s.getUser().setAltitude(userOfMessage.getAltitude());
    		s.getUser().setLatitude(userOfMessage.getLatitude());
    		s.getUser().setLongitude(userOfMessage.getLongitude());
    		s.getUser().setIP(address.toString());
    		s.getUser().setPort(String.valueOf(port));
    		
    		
    		try {
				answer = new LoginMessageAnswer(s.getUser(), String.valueOf(s.getUser().hashCode())).toXML();
			} catch (ParserConfigurationException | TransformerException e) { 
				if (DEBUG) 
            		System.out.println("ops... making a loginAnswer message");
			}
    		
    		outgoingRequests.add(new DatagramPacket(answer.getBytes(), answer.getBytes().length, address, port));
				
    		ListMessage listMessage = new ListMessage();
    		fillListMessage(listMessage, s);
    		
    		try {
				answer = listMessage.toXML();
			} catch (ParserConfigurationException | TransformerException e) { }
    		
    		outgoingRequests.add(new DatagramPacket(answer.getBytes(),  answer.getBytes().length, address, port));
    		
        	s.getUser().setOnline();
    	}
    }
    
	private void manageRegisterRequest(DatagramPacket dp) {
		String request = new String(dp.getData(), 0, dp.getLength());
		RegisterMessage rm = null;
		String answer = null;
    	
    	try {
			rm = RegisterMessage.fromXML(request);
		} catch (XmlMessageReprException e) {
			if (DEBUG) 
        		System.out.println("Register message cannot be serialized :(");   					
		}
    	
    	if (DEBUG) 
    		System.out.println("Register message recieved from \"" + rm.getUser().getUsername() +"\"");
    	
    	
    	if (registeredUsers.contains(new SimpleIMUser(rm.getUser(), "DUMMY"))) {
    		if (DEBUG) 
        		System.out.println("Sending REFUSE register to \"" + rm.getUser().getUsername() +"\"");
        	
    		try {
				answer = new RegisterMessageAnswer(RegisterMessageAnswer.REFUSED).toXML();
			} catch (ParserConfigurationException | TransformerException e) {
				if (DEBUG) 
            		System.out.println("ops... should not be here :("); 
			}               		
    	} else {
    		if (DEBUG) 
        		System.out.println("Sending ACCEPT register to \"" + rm.getUser().getUsername() +"\"");
        	
    		rm.getUser().setOffline();
    		registeredUsers.add(new SimpleIMUser(rm.getUser(), rm.getPassword()));
    		
    		try {
				answer = new RegisterMessageAnswer(RegisterMessageAnswer.ACCEPTED).toXML();
			} catch (ParserConfigurationException | TransformerException e) {
				if (DEBUG) 
            		System.out.println("ops... should not be here :("); 
			}
    	}
    	
		outgoingRequests.add(new DatagramPacket(answer.getBytes(), answer.getBytes().length, dp.getAddress(), dp.getPort()));
	}

    @Override
    protected void finalize() throws Throwable {
    	handleRequests.stopHandleMessages();
    	handleOutgoingPackets.stopHandleMessages();
    	handleIncomingPackets.stopHandleMessages();
    	
    	inSocket.close();
    	outSocket.close();
        super.finalize();
    }

}

