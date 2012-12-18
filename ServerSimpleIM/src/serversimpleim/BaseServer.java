package serversimpleim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import serversimpleim.datatypes.SimpleIMUser;

import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;

public class BaseServer {
	public static final boolean DEBUG = true;
	
	UserInfo serverUserInfo;
	
    Vector<SimpleIMUser> registeredUsers;
    
    public static String FILE_WITH_USERS="users.txt";
    
    
    public BaseServer() {
        registeredUsers = new Vector<>();
//        if (DEBUG)
//        	registerDebugUsers();
        
        readUsersFromFile();
    }
    
    private void readUsersFromFile() {
    	File f = new File(FILE_WITH_USERS);
    	if (!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e1) {
				System.out.println("creating file");
			}
    	BufferedReader inputStream = null;
    	try {
			inputStream = new BufferedReader(new FileReader(f));
		
	    	String username;
	    	String password;
	    	while ((username = inputStream.readLine()) != null) {
	    		if ((password = inputStream.readLine()) != null)
					try {
						registeredUsers.add(new SimpleIMUser(new UserInfo(username, "10.10.1.1", UserInfo.MIN_ALLOWED_PORT), password));
					} catch (InvalidDataException e) {
						//oopss
					}
	    		
	    		username = null;
	    		password = null;
			}
    	} catch (IOException e) {
			System.out.println("file does not exists");
		}
    	
    	try {
			inputStream.close();
		} catch (IOException e) { }
	}
    
    protected void addUserToRegisteredUsers(SimpleIMUser simu) {
    	registeredUsers.add(simu);
    	
    	File f = new File(FILE_WITH_USERS);
    	PrintWriter out = null;
    	try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(f, true)));
			out.println();
			out.println(simu.getUser().getUsername());
			out.print(simu.getPassword());
    	} catch (IOException e) {
			System.out.println("file does not exists");
		}
    	out.close();
    }

	@SuppressWarnings("unused")
	private void registerDebugUsers() {
  		try {
			registeredUsers.add(new SimpleIMUser(
					new UserInfo("prova1", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS), 
					"prova1"));
			registeredUsers.add(new SimpleIMUser(
	  				new UserInfo("prova2", "10.2.1.2", 2000, UserInfo.OFFLINE_STATUS), 
	  				"prova2"));
	  		registeredUsers.add(new SimpleIMUser(
	  				new UserInfo("prova3", "10.2.1.3", 2000, UserInfo.OFFLINE_STATUS), 
	  				"prova3"));
		} catch (InvalidDataException e) {
			/* cannot be here */
		}
  		
  	}
    
    
    protected SimpleIMUser getTheUserFromRegistered(UserInfo userOfMessage) {
    	int index = registeredUsers.indexOf(new SimpleIMUser(userOfMessage, "DUMMY"));
    	
    	if (index == -1)
    		return null;
    	
		return registeredUsers.get(index);
	}
    
    protected void fillListMessage(ListMessage listMessage, SimpleIMUser forUser) {
    	for (SimpleIMUser simu : registeredUsers) {
    		if (!simu.equals(forUser))
    			listMessage.addUser(simu.getUser());
		}
	}
    
    
    
	
}


