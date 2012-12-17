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
	
	public static final int SECONDS_TO_CHECK_SENT_MESSAGES = 2;
	public static final int NUMBER_MESSAGE_SENT_RETRIES = 3;
	
	public static final int SECONDS_TO_CHECK_USER_INFO = 15;
	public static final int NUMBER_USER_INFO_REQUEST_RETRIES = 3;
	
	
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
	
	public boolean sendMessageToAll(String msg);
	

	public boolean isUserLoggedIn();
	public boolean isNetworkConnected();
	
	/* stuff for chat */
	public void unsetCurrentUserChat();
	public void setCurrentUserChat(String username_to_chat);
	
	/* stuff for map */
	public void activateMap(int my_rate, int others_rate);
	public void deactivateMap();
	public boolean isMapActivated();
	public int getMyRefreshTime();
	public int getOthersRefreshTime();	
}
