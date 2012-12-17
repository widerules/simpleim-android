package com.tolmms.simpleim.interfaces;

import java.net.UnknownHostException;

import com.tolmms.simpleim.communication.CannotSendBecauseOfWrongUserInfo;
import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
import com.tolmms.simpleim.exceptions.NotEnoughResourcesException;
import com.tolmms.simpleim.exceptions.UserIsAlreadyLoggedInException;
import com.tolmms.simpleim.exceptions.UserNotLoggedInException;
import com.tolmms.simpleim.exceptions.UserToChatWithIsNotRecognizedException;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;

public interface IAppManager {
	public static String INTENT_ACTION_USER_STATE_CHANGED = "com.tolmms.simpleim.USER_STATE_CHANGED";
	public static String INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA = "com.tolmms.simpleim.USER_STATE_CHANGED.USERNAME";
	public static String INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA = "com.tolmms.simpleim.USER_STATE_CHANGED.STATE";
	public static String INTENT_ACTION_MESSAGES_RECEIVED_SENT = "com.tolmms.simpleim.MESSAGES_RECEIVED_SENT";
	public static String INTENT_ACTION_MESSAGES_RECEIVED_SENT_USERNAME_EXTRA = "com.tolmms.simpleim.MESSAGES_RECEIVED_SENT_USERNAME_EXTRA";
	public static String INTENT_ACTION_USER_POSITION_CHANGED = "com.tolmms.simpleim.USER_POSITION_CHANGED";
	public static String INTENT_ACTION_OTHER_POSITION_CHANGED = "com.tolmms.simpleim.OTHER_POSITION_CHANGED";
	
	public void loginUser(String username, String password) 
			throws UsernameOrPasswordException, 
					UnknownHostException, 
					CommunicationException, 
					UserIsAlreadyLoggedInException, 
					NotEnoughResourcesException;
	public void exit() 
			throws UserNotLoggedInException;
	public void registerUser(String username, String password) 
			throws CommunicationException,
				UsernameAlreadyExistsException, 
				UnknownHostException, 
				UserIsAlreadyLoggedInException;
	public void sendMessage(String username_to_chat, String the_message) 
			throws UserNotLoggedInException, 
					UserToChatWithIsNotRecognizedException, 
					CannotSendBecauseOfWrongUserInfo, 
					InvalidDataException;
	
	

	public boolean isUserLoggedIn();
	public boolean isNetworkConnected();
	
	public void unsetCurrentUserChat();
	public void setCurrentUserChat(String username_to_chat);
	
	public void sendMessageToAll(String msg);
	
	public void viewingMap(boolean b);	
}
