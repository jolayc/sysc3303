package iteration1;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
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
public class server implements Runnable {
	
	private DatagramSocket sendSocket, receiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	private byte[] path;
	
	private int port = 69;
	//private int offset;
	
	private String rq;
	private String read = "READ";
	private String write = "WRITE";
	
	private int[] blockNumber;
	
	
	public server() {
		try {
			// Construct a socket to receive bounded to port 69
			receiveSocket = new DatagramSocket(port);
			blockNumber = new int[2];
			rq = "NONE";
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run() {
		while (!receiveSocket.isClosed()) {
			byte[] data = new byte[4 + 512];
			// Wait on port 69
			receivePacket = new DatagramPacket(data, data.length);
			receivePack(receiveSocket, receivePacket);
				
			if(!((rq.equals(read))||(rq.equals(write)))){
		
				path = toBytes(getPath(receivePacket));
				rq = checkReadWrite(receivePacket.getData());
				
				if(rq.equals(read)){
					blockNumber[0] = 0;
					blockNumber[1] = 1;
				}
				// Create a thread to process request
				new Thread(new serverThread(receivePacket, path, rq, blockNumber)).start();
			}
			else if(rq.equals(read)){
				calcBlockNumber();
				new Thread(new serverThread(receivePacket, path, rq, blockNumber)).start();
			}
			else if(rq.equals(write)){
				calcBlockNumber();
				new Thread(new serverThread(receivePacket, path, rq, blockNumber)).start();
			}
		}
		shutdown();
	}
	
	/**
	 * Checks if the packet received is a read or write request
	 * @param data	Bytes from the datagram packet
	 * @return READ if read request, WRITE if write request
	 */
	private String checkReadWrite(byte[] data) {
	
		if(data[1] == one) rq = read;
		else rq = write;
		return rq;
	}
	
	/**
	 * Stops the continuous running loop
	 * of the server by setting the running flag 
	 * to false
	 */
	private synchronized void stop() {
		//running = false;
		receiveSocket.close();
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
	
	private String getPath(DatagramPacket packet){
		byte[] data = packet.getData();
		byte[] filename = new byte[data.length];
		byte[] path;
		
		for(int i = 0; i < data.length; i++){
			if(data[2+i] == 0){
				path = new byte[i];
				for(int j = 0; j < i; j++){
					path[j] = filename[j];
				}		
				return new String(path);
			}
			else{
				filename[i] = data[2+i];
			}
		}
		return null;
	}
	
	private byte[] toBytes(String p) {
		byte[] bytes = null;
		System.out.println(p);
		Path path = Paths.get(p);
		// Try to convert File into byte[]
		try {
			bytes = Files.readAllBytes(path);
		} catch (FileNotFoundException fe) {
			// File not found
			fe.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.exit(1);
		}
		// return file as bytes
		return bytes;
	}
	
	private int[] calcBlockNumber(){

		if(blockNumber[1] == 9) {
			blockNumber[0]++;
			blockNumber[1] = 0;
		}
		else{
			blockNumber[1]++;
		}
		
		if(blockNumber[0] > 9){
			blockNumber[0] = 0;
			blockNumber[1] = 1;
		}
		return blockNumber;
	}
	
	/**
	 * Receive a packet being sent to port 69
	 * @param sock DatagramSocket ported to port 69
	 * @param dp DatagramPacket being sent
	 */
	private void receivePack(DatagramSocket sock, DatagramPacket dp) {
		try {
			sock.receive(dp);
			printReceive(dp);
		} catch (SocketException se) {
			System.out.println("Socket is closed");
		} catch (IOException e) {
			System.exit(1);
		}
		
	}
	
	// Print information relating to receive request
	private void printReceive(DatagramPacket dp) {
		System.out.println("Server: Packet received");
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
		new Thread(s).start();
		System.out.println("Server: To exit, enter 'exit'");
		Scanner sc = new Scanner(System.in);
		for(;;) {
			if (sc.hasNextLine()) {
				String msg = sc.nextLine().toLowerCase();
				if (msg.equals("exit")) {
					sc.close();
					s.shutdown();
					break;
				}
			}
		}
	}
}
