package com.tolmms.simpleim.interfaces;

import java.util.Vector;

import com.tolmms.simpleim.datatypes.MessageInfo;
import com.tolmms.simpleim.datatypes.UserInfo;

public interface IAppManagerForComm {
	public void setUserList(Vector<UserInfo> userList);
	
	public void recievedMessage(MessageInfo mi);
	public void receivedMessageAnswer(UserInfo user, int messageHashAck);
	
	public void userLoggedOut(UserInfo source);
	public void userLoggedIn(UserInfo source);
	
	public void receivedUserInfoRequest(UserInfo source);
	public void receivedUserInfoAnswer(UserInfo source);
	
	
	public boolean isForCurrentUser(UserInfo destination);
	public boolean isUserLoggedIn();
}
