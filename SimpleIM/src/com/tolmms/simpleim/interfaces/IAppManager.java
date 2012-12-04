package com.tolmms.simpleim.interfaces;

import java.net.UnknownHostException;
import java.util.Vector;

import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;

public interface IAppManager {
	public void loginUser(String username, String password) throws UsernameOrPasswordException, UnknownHostException, CommunicationException;
	public void registerUser(String username, String password, String email) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException;
	
	public void exit();
	
	public boolean isUserLoggedIn();
	public boolean isNetworkConnected();
	
	
	public static String INTENT_ACTION_USER_LIST_RECIEVED = "com.tolmms.simpleim.USER_LIST_RECIEVED";
	
	//public int addNewFriendRequest(String username);
	
	// must also be able to send answers to the requests.
	
	// methods for receiving and sending messages
	
	
	

}
