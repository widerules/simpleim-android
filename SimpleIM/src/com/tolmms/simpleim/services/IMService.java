package com.tolmms.simpleim.services;

import java.net.UnknownHostException;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.tolmms.simpleim.ChatActivity;
import com.tolmms.simpleim.MainActivity;
import com.tolmms.simpleim.R;
import com.tolmms.simpleim.communication.CannotSendBecauseOfWrongUserInfo;
import com.tolmms.simpleim.communication.Communication;
import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.communication.UnableToStartSockets;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.exceptions.NotEnoughResourcesException;
import com.tolmms.simpleim.exceptions.UserIsAlreadyLoggedInException;
import com.tolmms.simpleim.exceptions.UserNotLoggedInException;
import com.tolmms.simpleim.exceptions.UserToChatWithIsNotRecognizedException;
import com.tolmms.simpleim.exceptions.UsernameAlreadyExistsException;
import com.tolmms.simpleim.exceptions.UsernameOrPasswordException;
import com.tolmms.simpleim.interfaces.IAppManager;
import com.tolmms.simpleim.interfaces.IAppManagerForComm;
import com.tolmms.simpleim.interfaces.ICommunication;
import com.tolmms.simpleim.storage.TemporaryStorage;

public class IMService extends Service implements IAppManager, IAppManagerForComm {
	private final IBinder iMBinder = new IMBinder();
	
	private NotificationManager notifManager = null;
	private ConnectivityManager conManager = null;
	
	private LocationManager lm = null;
	private LocationListener myLoclistener = null;
	
	private ICommunication communication = null;
	
	private Intent messageReceivedSent = null;
	private Intent userStateChange = null;	
	
	private String currentUserChat = "";
//	private boolean viewingMap = false;
	
	private boolean isLogged = false;
	

	/* 
	 * *************************************************************************
	 * Stuff for service
	 * *************************************************************************
	 */
	public class IMBinder extends Binder {
		public IAppManager getService() {
			return IMService.this;
		}
	}

	@Override
	public IBinder onBind(Intent i) {
		return iMBinder;
	}

	/*
	 * *************************************************************************
	 * *************************************************************************
	 */
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
		
	@Override
	public void onCreate() {
		notifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		
		communication = new Communication(this);
		messageReceivedSent = new Intent(INTENT_ACTION_MESSAGES_RECEIVED_SENT);
		userStateChange = new Intent(INTENT_ACTION_USER_STATE_CHANGED);
		
		TemporaryStorage.user_list.clear();
		TemporaryStorage.messages.clear();
		
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		myLoclistener =  new MyLocListener();
		
		
		
		
		if (MainActivity.DEBUG) {
			Vector<UserInfo> user_list = TemporaryStorage.user_list;
//			Vector<UserInfo> user_list = new Vector<UserInfo>();
			
			
			user_list.add(new UserInfo("prova1", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS));
			user_list.add(new UserInfo("arova1", "10.2.1.1", 2000, UserInfo.ONLINE_STATUS));
			user_list.add(new UserInfo("prova2", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS));
			user_list.add(new UserInfo("aarova1", "10.2.1.1", 2000, UserInfo.ONLINE_STATUS));
			user_list.add(new UserInfo("aaaaarova1", "10.2.1.1", 2000, UserInfo.ONLINE_STATUS));
			user_list.add(new UserInfo("aaaaaaaaarova1", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS));
		
			
			TemporaryStorage.reorderUserList();
			
			LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
			
			
			LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
			
			
			// da mettere solo quando si fa il login
			UserInfo myInfo = new UserInfo("artur", "10101", 10);
			TemporaryStorage.myInfo.setOnline();
			TemporaryStorage.myInfo.set(myInfo.getUsername(), myInfo.getIp(), myInfo.getPort());
//			TemporaryStorage.user_list = user_list;
			
//			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLoclistener);
		}
		
		
				
	}
	
	/* 
	 * *************************************************************************
	 * Private methods and classes
	 * *************************************************************************
	 */
	
	/*
	 * class that defines what actions to do when my location is changed
	 */
	private class MyLocListener implements LocationListener {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}
		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}
		@Override
		public void onLocationChanged(Location location) {
			TemporaryStorage.myInfo.setLatitude(location.getLatitude());
			TemporaryStorage.myInfo.setLongitude(location.getLongitude());
			TemporaryStorage.myInfo.setAltitude(location.getAltitude());
		}
	};
	
	
	/*
	 * shows tray notification when needed
	 */
	private void showNotificationIfNeeded(String username_to_chat) {

		Intent notifyOpensChatIntent = null;
		TaskStackBuilder stackBuilder = null;
		PendingIntent contentIntent = null;
		NotificationCompat.Builder builder = null;
		
		notifyOpensChatIntent = new Intent(this, ChatActivity.class).
								putExtra(ChatActivity.USERNAME_TO_CHAT_WITH_EXTRA, username_to_chat).
								setAction(ChatActivity.MESSAGE_TO_A_USER);
		stackBuilder = TaskStackBuilder.
							create(this).
							addParentStack(ChatActivity.class).
							addNextIntent(notifyOpensChatIntent);
		contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder = new NotificationCompat.Builder(this).setContentIntent(contentIntent);
		
		
		Notification n = builder.
				setContentTitle(getString(R.string.it_notification_new_message_title)).
				setContentText(getString(R.string.it_notification_new_message_text) + username_to_chat).
				setSmallIcon(R.drawable.ic_stat_new_message).
				setAutoCancel(true).
				build();
		
		//TODO forse da rimuovere il coso di debug
		if (MainActivity.DEBUG)
			notifManager.notify(username_to_chat.hashCode(), n);
		else {
			if (!currentUserChat.equals(username_to_chat))
				notifManager.notify(username_to_chat.hashCode(), n);
		}
	}
	
	/*
	 * send an intent do ChatActivity to tell that there is a new message for current
	 * user - the ChatActivity should update the view of messages!
	 */
	private void notifyNewMessageToChatActivity(String username_to_chat) {
		if (currentUserChat.equals(username_to_chat)) {
			messageReceivedSent.putExtra(INTENT_ACTION_MESSAGES_RECEIVED_SENT_USERNAME_EXTRA, username_to_chat);
			LocalBroadcastManager.getInstance(this).sendBroadcast(messageReceivedSent);
		}
	}
	

	/* 
	 * *************************************************************************
	 * IAppManager
	 * *************************************************************************
	 */
	@Override
	public void loginUser(String username, String password) 
			throws UsernameOrPasswordException, 
					UnknownHostException, 
					CommunicationException, 
					UserIsAlreadyLoggedInException, 
					NotEnoughResourcesException {
		
		if (isLogged)
			throw new UserIsAlreadyLoggedInException();
		
		UserInfo myInfo = null;
		
		try {
			myInfo = communication.login(username, password);
		} catch (UnableToStartSockets e) {
			throw new NotEnoughResourcesException(e.getMessage());
		}	
		
		//TODO assicurarmi che la risposta di myInfo sia effettivamente mia
		// 		cioè che myInfo.username == username
		TemporaryStorage.myInfo.setOnline();
		TemporaryStorage.myInfo.set(username, myInfo.getIp(), myInfo.getPort());
		
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLoclistener);
		isLogged = true;
		
		
		communication.announceIAmOnline(TemporaryStorage.myInfo, TemporaryStorage.user_list);
		
		return;
	}
	
	@Override
	public void exit() {
		communication.logout();
		
		isLogged = false;
		
		lm.removeUpdates(myLoclistener);
		TemporaryStorage.myInfo.clearInfos();
		TemporaryStorage.user_list.clear();
		TemporaryStorage.messages.clear();
	}
	
	@Override
	public void registerUser(String username, String password) 
			throws CommunicationException, 
				UsernameAlreadyExistsException, 
				UnknownHostException, 
				UserIsAlreadyLoggedInException {
		
		if (isLogged)
			throw new UserIsAlreadyLoggedInException();
		
		communication.register(username, password);
	}
	
	@Override
	public void sendMessage(String username_to_chat, String the_message) 
			throws UserNotLoggedInException, 
				UserToChatWithIsNotRecognizedException, 
				CannotSendBecauseOfWrongUserInfo {
		UserInfo user_to_chat;
		
		if (!isLogged)
			throw new UserNotLoggedInException();
		
		if ((user_to_chat = TemporaryStorage.getUserInfoByUsername(username_to_chat)) == null)
			throw new UserToChatWithIsNotRecognizedException();
		
		
		communication.sendMessage(TemporaryStorage.myInfo, user_to_chat, the_message);

		
		// add the message to storage and notify chat activity if needed
		TemporaryStorage.addMessage(user_to_chat, getString(R.string.it_chat_self_name) + ": " + the_message);
		notifyNewMessageToChatActivity(username_to_chat);
		
		if (MainActivity.DEBUG)
			showNotificationIfNeeded(username_to_chat);
		
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
	
	
	@Override
	public void unsetCurrentUserChat() {
		currentUserChat = "";		
	}

	@Override
	public void setCurrentUserChat(String username_to_chat) {
		currentUserChat = username_to_chat;
		
	}

	@Override
	public void viewingMap(boolean b) {
		//TODO serve???
//		viewingMap = b;		
	}

	@Override
	public void sendMessageToAll(String msg) {
		// TODO iniare un messaggio a tutti
		
	}
	
	/*
	 * *************************************************************************
	 * *************************************************************************
	 */
	
	
	/* 
	 * *************************************************************************
	 * IAppManagerForComm
	 * *************************************************************************
	 */
	public void setUserList(Vector<UserInfo> userList) {
		for (UserInfo userInfo : userList) {
			if (TemporaryStorage.user_list.contains(userInfo))
				continue; //TODO AGGIORNARE I DATI
			TemporaryStorage.user_list.add(userInfo);
		}
		
	}

	@Override
	public void recievedMessage(UserInfo source, String message) {
		String username_to_chat;
		if (source == null)
			return;
		
		if (!TemporaryStorage.user_list.contains(source))
			return;
		
		username_to_chat = source.getUsername();
		
		if (TemporaryStorage.getUserInfoByUsername(username_to_chat).isOnline())
			return;
		
		TemporaryStorage.addMessage(source, username_to_chat + ": " + message);		
		notifyNewMessageToChatActivity(username_to_chat);
		
		
		
		showNotificationIfNeeded(username_to_chat);
		//notification of recieved message
		
	}

	@Override
	public void userLoggedOut(UserInfo source) {
		if (!TemporaryStorage.user_list.contains(source))
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
		if (!TemporaryStorage.user_list.contains(source)) {
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
	/*
	 * *************************************************************************
	 * *************************************************************************
	 */
	
	
}
