package com.tolmms.simpleim.interfaces;

import java.net.UnknownHostException;
import java.util.List;

import com.tolmms.simpleim.communication.CannotSendBecauseOfWrongUserInfo;
import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.communication.UnableToStartSockets;
import com.tolmms.simpleim.datatypes.MessageInfo;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;

public interface ICommunication {
	public UserInfo login(String username, String password) 
			throws UsernameOrPasswordException, CommunicationException, 
					UnknownHostException, UnableToStartSockets;
	
	public void register(String username, String password) 
			throws CommunicationException, UsernameAlreadyExistsException, 
					UnknownHostException;
	
	public void sendMessage(MessageInfo mi)
			throws CannotSendBecauseOfWrongUserInfo;
	
	
	public boolean logout();
	
	public void announceIAmOnline(UserInfo me, List<UserInfo> userList);

}
