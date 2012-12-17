package com.tolmms.simpleim.services;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tolmms.simpleim.ChatActivity;
import com.tolmms.simpleim.MainActivity;
import com.tolmms.simpleim.R;
import com.tolmms.simpleim.communication.CannotSendBecauseOfWrongUserInfo;
import com.tolmms.simpleim.communication.Communication;
import com.tolmms.simpleim.communication.CommunicationException;
import com.tolmms.simpleim.communication.UnableToStartSockets;
import com.tolmms.simpleim.datatypes.MessageInfo;
import com.tolmms.simpleim.datatypes.MessageRepresentation;
import com.tolmms.simpleim.datatypes.UserInfo;
import com.tolmms.simpleim.datatypes.UserInfoRepr;
import com.tolmms.simpleim.datatypes.exceptions.InvalidDataException;
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
	
	public static final int SECONDS_TO_CHECK_SENT_MESSAGES = 2; // 2 seconds

	public static final int SECONDS_TO_CHECK_USER_INFO = 15; // 30 seconds
	public static final int NUMBER_USER_INFO_REQUEST_RETRIES = 3;
	

	Vector<MessageRepresentation> sentMessagesWaitingForAnswer = new Vector<MessageRepresentation>();
	Vector<UserInfoRepr> userInfoReprs = new Vector<UserInfoRepr>();
	
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
		//TODO da fare il logout
		super.onDestroy();
	}
	
	
	Handler handler;
	Runnable msgAckManager;
	Runnable userInfoRequestManager;
		
	@Override
	public void onCreate() {
		notifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		
		communication = new Communication(this);
		messageReceivedSent = new Intent(INTENT_ACTION_MESSAGES_RECEIVED_SENT);
		userStateChange = new Intent(INTENT_ACTION_USER_STATE_CHANGED);
		
		TemporaryStorage.user_list.clear();
		TemporaryStorage.messages.clear();
		
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		myLoclistener =  new LocationListener() {
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
				
				if (MainActivity.DEBUG)
					Log.d("ImService MyLocListener", "changed the location data");
			}
		};
		
		
		
		
		handler = new Handler();
		

		msgAckManager = new Runnable() {
		    public void run() {
		    	//TODO è un poling...
	    		Iterator<MessageRepresentation> it = sentMessagesWaitingForAnswer.iterator();
	    		
	    		while (it.hasNext()) {
	    			MessageRepresentation mr = it.next();
	    			if (mr.ackRecieved) {
	    				it.remove();
	    				if (MainActivity.DEBUG)
	    					Log.d("IMService - msgAcqManager", "Message has received ack - not monitoring it anymore");
	    				continue;
	    			}
	    			
	    			if (mr.ackRecieved || mr.sentRetries > MessageRepresentation.MAX_RETRIES) {
	    				it.remove();
	    				if (MainActivity.DEBUG)
	    					Log.d("IMService - msgAcqManager", "Message has reached maximum retries - not monitoring it anymore");
	    				continue;
	    			}
    				Calendar c = Calendar.getInstance();
					Date now = c.getTime();
					
					c.setTime(mr.getMessageInfo().getSentTime());
					
					c.add(Calendar.SECOND, SECONDS_TO_CHECK_SENT_MESSAGES * mr.sentRetries);
					
					if (now.after(c.getTime())) {
						mr.sentRetries++;
						try {
							communication.sendMessage(mr.getMessageInfo());
						} catch (CannotSendBecauseOfWrongUserInfo e) { /* it is not likely to be here */ }
						it.remove();
						
	    				if (MainActivity.DEBUG)
	    					Log.d("IMService - msgAcqManager", "Message has not rceived ack - trying to sed it again");
					}
	    		}
	    		
				handler.postDelayed(this, SECONDS_TO_CHECK_SENT_MESSAGES * 1000);
		    }
		};
		
		userInfoRequestManager = new Runnable() {
			@Override
			public void run() {
				Calendar c = Calendar.getInstance();
				Date now = c.getTime();
				
				for (UserInfoRepr uir : userInfoReprs) {
					// now - last >= Seconds to check
					// now >= last + seconds to check
					
					if (!uir.u.isOnline())
						continue;
					
					c.setTime(uir.last_update);
					c.add(Calendar.SECOND, SECONDS_TO_CHECK_USER_INFO * NUMBER_USER_INFO_REQUEST_RETRIES);
					if (now.after(c.getTime())) {
						userLoggedOut(uir.u);
						
						if (MainActivity.DEBUG)
	    					Log.d("IMService - userInfoRequestManager", "set a user offline...");
						
						continue;
					}
					c.setTime(uir.last_update);
					c.add(Calendar.SECOND, SECONDS_TO_CHECK_USER_INFO);
					if (now.after(c.getTime())) {
						communication.sendUserInfoRequest(TemporaryStorage.myInfo, uir.u);

						if (MainActivity.DEBUG)
	    					Log.d("IMService - userInfoRequestManager", "sent userinforequest...");
					}
					
				}
				
				handler.postDelayed(this, SECONDS_TO_CHECK_USER_INFO * 1000);
			}
		};
		
		
//		
//		if (MainActivity.DEBUG) {
//			Vector<UserInfo> user_list = TemporaryStorage.user_list;
////			Vector<UserInfo> user_list = new Vector<UserInfo>();
//			
//			
//			try {
//				user_list.add(new UserInfo("prova1", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS));
//				user_list.add(new UserInfo("arova1", "10.2.1.1", 2000, UserInfo.ONLINE_STATUS));
//				user_list.add(new UserInfo("prova2", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS));
//				user_list.add(new UserInfo("aarova1", "10.2.1.1", 2000, UserInfo.ONLINE_STATUS));
//				user_list.add(new UserInfo("aaaaarova1", "10.2.1.1", 2000, UserInfo.ONLINE_STATUS));
//				user_list.add(new UserInfo("aaaaaaaaarova1", "10.2.1.1", 2000, UserInfo.OFFLINE_STATUS));
//			} catch (InvalidDataException e) { /* cannot be here */ }
//			
//			TemporaryStorage.reorderUserList();
//			
//			LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
//			
//			
//			LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
//			
//			
//			// da mettere solo quando si fa il login
//			UserInfo myInfo = null;
//			
//			try {
//				myInfo = new UserInfo("artur", "10101", 10);
//			} catch (InvalidDataException e) { /* cannot be here */ }
//			
//			TemporaryStorage.myInfo.setOnline();
//			TemporaryStorage.myInfo.set(myInfo.getUsername(), myInfo.getIp(), myInfo.getPort());
////			TemporaryStorage.user_list = user_list;
//			
////			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLoclistener);
//		}
		
		
				
	}
	
	
	
	/* 
	 * *************************************************************************
	 * Private methods and classes
	 * *************************************************************************
	 */
	
	
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
		

		if (!currentUserChat.equals(username_to_chat))
			notifManager.notify(username_to_chat.hashCode(), n);
		
		if (MainActivity.DEBUG)
			Log.d("show notification if needed", "current: " + currentUserChat + "; username_to_chat: " + username_to_chat);
	
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

		
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLoclistener, Looper.getMainLooper());
		
		isLogged = true;
		
		
		communication.announceIAmOnline(TemporaryStorage.myInfo, TemporaryStorage.user_list);
		
		msgAckManager.run();
		userInfoRequestManager.run();
		
		return;
	}
	
	@Override
	public void exit() 
			throws UserNotLoggedInException {
		
		if (!isLogged)
			throw new UserNotLoggedInException();
		
		//TODO
//		boolean success = false;
//		
//		success = 
		communication.logout(TemporaryStorage.myInfo, TemporaryStorage.user_list);
		
//		if (!success)
//			throw new CannotLogOutException();
		//even if I cannot announce all that i logout... i must exit from program!
		
		isLogged = false;
		lm.removeUpdates(myLoclistener);
		TemporaryStorage.myInfo.clearInfos();
		TemporaryStorage.user_list.clear();
		TemporaryStorage.messages.clear();
		
		handler.removeCallbacks(msgAckManager);
		handler.removeCallbacks(userInfoRequestManager);
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
				CannotSendBecauseOfWrongUserInfo, 
				InvalidDataException {
		UserInfo user_to_chat;
		
		if (!isLogged)
			throw new UserNotLoggedInException();
		
		if ((user_to_chat = TemporaryStorage.getUserInfoByUsername(username_to_chat)) == null)
			throw new UserToChatWithIsNotRecognizedException();
		
		if (MainActivity.DEBUG)
			Log.d("IMService - sendMessage to", user_to_chat.toString());
		
		MessageInfo mi = new MessageInfo(TemporaryStorage.myInfo, user_to_chat, the_message);
		
		communication.sendMessage(mi);
		
		MessageRepresentation mr = new MessageRepresentation(mi);

		
		sentMessagesWaitingForAnswer.add(mr);
		// add the message to storage and notify chat activity if needed
		TemporaryStorage.addMessage(user_to_chat, mr);
		notifyNewMessageToChatActivity(username_to_chat);
		
//		if (MainActivity.DEBUG)
//			showNotificationIfNeeded(username_to_chat);
		
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
			userInfoReprs.add(new UserInfoRepr(userInfo, Calendar.getInstance().getTime()));
		}
		
	}

	/* TODO 
	 * 
	 */
	@Override
	public void recievedMessage(MessageInfo mi) {
		String username_to_chat;
		UserInfo source = mi.getSource();
		
		if (!TemporaryStorage.user_list.contains(source)) {
			/*
			 * this is the case when a user has registered, logged 
			 * in and I did not received login message but he now writes to me.
			 */
			TemporaryStorage.user_list.add(source);
			userInfoReprs.add(new UserInfoRepr(source, Calendar.getInstance().getTime()));
		}
		
		username_to_chat = source.getUsername();
		
		MessageRepresentation mr = new MessageRepresentation(mi);
		
		receivedUserInfoAnswer(source);
		
		if (!TemporaryStorage.getMessagesByUser(source).contains(mr)) {
			TemporaryStorage.addMessage(source, mr);
			
			notifyNewMessageToChatActivity(username_to_chat);
			showNotificationIfNeeded(username_to_chat);
			//notification of recieved message
		}
		
		communication.sendMessageAck(TemporaryStorage.myInfo, source, mi.hashCode());
				
	}

	@Override
	public void userLoggedOut(UserInfo source) {
		if (!TemporaryStorage.user_list.contains(source))
			return;
		// non c'è l'utente che fa il logout;
		
		TemporaryStorage.user_list.get(TemporaryStorage.user_list.indexOf(source)).setOffline();
		
		notifyUserStateChanged(source, UserInfo.OFFLINE_STATUS);
		
		//send messages
	}

	@Override
	public void userLoggedIn(UserInfo source) {
		if (!TemporaryStorage.user_list.contains(source)) {
			/*
			 * this is the case when a user has registered after I logged in - so I don't know him.
			 */
			TemporaryStorage.user_list.add(source);
			userInfoReprs.add(new UserInfoRepr(source, Calendar.getInstance().getTime()));
		}
		receivedUserInfoAnswer(source);
		
	}
	
	private void notifyUserStateChanged(UserInfo u, String state) {
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA, u.getUsername());
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA, state);
		LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
	}
	
	@Override
	public void receivedMessageAnswer(UserInfo user, int messageHashAck) {
//		Vector<MessageRepresentation> ms = TemporaryStorage.getMessagesByUser(user);
//		
//		if (ms == null)
//			return;
//		
//		for(int i = TemporaryStorage.getMaxMessageCountHistoryPerUser() - 1; i >= 0; --i) {
//			if (ms.elementAt(i).getMessageInfo().hashCode() == messageHashAck) {
//				ms.elementAt(i).ackRecieved = true;
//				return;
//			}
//		}
		
		for (MessageRepresentation mr : sentMessagesWaitingForAnswer) {
			if (mr.getMessageInfo().hashCode() == messageHashAck) {
				mr.ackRecieved = true;
				notifyNewMessageToChatActivity(user.getUsername());
				return;
			}
		}
		
	}

	@Override
	public void receivedUserInfoRequest(UserInfo source) {
		receivedUserInfoAnswer(source);
		communication.sendUserInfoAnswer(TemporaryStorage.myInfo, source);
	}

	@Override
	public void receivedUserInfoAnswer(UserInfo source) {
		//aggiorno i dati di source e basta
		
		UserInfoRepr uir = null;
		try {
			uir = userInfoReprs.get(userInfoReprs.indexOf(new UserInfoRepr(source, null)));
		} catch (ArrayIndexOutOfBoundsException e) {
			//ok
		}
		
		if (uir == null)
			return; // non esiste... allora da mettere, si.. da mettere - basta che non sia il UserInfo.ServerName!

		
		
		uir.u.setAltitude(source.getAltitude());
		uir.u.setLongitude(source.getLongitude());
		uir.u.setLatitude(source.getLatitude());
		uir.u.setIP(source.getIp());
		uir.u.setPort(source.getPort());
		uir.u.setOnline();
		
		notifyUserStateChanged(uir.u, UserInfo.ONLINE_STATUS);
		
		uir.last_update = Calendar.getInstance().getTime();
		
	}
	
	
	
	
	/*
	 * *************************************************************************
	 * *************************************************************************
	 */	
	
	
	

}
