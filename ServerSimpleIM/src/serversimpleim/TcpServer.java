package serversimpleim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

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

public class TcpServer extends BaseServer {
	public static final boolean DEBUG = true;
	// listener
    ServerSocket socket;
// accepted incoming connection
    Socket incoming;
    BufferedReader in;
    PrintWriter out;
    
    
    Vector<SimpleIMUser> registeredUsers;


    public TcpServer() throws IOException {
    	this(4445);
    }


    public TcpServer(int port) throws IOException {
        super();
        socket = new ServerSocket(port);
        registeredUsers = new Vector<>();
        
        if (DEBUG)
        	registerDebugUsers();
    }


    private void registerDebugUsers() {
		registeredUsers.add(new SimpleIMUser(
				new UserInfo("prova1", "10.2.1.1", "2000", UserInfo.OFFLINE_STATUS), 
				"prova1"));
		registeredUsers.add(new SimpleIMUser(
				new UserInfo("prova2", "10.2.1.2", "2000", UserInfo.OFFLINE_STATUS), 
				"prova2"));
		registeredUsers.add(new SimpleIMUser(
				new UserInfo("prova3", "10.2.1.3", "2000", UserInfo.OFFLINE_STATUS), 
				"prova3"));
		

	}


	public void run() {
        String request;
    	  
        while (true) {
            request = null;
           
            try {
				incoming = socket.accept();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
            
            try {
				in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(incoming.getOutputStream())),true);
				request = in.readLine();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
            
            
            
            String message_type = Procedures.getMessageType(request);
            
            if (message_type == null) {
            	
            	try {
					in.close();
					out.close();
	                incoming.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
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
					
					try {
						in.close();
						out.close();
		                incoming.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}                	
					
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
						answer = new LoginMessageAnswer().toXML();
					} catch (ParserConfigurationException | TransformerException e) { 
						//TODO cannot be here
					}
            		
            		out.write(answer);
            	} else {
            		if (DEBUG) 
                		System.out.println("Sending ACCEPT login to \"" + userOfMessage.getUsername() +"\"");
                	
            		try {
						answer = new LoginMessageAnswer("345675432345").toXML();
					} catch (ParserConfigurationException | TransformerException e) { 
						//TODO cannot be here
					}
            		
            		out.write(answer);
            		
            		
            		ListMessage listMessage = new ListMessage();
            		fillListMessage(listMessage);
            		
            		try {
						answer = listMessage.toXML();
					} catch (ParserConfigurationException | TransformerException e) { }
            		
            		out.write(answer);
            	}
            	//must close connections
            } else if (Procedures.isRegisterMessage(message_type)) {
            	RegisterMessage rm = null;
            	
            	try {
					rm = RegisterMessage.fromXML(request);
				} catch (XmlMessageReprException e) {
					if (DEBUG) 
                		System.out.println("Register message cannot be serialized :(");
					//close all
					
					try {
						in.close();
						out.close();
		                incoming.close();
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
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
						// TODO cannot arrive here
					}               		
            	} else {
            		if (DEBUG) 
                		System.out.println("Sending ACCEPT register to \"" + rm.getUser().getUsername() +"\"");
                	
            		try {
						answer = new RegisterMessageAnswer(RegisterMessageAnswer.ACCEPTED).toXML();
					} catch (ParserConfigurationException | TransformerException e) {
						// TODO cannot arrive here
					}
            	}
            	
            	out.println(answer);                	
            }
            
            try {
				in.close();
				out.close();
                incoming.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }


    private void fillListMessage(ListMessage listMessage) {
    	for (SimpleIMUser simu : registeredUsers) {
    		listMessage.addUser(simu.getUser());
		}
	}


	private SimpleIMUser getTheUserFromRegistered(UserInfo userOfMessage) {
    	int index = registeredUsers.indexOf(new SimpleIMUser(userOfMessage, "DUMMY"));
    	
    	if (index == -1)
    		return null;
    	
		return registeredUsers.get(index);
	}


	@Override
    protected void finalize() throws Throwable {
    	socket.close();
        super.finalize();
    }

}
