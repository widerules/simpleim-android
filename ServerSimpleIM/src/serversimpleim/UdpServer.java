package serversimpleim;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpServer extends BaseServer {
	    DatagramSocket socket = null;


	    public UdpServer() throws IOException {
	    	this(4445);
	    }


	    public UdpServer(int port) throws IOException {
	        super();
	        socket = new DatagramSocket(port);
	    }


	    public void run() {
	        byte[] buf;
	        String request, response;
	        DatagramPacket packet;
	    	  
	        while (true) {

	            buf = new byte[1024];
	            packet = new DatagramPacket(buf, buf.length);
	                
	            try {
	// receive request
	                socket.receive(packet);
	                request = new String(buf).trim();
	System.out.println(request);

	// interpret commands
	
//	                response = interpretCmd(request);
	
//	System.out.println(response);

	// send the response to client at "address" and "port"
//	                buf = response.getBytes();
//	                InetAddress address = packet.getAddress();
	                int port = packet.getPort();
//	                packet = new DatagramPacket(buf, buf.length, address, port);
	                socket.send(packet);
	                
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }


	    @Override
	    protected void finalize() throws Throwable {
	    	socket.close();
	        super.finalize();
	    }

	}

