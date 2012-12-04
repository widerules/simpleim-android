package com.tolmms.simpleim.storage;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import com.tolmms.simpleim.datatypes.UserInfo;

public class TemporaryStorage {
	public static Vector<UserInfo> user_list = new Vector<UserInfo>();
	public static HashMap<UserInfo, String> messages = new HashMap<UserInfo, String>();
//	private HashMap<UserInfo, Vector<Pair<UserInfo, String>>> messages
	
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
	
	
}
