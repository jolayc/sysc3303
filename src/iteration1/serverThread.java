package iteration1;
import java.net.*;
import java.io.IOException;
import java.util.Arrays;


/**
 * A server thread that will handle read/write requests received by server, sent by client 
 */

public class serverThread extends Thread implements Runnable {
	
	// byte values being used
	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	
	private DatagramSocket sendSocket;
	private DatagramPacket receivePacket, sendPacket;
	
	// String identifiers
	private String message;
	private String read = "READ";
	private String write = "WRITE";
	
	public serverThread(DatagramPacket receivePacket, String message) {
		this.message = message;
		this.receivePacket = receivePacket;
	}
	
	public void run() {
		byte response[] = new byte[4];
		
		// create response packet
		// send data packet when receiving a read request
		if (message.equals(read)) response = createDataPacket();
		
		// send a acknowledge packet when receiving a write request or data packet
		else if(message.equals(write)) response = createACKPacket();
		
		// Construct a socket to send packets to any available port
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		// create a datagram packet that will contain sendBytes that will be ported to the same
		// port as receivePacket
		try {
			sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receivePacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		sendPack(sendSocket, sendPacket);
		// close socket when done sending the packet and stop thread
		sendSocket.close();
	}
	
	private void sendPack(DatagramSocket sock, DatagramPacket dp) {
		printSend(dp);
		try {
			sock.send(dp);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Create a data packet containing {0,3,0,1}
	 * @return byte[4] data packet
	 */
	public byte[] createDataPacket() {
		return new byte[] {0,3,0,1};
	}
	
	/**
	 * Create an acknowledge packet containing {0,4,0,0}
	 * @return byte[4] acknowledge packet
	 */
	public byte[] createACKPacket() {
		return new byte[] {0,4};
	}
	
	// Print information relating to send request 
	private void printSend(DatagramPacket dp) {
		System.out.println("Host: Sending packet");
		System.out.println("To host: " + dp.getAddress());
		System.out.println("Destination Port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print information relating to packet
	private void printInfo(DatagramPacket dp) {
		int len = dp.getLength();
		System.out.println("Length: " + len);
		System.out.println("Containing: ");

		// prints the contents of packet as bytes
		System.out.println(Arrays.toString(dp.getData()));
		// prints the contents of packet as a String
		String contents = new String(dp.getData(),0,len);
		System.out.println(contents);
	}
}
