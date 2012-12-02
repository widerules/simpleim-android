package com.tolmms.simpleim.services;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Vector;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Pair;

import com.tolmms.simpleim.communication.Communication;
import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.interfaces.IAppManagerForComm;
import com.tolmms.simpleim.interfaces.ICommunication;

public class IMService extends Service implements IAppManager, IAppManagerForComm {
	private final IBinder iMBinder = new IMBinder();
	
	private NotificationManager notifManager = null;
	
	private boolean isLogged = false;
	
	
	private ICommunication communication = null;
	
	private Vector<UserInfo> userList = null;
	private HashMap<UserInfo, Vector<Pair<UserInfo, String>>> messages = null;

	private ConnectivityManager conManager;
	
//	private UserInfo myInfo;
	
	
	
	public class IMBinder extends Binder {
		public IAppManager getService() {
			return IMService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent i) {
		return iMBinder;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	
	@Override
	public void onCreate() {
		notifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		
		messages = new HashMap<UserInfo, Vector<Pair<UserInfo,String>>>();
		
		communication = new Communication(this);
		
		
				
	}
	
	
	@Override
	public void loginUser(String username, String password) throws UsernameOrPasswordException, UnknownHostException, CommunicationException {
		if (isLogged)
			return;
		
		isLogged = communication.login(username, password);
		
		isLogged = true;
		return;
	}

	@Override
	public void registerUser(String username, String password, String email) throws CommunicationException, UsernameAlreadyExistsException, UnknownHostException {
		communication.register(username, password, email);
	}

	@Override
	public void exit() {		
		isLogged = false;
		communication.logout();
	}

	@Override
	public boolean isUserLoggedIn() {
		return isLogged;
	}

	@Override
	public boolean isNetworkConnected() {
		NetworkInfo netInfo = conManager.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnected())
			return false;
		return true;
	}

	public void setUserList(Vector<UserInfo> userList) {
		this.userList = userList;
		
	}

	
	@Override
	public void recievedMessage(UserInfo source, String message) {
		//TODO must show up the message and add to a list?
		Vector<Pair<UserInfo, String>> messagesSource = messages.get(source);
		
		if (messagesSource == null)
			messagesSource = new Vector<Pair<UserInfo,String>>();
		
		messagesSource.add(new Pair<UserInfo, String>(source, message));
		
		messages.put(source, messagesSource);
		
		//notification of recieved message
		
	}

	@Override
	public void userLoggedOut(UserInfo source) {
		// TODO ancora da fare
		
		// maybe a notification user loggod out
		
	}

	@Override
	public void userLoggedIn(UserInfo source) {
		// TODO ancora da fare
		
		// maybe a notification user logged in
	}

}
