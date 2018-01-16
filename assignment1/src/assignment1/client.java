package assignment1;
import java.net.*;
import java.io.*;

public class client {
	private DatagramSocket sendReceiveSocket;
	private byte[] requests;
	private DatagramPacket sendPacket;
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte two = 0x10;
	private final String filename = "text.txt";
	private final String mode = "netascii";
	
	public client() {
		/* Construct a datagram socket and bind it to any available
		 * port and on the local host machine. This socket will be used to 
		 * send and receive UDP Datagram packets.
		 */
		try {
			sendReceiveSocket = new DatagramSocket();
			requests = new byte[100];
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		try {
			for (int i = 0; i < 11; i++) {
				requests = generateByteArray(i);
				sendPacket = new DatagramPacket(requests, requests.length, InetAddress.getLocalHost(), 23);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private byte[] generateByteArray(int index) {
		byte[] fileBytes = filename.getBytes();
		byte[] modeBytes = mode.getBytes();
		byte[] byteArray = new byte[100];
		if (index == 10) {
			
		} else {
			switch(index) {
			case 0:
				
				break;
			default:
				
				break;
			}
		} return byteArray;
	}
	
	public static void main(String[] args) {
		client c = new client();
		c.sendAndReceive();
	}
}
