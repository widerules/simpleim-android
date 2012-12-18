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
	
	private boolean isLogged = false;
	
	/* stuff for map */
	boolean isMapActivated = false;
	int my_location_refresh_rate = 10;		//Seconds
	int others_location_refresh_rate = 15;	//Seconds
	

	Handler handler = null;
	
	MessageAckManager msgAckManager = null;
	Vector<MessageRepresentation> sentMessagesWaitingForAnswer = null;
	
	UserInfoRequestManager userInfoRequestManager = null;
	Vector<UserInfoRepr> userInfoReprs = null;
	
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
		try {
			exit();
		} catch (UserNotLoggedInException e) { }
		
		super.onDestroy();
	}
	
	@Override
	public void onCreate() {
		notifManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		
		communication = new Communication(this);
		messageReceivedSent = new Intent(INTENT_ACTION_MESSAGES_RECEIVED_SENT);
		userStateChange = new Intent(INTENT_ACTION_USER_STATE_CHANGED);
		
		synchronized (TemporaryStorage.user_list) {
			TemporaryStorage.user_list.clear();
		}
		
		TemporaryStorage.messages.clear();
		
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		myLoclistener =  new MyLocationListener();
		
		sentMessagesWaitingForAnswer = new Vector<MessageRepresentation>();
		userInfoReprs = new Vector<UserInfoRepr>();
		
		handler = new Handler();
		msgAckManager = new MessageAckManager();
		userInfoRequestManager = new UserInfoRequestManager();				
	}
	
	
	
	/* 
	 * *************************************************************************
	 * Private methods and classes
	 * *************************************************************************
	 */
	
	private class MessageAckManager implements Runnable {
		@Override
		public void run() {
			Iterator<MessageRepresentation> it = sentMessagesWaitingForAnswer.iterator();

			while (it.hasNext()) {
				MessageRepresentation mr = it.next();
				if (mr.ackRecieved) {
					it.remove();
					if (MainActivity.DEBUG)
						Log.d("IMService - msgAcqManager", "Message has received ack - not monitoring it anymore");
					continue;
				}

				if (mr.sentRetries > NUMBER_MESSAGE_SENT_RETRIES) {
					it.remove();
					if (MainActivity.DEBUG)
						Log.d("IMService - msgAcqManager", "Message has reached maximum retries - not monitoring it anymore");
					notifyNewMessageToChatActivity(mr.getMessageInfo().getDestination().getUsername());
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

					if (MainActivity.DEBUG)
						Log.d("IMService - msgAcqManager", "Message has not rceived ack - trying to sed it again");
				}
			}

			handler.postDelayed(this, SECONDS_TO_CHECK_SENT_MESSAGES * 1000);
		}
	}
	
	private class UserInfoRequestManager implements Runnable {
		@Override
		public void run() {
			Calendar c = Calendar.getInstance();
			Date now = c.getTime();
			
			int seconds_to_check = SECONDS_TO_CHECK_USER_INFO;
			if (isMapActivated)
				seconds_to_check = Math.min(others_location_refresh_rate, SECONDS_TO_CHECK_USER_INFO);
			synchronized (userInfoReprs) {
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
					c.add(Calendar.SECOND, seconds_to_check);
					if (now.after(c.getTime())) {
						communication.sendUserInfoRequest(TemporaryStorage.myInfo, uir.u);

						if (MainActivity.DEBUG)
	    					Log.d("IMService - userInfoRequestManager", "sent userinforequest...");
					}
					
				}
			}
	
			handler.postDelayed(this, seconds_to_check * 1000);
		}
	}
	
	private class MyLocationListener implements LocationListener {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {				
		}
		
		@Override
		public void onProviderEnabled(String provider) {				
		}
		
		@Override
		public void onProviderDisabled(String provider) {				
		}
		
		@Override
		public void onLocationChanged(Location location) {
			TemporaryStorage.myInfo.setLatitude(location.getLatitude());
			TemporaryStorage.myInfo.setLongitude(location.getLongitude());
			TemporaryStorage.myInfo.setAltitude(location.getAltitude());
			
			if (MainActivity.DEBUG)
				Log.d("ImService MyLocListener", "changed the location data");
		}
	}
	
	/*
	 * shows tray notification when needed
	 */
	private void showNotificationIfNeeded(String username_to_chat) {

		Intent notifyOpensChatIntent = null;
//		TaskStackBuilder stackBuilder = null;
		PendingIntent contentIntent = null;
		NotificationCompat.Builder builder = null;
		
		notifyOpensChatIntent = new Intent(this, ChatActivity.class).
								putExtra(ChatActivity.USERNAME_TO_CHAT_WITH_EXTRA, username_to_chat).
								setAction(ChatActivity.MESSAGE_TO_A_USER);
		
        contentIntent = PendingIntent.getActivity(getApplicationContext(), username_to_chat.hashCode(), notifyOpensChatIntent, PendingIntent.FLAG_ONE_SHOT);
//		stackBuilder = TaskStackBuilder.
//							create(this).
//							addParentStack(ChatActivity.class).
//							addNextIntent(notifyOpensChatIntent);
//		contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT); // here the error
		builder = new NotificationCompat.Builder(this).setContentIntent(contentIntent);
		
		
		Notification n = builder.
				setContentTitle(getString(R.string.it_notification_new_message_title)).
				setContentText(getString(R.string.it_notification_new_message_text) + " " + username_to_chat).
				setSmallIcon(R.drawable.ic_stat_new_message).
//				setAutoCancel(true).
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
	
	private void notifyUserStateChanged(UserInfo u, String state) {
		TemporaryStorage.reorderUserList();
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_USERNAME_EXTRA, u.getUsername());
		userStateChange.putExtra(INTENT_ACTION_USER_STATE_CHANGED_STATE_EXTRA, state);
		LocalBroadcastManager.getInstance(this).sendBroadcast(userStateChange);
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
		
		TemporaryStorage.myInfo.setOnline();
		TemporaryStorage.myInfo.set(username, myInfo.getIp(), myInfo.getPort());
		TemporaryStorage.myInfo.locationData(false);
		
		isLogged = true;
		isMapActivated = false;
		
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
		
		deactivateMap();
		communication.logout(TemporaryStorage.myInfo, TemporaryStorage.user_list);
		
		isLogged = false;
		
		TemporaryStorage.myInfo.clearInfos();
		synchronized (TemporaryStorage.user_list) {
			TemporaryStorage.user_list.clear();
		}
		TemporaryStorage.messages.clear();
		
		userInfoReprs.clear();
		
		handler.removeCallbacks(msgAckManager);
		handler.removeCallbacks(userInfoRequestManager);
		notifManager.cancelAll();
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
		
		MessageInfo mi = new MessageInfo(TemporaryStorage.myInfo, user_to_chat, the_message);
		
		communication.sendMessage(mi);
		
		MessageRepresentation mr = new MessageRepresentation(mi);

		
		sentMessagesWaitingForAnswer.add(mr);
		// add the message to storage and notify chat activity if needed
		TemporaryStorage.addMessage(user_to_chat, mr);
		notifyNewMessageToChatActivity(username_to_chat);

	}
	
	@Override
	public boolean sendMessageToAll(String msg) {
		boolean toRet = true;
		
		UserInfo[] uis = TemporaryStorage.user_list.toArray(new UserInfo[0]);
		
		for (UserInfo ui : uis) {
			if (!ui.isOnline())
				continue;
			try {
				sendMessage(ui.getUsername(), msg);
			} catch (UserNotLoggedInException e) {
				toRet = false;
			} catch (UserToChatWithIsNotRecognizedException e) {
				toRet = false;
			} catch (CannotSendBecauseOfWrongUserInfo e) {
				toRet = false;
			} catch (InvalidDataException e) {
				toRet = false;
			}
		}
		return toRet;
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
		if (MainActivity.DEBUG)
			Log.d("IMService - setCurrentUserChat", username_to_chat);
		notifManager.cancel(username_to_chat.hashCode());
	}
	
	
	@Override
	public void activateMap(int my_rate, int others_rate) {
		if (isMapActivated || !isLogged)
			return;
		isMapActivated = true;
		my_location_refresh_rate = my_rate;
		others_location_refresh_rate = others_rate;
		
		TemporaryStorage.myInfo.locationData(true);
		if (lm != null)
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, my_location_refresh_rate, myLoclistener, Looper.getMainLooper());	
	}
	
	public void deactivateMap() {
		if (!isMapActivated || !isLogged)
			return;
		
		isMapActivated = false;
		TemporaryStorage.myInfo.locationData(isMapActivated);
		lm.removeUpdates(myLoclistener);
	}

	@Override
	public boolean isMapActivated() {
		return isMapActivated;
	}

	@Override
	public int getMyRefreshTime() {
		return my_location_refresh_rate;
	}

	@Override
	public int getOthersRefreshTime() {
		return others_location_refresh_rate;
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
	@Override
	public void setUserList(Vector<UserInfo> userList) {
		synchronized (TemporaryStorage.user_list) {
			synchronized (userInfoReprs) {
				for (UserInfo userInfo : userList) {
					
					if (TemporaryStorage.user_list.contains(userInfo)) {
						if (MainActivity.DEBUG)
							Log.d("setUserList", "NON DOVREI ESSERE QUI " + userInfo.toString());
						continue;
					}
					if (MainActivity.DEBUG)
						Log.d("setUserList", userInfo.toString());
					TemporaryStorage.user_list.add(userInfo);
					userInfoReprs.add(new UserInfoRepr(userInfo, Calendar.getInstance().getTime()));
				}
			}
		}
		TemporaryStorage.reorderUserList();
	}

	@Override
	public void recievedMessage(MessageInfo mi) {
		String username_to_chat;
		UserInfo source = mi.getSource();
		
		/* if the message source is me... then it is spoofed */
		if (source.equals(TemporaryStorage.myInfo))
			return;
		
		/* if the destination of the message is not me... then nothing to do */
		if (!mi.getDestination().equals(TemporaryStorage.myInfo))
			return;
		
		if (!TemporaryStorage.user_list.contains(source)) {
			/*
			 * this is the case when a user has registered, logged 
			 * in and I did not received login message but he now writes to me.
			 */
			synchronized (TemporaryStorage.user_list) {
				TemporaryStorage.user_list.add(source);
			}
			
			synchronized (userInfoReprs) {
				userInfoReprs.add(new UserInfoRepr(source, Calendar.getInstance().getTime()));
			}
		}
		
		username_to_chat = source.getUsername();
		
		MessageRepresentation mr = new MessageRepresentation(mi);
		mr.ackRecieved = true;
		
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
		
		synchronized (TemporaryStorage.user_list) {
			UserInfo u = TemporaryStorage.user_list.get(TemporaryStorage.user_list.indexOf(source));
			u.locationData(source.hasLocationData());
			if (source.hasLocationData()) {
				u.setAltitude(source.getAltitude());
				u.setLongitude(source.getLongitude());
				u.setLatitude(source.getLatitude());
			}
			u.setIP(source.getIp());
			u.setPort(source.getPort());
			u.setOffline();
		}
		
		
		
		notifyUserStateChanged(source, UserInfo.OFFLINE_STATUS);
	}

	@Override
	public void userLoggedIn(UserInfo source) {
		if (source.equals(TemporaryStorage.myInfo))
			return;
		if (!TemporaryStorage.user_list.contains(source)) {
			/*
			 * this is the case when a user has registered after I logged in - so I don't know him.
			 */
			synchronized (TemporaryStorage.user_list) {
				TemporaryStorage.user_list.add(source);
			}
			
			synchronized (userInfoReprs) {
				userInfoReprs.add(new UserInfoRepr(source, Calendar.getInstance().getTime()));
			}
		}
		receivedUserInfoAnswer(source);
		
	}
	
	@Override
	public void receivedMessageAnswer(UserInfo user, int messageHashAck) {
		if (user.equals(TemporaryStorage.myInfo))
			return;
		
		for (MessageRepresentation mr : sentMessagesWaitingForAnswer) {
			if (mr.getMessageInfo().getDestination().equals(user) && mr.getMessageInfo().hashCode() == messageHashAck) {
				mr.ackRecieved = true;
				notifyNewMessageToChatActivity(user.getUsername());
				receivedUserInfoAnswer(user);
				return;
			}
		}
		
	}

	@Override
	public void receivedUserInfoRequest(UserInfo source) {
		if (source.equals(TemporaryStorage.myInfo))
			return;
		receivedUserInfoAnswer(source);
		communication.sendUserInfoAnswer(TemporaryStorage.myInfo, source);
	}

	@Override
	public void receivedUserInfoAnswer(UserInfo source) {		
		UserInfoRepr uir = null;
		try {
			uir = userInfoReprs.get(userInfoReprs.indexOf(new UserInfoRepr(source, null)));
		} catch (ArrayIndexOutOfBoundsException e) {
			//ok
		}
		
		if (uir == null)
			return;

		
		boolean previous_state = uir.u.isOnline();

		uir.u.locationData(source.hasLocationData());
		if (source.hasLocationData()) {
			uir.u.setAltitude(source.getAltitude());
			uir.u.setLongitude(source.getLongitude());
			uir.u.setLatitude(source.getLatitude());
		}
		uir.u.setIP(source.getIp());
		uir.u.setPort(source.getPort());
		uir.u.setOnline();
		
		if (!previous_state)
			notifyUserStateChanged(uir.u, UserInfo.ONLINE_STATUS);
		
		uir.last_update = Calendar.getInstance().getTime();
		
	}
	
	/*
	 * *************************************************************************
	 * *************************************************************************
	 */	
}
