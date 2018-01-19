package assignment1;
import java.net.*;
import java.util.Arrays;
import java.io.*;

public class host {
	protected DatagramSocket receiveSocket, sendReceiveSocket, sendSocket;
	protected DatagramPacket sendPacket, receivePacket;
	private final int port = 23;
	
	public host() {
		try {
			// Create a datagram socket to send and receive
			sendReceiveSocket = new DatagramSocket();
			// Create datagram socket to receive (port 23)
			receiveSocket = new DatagramSocket(port);
			sendSocket = new DatagramSocket();
		} catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void loop() {
		byte[] data;
		int len, port;
		InetAddress address;
		
		while(true) {
			data = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			
			// Attempt to retrieve data from socket
			try {
				System.out.println("Waiting to receive...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("Receive socket timed out. \n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// Print out information
			printPacketReceived(receivePacket);
			
			// Form a packet to send containing exactly what it received
			
		}
	}
	
	private void printPacketReceived(DatagramPacket dp) {
		System.out.println("Packet received: ");
		System.out.println("From host: " + dp.getAddress());
		System.out.println("From port: " + dp.getPort());
		printPacketInfo(dp);
	}
	
	private void printPacketInfo(DatagramPacket dp) {
		int len = dp.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: (bytes) ");
		System.out.print(Arrays.toString(dp.getData()));
		String packet = new String(dp.getData(), 0, len);
		System.out.println(", (text) " + packet);
	}
	
	public static void main(String[] args) {
		host h = new host();
		h.loop();
	}

}
