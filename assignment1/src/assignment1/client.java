package assignment1;
import java.net.*;
import java.io.*;

public class client {
	protected DatagramSocket sendReceiveSocket;
	protected DatagramPacket sendPacket, receivePacket;
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte two = 0x10;
	
	protected final String filename = "text.txt";
	protected final String mode = "netascii";
	protected int port = 23;
	
	protected byte[] msg;
	
	public client() {
		/* Construct a datagram socket and bind it to any available
		 * port and on the local host machine. This socket will be used to 
		 * send and receive UDP Datagram packets.
		 */
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		try {
			for (int i = 0; i < 11; i++) {
				msg = getByteArray(i);
				sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 23);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("Client: Sending packet: ");
		
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Client: Packet sent.\n");
	}
	
	private byte[] getByteArray(int index) {
		
	}
	
	public static void main(String[] args) {
		client c = new client();
		c.sendAndReceive();
	}
}
