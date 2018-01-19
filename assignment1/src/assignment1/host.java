package assignment1;
import java.net.*;
import java.util.Arrays;
import java.io.*;

public class host {
	protected DatagramSocket receiveSocket, sendReceiveSocket;
	protected DatagramPacket sendReceivePacket, receivePacket;
	
	public host() {
		int port = 23;
		try {
			// Create a datagram socket to send and receive
			sendReceiveSocket = new DatagramSocket();
			// Create datagram socket to receive (port 23)
			receiveSocket = new DatagramSocket(port);
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
			//len = receivePacket.getLength();
			//port = receivePacket.getPort();
			//address = receivePacket.getAddress();
			
			// Try to receive request
			try {
				System.out.println("Waiting to receive...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("Receive socket timed out. \n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// Print out information it has received
			printPacketReceived(receivePacket);
			
			// Form a packet to send containing exactly what it received
			try {
				sendReceivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), receivePacket.getPort()); 
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// Send packet on its send/receive socket to port 69
			try {
				sendReceiveSocket.send(sendReceivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	private void sendToPort69(DatagramPacket dp)  {
		
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
