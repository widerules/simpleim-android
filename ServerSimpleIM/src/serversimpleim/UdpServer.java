package serversimpleim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import serversimpleim.datatypes.SimpleIMUser;

import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.LoginMessage;
import com.tolmms.simpleim.datatypes.LoginMessageAnswer;
import com.tolmms.simpleim.datatypes.Procedures;
import com.tolmms.simpleim.datatypes.RegisterMessage;
import com.tolmms.simpleim.datatypes.RegisterMessageAnswer;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.exceptions.XmlMessageReprException;

public class UdpServer extends BaseServer {
	    DatagramSocket socket = null;
	    
	    public static final int UDP_BUFFER = 10 * 1024; // 10Kb


	    public UdpServer() throws IOException {
	    	this(4445);
	    }


	    public UdpServer(int port) throws IOException {
	        super();
	        socket = new DatagramSocket(port);
	    }


	    public void run() {
	        byte[] buf;
	        String request;
	        DatagramPacket packet;
	    	  
	        while (true) {

	            buf = new byte[UDP_BUFFER];
	            packet = new DatagramPacket(buf, buf.length);
            
                try {
					socket.receive(packet);
				} catch (IOException e2) {
					if (DEBUG) 
                		System.out.println("ops... receiving a packet from socket");	 
					continue;
				}
                
                request = new String(buf).trim();
                
                
                String message_type = null;
                try {
                message_type = Procedures.getMessageType(request);
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
                	LoginMessage lm = null;
                	UserInfo userOfMessage = null;
                	String password = null;
                	
                	
                	try {
    					lm = LoginMessage.fromXML(request);
    				} catch (XmlMessageReprException e1) {
    					if (DEBUG) 
                    		System.out.println("Login message cannot be serialized :(");
    					continue;
    				}
                	
                	userOfMessage = lm.getUser();
                	password = lm.getPassword();
                	
                	if (DEBUG) 
                		System.out.println("Login message recieved from \"" + userOfMessage.getUsername() +"\"");
                	
                	SimpleIMUser s = getTheUserFromRegistered(userOfMessage);
                	
                	String answer = null;
                	
                	if (s == null || (s != null && !s.samePassword(password))) {
                		if (DEBUG) 
                    		System.out.println("Sending REFUSE login to \"" + userOfMessage.getUsername() +"\"");
                    	
                		try {
    						answer = new LoginMessageAnswer(userOfMessage).toXML();
    					} catch (ParserConfigurationException | TransformerException e) { 
    						//TODO cannot be here
    					}
                		
                		buf = answer.getBytes();
                		
                		InetAddress address = packet.getAddress();
                		int port = packet.getPort();
                		
//                		System.out.println(answer);
                		
                		packet = new DatagramPacket(answer.getBytes(), answer.getBytes().length, address, port);
                		
                		
                		
                		try {
							socket.send(packet);
						} catch (IOException e) {
							if (DEBUG) 
		                		System.out.println("sending the refuse login");	 
							continue;
						}
                	} else {
                		if (DEBUG) 
                    		System.out.println("Sending ACCEPT login to \"" + userOfMessage.getUsername() +"\"");
                		
                		InetAddress address = packet.getAddress();
                		int port = packet.getPort();
                		
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
    						continue;
    					}
                		
                		try {
                			socket.send(new DatagramPacket(answer.getBytes(),  answer.getBytes().length, address, port));
                		} catch (IOException e) {
							if (DEBUG) 
		                		System.out.println("sending the accept login");	 
							continue;
						}
                		
                		
                		ListMessage listMessage = new ListMessage();
                		fillListMessage(listMessage, s);
                		
                		try {
    						answer = listMessage.toXML();
    					} catch (ParserConfigurationException | TransformerException e) { }
                		
                		buf = answer.getBytes();
                		
                		try {
                			socket.send(new DatagramPacket(buf,  buf.length, address, port));
                		} catch (IOException e) {
							if (DEBUG) 
		                		System.out.println("sending the userlist message");	 
							continue;
						}
                		
                    	s.getUser().setOnline();
                	}                	
                } else if (Procedures.isRegisterMessage(message_type)) {
                	RegisterMessage rm = null;
                	
                	try {
    					rm = RegisterMessage.fromXML(request);
    				} catch (XmlMessageReprException e) {
    					if (DEBUG) 
                    		System.out.println("Register message cannot be serialized :(");   					
    				}
                	
                	
                	if (DEBUG) 
                		System.out.println("Register message recieved from \"" + rm.getUser().getUsername() +"\"");
                	
                	String answer = null;
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
                	
                	buf = answer.getBytes();
            		InetAddress address = packet.getAddress();
            		int port = packet.getPort();
            		
            		packet = new DatagramPacket(buf,  buf.length, address, port);
            		try {
						socket.send(packet);
					} catch (IOException e) {
						if (DEBUG) 
	                		System.out.println("sending the register answer message");	 
						continue;
					}         	
                }
	        }
	    }


	    @Override
	    protected void finalize() throws Throwable {
	    	socket.close();
	        super.finalize();
	    }

	}

