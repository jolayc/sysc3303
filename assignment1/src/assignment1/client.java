package assignment1;
import java.net.*;
import java.io.*;

public class client {
	private DatagramSocket sendReceiveSocket;
	private byte[] requests;
	private DatagramPacket sendPacket;
	
	public client() {
		/* Construct a datagram socket and bind it to any available
		 * port and on the local host machine. This socket will be used to 
		 * send and receive UDP Datagram packets.
		 */
		try {
			sendReceiveSocket = new DatagramSocket();
			requests = new byte[20];
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		try {
			for (int i = 0; i < 11; i++) {
				requests = null;
				sendPacket = new DatagramPacket(requests, requests.length, InetAddress.getLocalHost(), 23);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		client c = new client();
		
	}
}
