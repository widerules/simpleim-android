package com.tolmms.simpleim.datatypes;

import java.util.Calendar;
import java.util.Date;

public class UserInfoRepr {
	public UserInfo u;
	public Date last_update;
	
	public UserInfoRepr(UserInfo u, Date last_update) {
		this.u = u;
		this.last_update = last_update;
	}
	
	public UserInfoRepr(UserInfo u) {
		this.u = u;
		last_update = Calendar.getInstance().getTime();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((u == null) ? 0 : u.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserInfoRepr other = (UserInfoRepr) obj;
		if (u == null) {
			if (other.u != null)
				return false;
		} else if (!u.equals(other.u))
			return false;
		return true;
	}
}