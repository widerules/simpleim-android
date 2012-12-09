package com.tolmms.simpleim.interfaces;

import java.net.UnknownHostException;

import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;

public interface ICommunication {

	public UserInfo login(String username, String password) throws UsernameOrPasswordException, CommunicationException, UnknownHostException;
	public boolean logout();
	public void register(String username, String password) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException;
	public void sendMessage(UserInfo myInfo, UserInfo user_to_chat, String the_message);

}
