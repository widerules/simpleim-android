package com.tolmms.simpleim.interfaces;

import java.util.Vector;

import com.tolmms.simpleim.datatypes.UserInfo;

public interface IAppManagerForComm {
	
	public void setUserList(Vector<UserInfo> userList);
	public void recievedMessage(UserInfo source, String message);
	public void userLoggedOut(UserInfo source);
	public void userLoggedIn(UserInfo source);
	public boolean isUserLoggedIn();
}
