package serversimpleim.datatypes;

import java.util.Calendar;
import java.util.Date;

import com.tolmms.simpleim.datatypes.UserInfo;

public class SimpleIMUser {
	UserInfo user;
	String password;
	public Date last_update;
	
	public SimpleIMUser(UserInfo user, String password) {
		this.user = user;
		this.password = password;
		
		this.last_update = Calendar.getInstance().getTime();
	}

	UserInfo getUserInfo() {
		return user;
	}
	
	public boolean samePassword(String password) {
		return this.password.equals(password);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		SimpleIMUser other = (SimpleIMUser) obj;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	public UserInfo getUser() {
		return user;
	}
	
	public String getPassword() {
		return password;
	}
	
}
