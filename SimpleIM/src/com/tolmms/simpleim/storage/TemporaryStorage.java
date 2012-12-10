package com.tolmms.simpleim.storage;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import com.tolmms.simpleim.datatypes.UserInfo;

public class TemporaryStorage {
	/**
	 * The collection of all users that app knows about
	 */
	public static Vector<UserInfo> user_list = new Vector<UserInfo>();
	
	/**
	 * Per each user in user_list contains the cllection of all messages of that user.
	 */
	public static HashMap<UserInfo, Vector<String>> messages = new HashMap<UserInfo, Vector<String>>();
	
	/**
	 * Contains My Info
	 */
	public static UserInfo myInfo = new UserInfo(null, null, null);
	
	
	
	//public static HashMap<UserInfo, Vector<Pair<UserInfo, String>>> messages = new HashMap<UserInfo, Vector<Pair<UserInfo,String>>>();
//	public static Vector<Pair<UserInfo, String>> getMessagesByUser(UserInfo user_to_chat) {
//		Vector<Pair<UserInfo, String>> ms;
//		
//		ms = messages.get(user_to_chat);
//		
//		if (ms == null) {
//			ms = new Vector<Pair<UserInfo,String>>();
//			messages.put(user_to_chat, ms);
//		}
//		
//		return ms;
//	}
//	
//	public static Vector<Pair<UserInfo, String>> getMessagesByUsername(String username) {
//		return getMessagesByUser(getUserInfoByUsername(username));
//	}
//	
//	
//	
//	public static void addMessage(UserInfo user_to_chat, UserInfo user_from, String msg) {
//		Vector<Pair<UserInfo, String>> ms = getMessagesByUser(user_to_chat);
//		
//		ms.add(new Pair<UserInfo, String>(user_from, msg));
//		
//		//TODO bisogna limitare la capacita della storia dei msg
//	}
	
	
	
	/**
	 * Reorders the list of all users.
	 * First come the online users and then the others (in alphabetical order!)
	 */
	public static void reorderUserList() {
		Collections.sort(user_list, new Comparator<UserInfo>() {
			@Override
			public int compare(UserInfo lhs, UserInfo rhs) {
				if (lhs.getStatus().compareTo(rhs.getStatus()) == 0)
					return lhs.getUsername().compareTo(rhs.getUsername());
				if (lhs.getStatus().equals(UserInfo.ONLINE_STATUS))
					return -1;
				return 1;
			}
		});
	}

	/**
	 * returns the UserInfo of username_to_chat
	 * @param username_to_chat
	 * @return the UserInfo or a null if the user_to_chat is not in user_list
	 */
	public static UserInfo getUserInfoByUsername(String username_to_chat) {
		UserInfo toRet = null;
		try {
			toRet = user_list.get(user_list.indexOf(new UserInfo(username_to_chat, null, null)));
		} catch (ArrayIndexOutOfBoundsException e) { }
		
		return toRet;
	}
	
	/**
	 * returns the messages of the user_to_chat
	 * @param user_to_chat
	 * @return the vector containing the user_to_chat messages or a null ref if user_to_chat is not in user_list
	 */
	public static Vector<String> getMessagesByUser(UserInfo user_to_chat) {
		if (!user_list.contains(user_to_chat))
			return null;
		
		Vector<String> ms;
		
		ms = messages.get(user_to_chat);
		
		if (ms == null) {
			ms = new Vector<String>();
			messages.put(user_to_chat, ms);
		}
		
		return ms;
	}
	
	/**
	 * 
	 * @param username
	 * @return the messages of user having username = username or null if a user with username does not exists in user_list
	 */
	public static Vector<String> getMessagesByUsername(String username) {
		return getMessagesByUser(getUserInfoByUsername(username));
	}
	
	
	
	/**
	 * adds a message in user_to_chat message history
	 * @param user_to_chat
	 * @param msg
	 * @return true if message is added successfully or false  if the user_to_chat is not in user_list (the msg is not added!)
	 */
	public static boolean addMessage(UserInfo user_to_chat, String msg) {
		if (!user_list.contains(user_to_chat))
			return false;
		Vector<String> ms = getMessagesByUser(user_to_chat);
		
		ms.add(msg);
		
		//TODO bisogna limitare la capacita della storia dei msg
		
		return true;
	}
	
	
}
