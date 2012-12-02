package serversimpleim;

import com.tolmms.simpleim.datatypes.CommunicationMessage;
import com.tolmms.simpleim.datatypes.UserInfo;

public class Main {

	public Main() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		new TcpServer(4445).run();
		

	}

}
