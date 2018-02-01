package iteration1;

import java.net.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Server of a client/server TFTP application
 * The server receives datagram packets from the intermediate host
 * (initially from the client) and responds with a datagram packet
 * depending on the request type
 * The response is created by a server thread created by the server
 * (multithreading)
 */
public class server {
	
	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	
	private int port = 69;
	
	private boolean running;
	
	public server() {
		running = true;
		try {
			// Construct a socket to receive bounded to port 69
			receiveSocket = new DatagramSocket(port);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run() {
		while (isRunning()) {
			byte[] data = new byte[20];
			// Wait on port 69
			receivePacket = new DatagramPacket(data, data.length);
			receivePack(receiveSocket, receivePacket);
			// Check request
			String rq = checkReadWrite(receivePacket.getData());
			// Create a thread to process request
			new Thread(new serverThread(receivePacket, rq));
		}
		shutdown();
	}
	
	/**
	 * Checks if the packet received is a read or write request
	 * @param data	Bytes from the datagram packet
	 * @return READ if read request, WRITE if write request
	 */
	private String checkReadWrite(byte[] data) {
		String rq;
		if(data[1] == one) rq = "READ";
		else rq = "WRITE";
		return rq;
	}
	
	/**
	 * Returns the running flag to check if 
	 * server should still be running
	 * @return True if server is running, False otherwise
	 */
	private synchronized boolean isRunning() {
		return running;
	}
	
	/**
	 * Stops the continuous running loop
	 * of the server by setting the running flag 
	 * to false
	 */
	private synchronized void stop() {
		running = false;
	}
	
	/**
	 * Shuts down the server by closing the socket used
	 * for receiving
	 */
	private synchronized void shutdown() {
		receiveSocket.close();
		System.out.println("Server: Requests are no longer being taken.");
		while (true) {} // allows for file transfers in progress to finish but refuse to create new connections
	}
		
	private void sendPack(DatagramSocket sock, DatagramPacket dp) {
		printSend(sendPacket);
		try {
			sock.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Receive a packet being sent to port 69
	 * @param sock DatagramSocket ported to port 69
	 * @param dp DatagramPacket being sent
	 */
	private void receivePack(DatagramSocket sock, DatagramPacket dp) {
		try {
			sock.receive(dp);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// Print information relating to send request 
	private void printSend(DatagramPacket dp) {
		System.out.println("Host: Sending packet");
		System.out.println("To host: " + dp.getAddress());
		System.out.println("Destination Port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print information relating to receive request
	private void printReceive(DatagramPacket dp) {
		System.out.println("Host: Packet received");
		System.out.println("From host: " + dp.getAddress());
		System.out.println("Port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print information relating to packet
	private void printInfo(DatagramPacket dp) {
		int len = dp.getLength();
		System.out.println("Length: " + len);
	    System.out.print("Containing: ");
	    
	    //prints the bytes of the packet
	    System.out.println("(bytes)" + Arrays.toString(dp.getData()));
	    
	    //prints the packet as text
		String received = new String(dp.getData(),0,len);
		System.out.println("(String)" + received);
	}
	
	public static void main(String[] args) {
		server s = new server();
		System.out.println("Server: To exit, enter 'exit'");
		Scanner sc = new Scanner(System.in);
		if (sc.hasNextLine()) {
			String msg = sc.nextLine().toLowerCase();
			if (msg.equals("exit")) {
				sc.close();
				s.stop();
			}
		}
	}
}
