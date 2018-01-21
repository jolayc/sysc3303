package assignment1;
import java.net.*;
import java.util.Arrays;
import java.io.*;

public class host {
	protected DatagramSocket receiveSocket, sendReceiveSocket, sendSocket;
	protected DatagramPacket sendReceivePacket, receivePacket;
	
	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	
	public host() {
		int port = 23;
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
	
	private void loop() {
		byte[] data;
		DatagramSocket sock;
		InetAddress client;
		int len, req, clientPort;
		String reqString;
		byte[] filename, mode;
		
		while(true) {
			data = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			
			// Try to receive request
			try {
				System.out.println("Waiting to receive...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("Receive socket timed out. \n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// Host and port
			client = receivePacket.getAddress();
			clientPort = receivePacket.getPort();
			len = receivePacket.getLength();
			
			// Print info about packet received
			printPacketInfo(receivePacket);
			
			// Form a packet containing exactly what is received
			try {
				sendReceivePacket = new DatagramPacket(data, len, InetAddress.getLocalHost(), 69);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// Send packet to host thru send/receive socket port 69
			try {
				sendReceiveSocket.send(sendReceivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate Host: Sending packet to client:");
			System.out.print(Arrays.toString(receivePacket.getData()));
			
			// Form a packet to send back to the host sending the request
			// Print out info being sent and send the request
			sendReceivePacket = new DatagramPacket(data, receivePacket.getLength(), client, clientPort);
			try {
				sendSocket.send(sendReceivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	private void close() {
		sendReceiveSocket.close();
		receiveSocket.close();
		System.out.println("Sockets closed.");
	}
	
	private void printPacketInfo(DatagramPacket dp) {
		int len = dp.getLength();
		byte[] data = dp.getData();
		System.out.println("Length: " + len);
		System.out.print("Containing: (bytes) ");
		System.out.println(Arrays.toString(dp.getData()));
		String packet = new String(dp.getData(), 0, len);
		String request = data[1] == one ? "Read":"Write";
		System.out.println("(type): " + request + " (filename and mode): " + packet);
	}
	
	public static void main(String[] args) {
		host h = new host();
		h.loop();
	}

}
