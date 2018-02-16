package iter2;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
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
public class Server implements Runnable {
	
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	
	private final byte ONE = 1;

	private byte[] path;
	
	private int port = 69;
	private String relativePath = System.getProperty("user.dir");
	
	private String rq;
	private String read = "READ";
	private String write = "WRITE";
	
	private int[] blockNumber;

	private File f;
	
	/**
	 * Constructor for server
	 * creates a socket to receive packets from host on port 69
	 * creates block number
	 */
	public Server() {
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
	
	/**
	 * Runs the server
	 */
	public void run() {
		while (!receiveSocket.isClosed()) {
			byte[] data = new byte[4 + 512];
			// Wait on port 69
			receivePacket = new DatagramPacket(data, data.length);
			receivePack(receiveSocket, receivePacket);
				
			if(!((rq.equals(read))||(rq.equals(write)))){
				// might need to be fixed here
				rq = checkReadWrite(receivePacket.getData());
				
				if(rq.equals(read)){
					path = toBytes(relativePath + "\\Server\\" + getPath(receivePacket));
					blockNumber[0] = 0;
					blockNumber[1] = 1;
					new Thread(new ServerThread(receivePacket, path, null, rq, blockNumber)).start();
				}
				
				if(rq.equals(write)) {
					path = toBytes(relativePath + "\\Client\\" + getPath(receivePacket));
					try{
					f = new File(relativePath + "\\Server\\" + getFilename(receivePacket.getData()));
					}catch(FileAlradyExistsException fe){
						fe.printStackTrace();
						DatagramPacket errorPacket= createErrorPacket(ErrorCode.FILE_ALREADY_EXISTS);
						sendErrorPacket(errorPacket);
						System.out.println("File already exists in Server folder.");
						//System.exit(1);;
						this.shutdown();
					}
					new Thread(new ServerThread(receivePacket, path, f, rq, blockNumber)).start();
				}
				
			}
			else if(rq.equals(read)){
				calcBlockNumber();
				new Thread(new ServerThread(receivePacket, path, null, rq, blockNumber)).start();
			}
			else if(rq.equals(write)){
				calcBlockNumber();
				new Thread(new ServerThread(receivePacket, path, f, rq, blockNumber)).start();
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
	
		if(data[1] == ONE) rq = read;
		else rq = write;
		return rq;
	}
	
	// this method needs to be improved ***
	//private synchronized void stop() {
		 //running = false;
		// receiveSocket.close();		
	//}
	
	/**
	 * Shuts down the server by closing the socket used
	 * for receiving
	 */
	private synchronized void shutdown() {
		receiveSocket.close();
		System.out.println("Server: Requests are no longer being taken.");
		while (true) {} // allows for file transfers in progress to finish but refuse to create new connections
	}
	
	/**
	 * Gets the path of the data in the DatagramPacket
	 * @param packet DatagramPacket with the path
	 * @return String, with the path
	 */
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
	
	/**
	 * A function that returns the file name of a String
	 * from a Request packet
	 * @param data		The RQ packet as a byte[]
	 * @return			File name in RQ packet 
	 */
	private String getFilename(byte[] data) {
		int size = 0;
		for(int i = 2; i < data.length; i++) {
			if(data[i] == 0) break;
			size++;
		}
		byte[] temp = new byte[size];
		
		for(int i = 0; i < temp.length; i++) {
			temp[i] = data[i+2];
		}	
		return new String(temp);	
	}
	/**
	 * converts contents of file to byte array
	 * @param p path to file 
	 * @return byte array with the contents of file
	 */
	
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
	/**
	 * increments block number 
	 * @return block number as array on integers
	 */
	
	private int[] calcBlockNumber(){

		if(blockNumber[1] == 9) {
			blockNumber[0]++;
			blockNumber[1] = 0;
		}
		else{
			blockNumber[1]++;
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
	
	/**
	 * Print information relating to receive request
	 * @param dp datagram Packet being printed
	 */
	private void printReceive(DatagramPacket dp) {
		System.out.println("Server: Packet received");
		System.out.println("From host: " + dp.getAddress());
		System.out.println("Port: " + dp.getPort());
		printInfo(dp);
	}
	
	/**
	 *  Print information relating to packet
	 * @param dp datagram Packet being printed
	 */
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
		Server s = new Server();
		new Thread(s).start();
		System.out.println("Server: To exit, enter 'exit'");
		Scanner sc = new Scanner(System.in);
		
		//Check for exit command
		while(true) {
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
