package com.tolmms.simpleim.services;

import java.net.UnknownHostException;
import java.util.Vector;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.tolmms.simpleim.communication.Communication;
import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.interfaces.IAppManagerForComm;
import com.tolmms.simpleim.interfaces.ICommunication;
import com.tolmms.simpleim.storage.TemporaryStorage;

public class IMService extends Service implements IAppManager, IAppManagerForComm {
	private final IBinder iMBinder = new IMBinder();
	
	private NotificationManager notifManager = null;
	
	private boolean isLogged = false;
	
	
	private ICommunication communication = null;

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
		
		communication = new Communication(this);
		
		TemporaryStorage.user_list.clear();
		TemporaryStorage.messages.clear();
		
		if (true) {
			Vector<UserInfo> user_list = TemporaryStorage.user_list;
//			Vector<UserInfo> user_list = new Vector<UserInfo>();
			
			
			user_list.add(new UserInfo("prova1", "10.2.1.1", "2000", UserInfo.OFFLINE_STATUS));
			user_list.add(new UserInfo("arova1", "10.2.1.1", "2000", UserInfo.ONLINE_STATUS));
			user_list.add(new UserInfo("prova2", "10.2.1.1", "2000", UserInfo.OFFLINE_STATUS));
			user_list.add(new UserInfo("aarova1", "10.2.1.1", "2000", UserInfo.ONLINE_STATUS));
			user_list.add(new UserInfo("aaaaarova1", "10.2.1.1", "2000", UserInfo.ONLINE_STATUS));
			user_list.add(new UserInfo("aaaaaaaaarova1", "10.2.1.1", "2000", UserInfo.OFFLINE_STATUS));
		
			
			TemporaryStorage.reorderUserList();
			
			LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
			
			
			LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
//			TemporaryStorage.user_list = user_list;
		}
		
		
				
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
		for (UserInfo userInfo : userList) {
			TemporaryStorage.user_list.add(userInfo);
		}
		
	}

	
	@Override
	public void recievedMessage(UserInfo source, String message) {
		//TODO must show up the message and add to a list?
//		Vector<Pair<UserInfo, String>> messagesSource = messages.get(source);
//		
//		if (messagesSource == null)
//			messagesSource = new Vector<Pair<UserInfo,String>>();
//		
//		messagesSource.add(new Pair<UserInfo, String>(source, message));
//		
//		messages.put(source, messagesSource);
		
		//notification of recieved message
		
	}
	
	
	Intent userStateChange = new Intent(INTENT_ACTION_USER_STATE_CHANGED);

	@Override
	public void userLoggedOut(UserInfo source) {
		int index = TemporaryStorage.user_list.indexOf(source);		
		
		if (index == -1)
			return;
		// non c'è l'utente che fa il logout;
		
		TemporaryStorage.user_list.get(TemporaryStorage.user_list.indexOf(source)).setOffline();
		
		
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA, source.getUsername());
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA, UserInfo.OFFLINE_STATUS);
		LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
		
		//send messages
	}

	@Override
	public void userLoggedIn(UserInfo source) {
		int index = TemporaryStorage.user_list.indexOf(source);		
		
		if (index == -1) {
			//TODO può essere il caso che l'utente si è da poco registrato
			TemporaryStorage.user_list.add(source);
		} else {
			TemporaryStorage.user_list.get(TemporaryStorage.user_list.indexOf(source)).setOnline();
		}
		
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA, source.getUsername());
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA, UserInfo.ONLINE_STATUS);
		LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
		
		// maybe a notification user logged in
	}

	Intent messageReceivedSent = new Intent(INTENT_ACTION_MESSAGES_RECEIVED_SENT);
	@Override
	public void sendMessage(String username_to_chat, String the_message) {
		// TODO Auto-generated method stub
		
		messageReceivedSent.putExtra(INTENT_ACTION_MESSAGES_RECEIVED_SENT_USERNAME_EXTRA, "aarova1");
		LocalBroadcastManager.getInstance(this).sendBroadcast(messageReceivedSent);
		
	}
	
	
}
