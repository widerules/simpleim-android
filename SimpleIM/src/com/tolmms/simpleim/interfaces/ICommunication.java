package com.tolmms.simpleim.interfaces;

import java.net.UnknownHostException;

import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;

public interface ICommunication {

	public boolean login(String username, String password) throws UsernameOrPasswordException, CommunicationException, UnknownHostException;
	public boolean logout();
	public void register(String username, String password, String email) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException;

}
