package serversimpleim;

import java.util.Vector;

import com.tolmms.simpleim.datatypes.ListMessage;
import com.tolmms.simpleim.datatypes.UserInfo;

import serversimpleim.datatypes.SimpleIMUser;

public class BaseServer {
	public static final boolean DEBUG = true;
	
	
    Vector<SimpleIMUser> registeredUsers;
    
    
    public BaseServer() {
        registeredUsers = new Vector<>();
        if (DEBUG)
        	registerDebugUsers();
    }
    
    private void registerDebugUsers() {
  		registeredUsers.add(new SimpleIMUser(
  				new UserInfo("prova1", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS), 
  				"prova1"));
  		registeredUsers.add(new SimpleIMUser(
  				new UserInfo("prova2", "10.2.1.2", 2000, UserInfo.OFFLINE_STATUS), 
  				"prova2"));
  		registeredUsers.add(new SimpleIMUser(
  				new UserInfo("prova3", "10.2.1.3", 2000, UserInfo.OFFLINE_STATUS), 
  				"prova3"));
  		

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


