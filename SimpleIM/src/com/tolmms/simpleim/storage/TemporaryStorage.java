package com.tolmms.simpleim.storage;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import android.util.Pair;

import com.tolmms.simpleim.datatypes.UserInfo;

public class TemporaryStorage {
	public static Vector<UserInfo> user_list = new Vector<UserInfo>();
	public static HashMap<UserInfo, Vector<String>> messages = new HashMap<UserInfo, Vector<String>>();
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

	public static UserInfo getUserInfoByUsername(String username_to_chat) {
		return user_list.get(user_list.indexOf(new UserInfo(username_to_chat, null, null)));
	}
	
	public static Vector<String> getMessagesByUser(UserInfo user_to_chat) {
		Vector<String> ms;
		
		ms = messages.get(user_to_chat);
		
		if (ms == null) {
			ms = new Vector<String>();
			messages.put(user_to_chat, ms);
		}
		
		return ms;
	}
	
	public static Vector<String> getMessagesByUsername(String username) {
		return getMessagesByUser(getUserInfoByUsername(username));
	}
	
	
	
	public static void addMessage(UserInfo user_to_chat, String msg) {
		Vector<String> ms = getMessagesByUser(user_to_chat);
		
		ms.add(msg);
		
		//TODO bisogna limitare la capacita della storia dei msg
	}
	
	
}
