package iteration4;

import java.net.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
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
	
	private final byte ZERO = 0x00;
	private final byte FOUR = 0x04;
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	
	private final byte ONE = 1;

	private byte[] path;
	
	private int port = 69;
	private String relativePath = System.getProperty("user.dir");
	private String directory;
	
	private String rq;
	private String read = "READ";
	private String write = "WRITE";
	private String other = "OTHER";
	
	private int[] blockNumber;
	
	private boolean multiClient;

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
			rq = "NONE";
			directory = relativePath + "\\Server\\";
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Runs the server, the Server waits on port 69
	 * for RRQ or WRQ packets, when it receives a RQ packet
	 * it creates a Thread to handle the transfer
	 */
	public void run() {
		while(!receiveSocket.isClosed()) {
			int[] blockNumber;
			// packet received buffer
			byte[] data = new byte[512 + 4];
			// Grab the request and create a ServerThread to handle the transfer
			receivePacket = new DatagramPacket(data, data.length);
			receivePack(receiveSocket, receivePacket);
			
			checkLegality(receivePacket);
			checkError(receivePacket);
 			rq = checkReadWrite(receivePacket.getData());
			if (rq.equals(read)) {
				path = toBytes(directory + getPath(receivePacket));
				blockNumber = new int[2];
				blockNumber[0] = 0;
				blockNumber[1] = 1;
				new Thread(new ServerThread(receivePacket, path, null, rq, blockNumber, multiClient)).start();
			} else if (rq.equals(write)) {
				blockNumber = new int[2];
				path = toBytes(relativePath + "\\Client\\" + getPath(receivePacket));
				try {
					f = new File(directory + "\\" + getFilename(receivePacket.getData()));
					
					if(f.exists()) {
						ErrorPacket errorPacket = new ErrorPacket(ErrorCode.FILE_ALREADY_EXISTS);
						sendErrorPacket(errorPacket);
						System.out.println("Server: Requested file already exists on machine!");
						shutdown();
					}
					// for error checking
					Writer w = new Writer(f.getPath(), false);
					w.close();
				} catch (AccessDeniedException e) {
					ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
					sendErrorPacket(fileAccessDenied);
					System.out.println("Access Violation.");
					shutdown();
				} catch (IOException e) {
					ErrorPacket diskFull = new ErrorPacket(ErrorCode.DISK_FULL_OR_ALLOCATION_EXCEEDED);
					sendErrorPacket(diskFull);
					System.out.println("The disk is full.");
					shutdown();
				}
				new Thread(new ServerThread(receivePacket, path, f, rq, blockNumber, multiClient)).start();
			} else {}
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
		else if (data[1] == (byte)2)rq = write;
		else rq = other;
		return rq;
	}
	
	/**
	 * Checks if there if packet is an error packet
	 * If it is, extract message
	 * @param packet, DatagramPacket that will be checked
	 */
	private void checkError(DatagramPacket packet) {
		
		//if packet is an error packet
		if(packet.getData()[1] == 5) {
			byte[] message = new byte[packet.getData().length - 5];
			
			//extract message
			for(int i = 0; i < message.length; i++) {
				if(packet.getData()[4+i] == 0) break;
				message[i] = packet.getData()[4+i];
			}
			System.out.println("Error! " + new String(message,0,message.length));
			shutdown();
		}
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
		Path path = Paths.get(p);
		// Try to convert File into byte[]
		try {
			bytes = Files.readAllBytes(path);
		} catch (AccessDeniedException e) {
			ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
			sendErrorPacket(fileAccessDenied);
			System.out.println("Access Violation.");
			shutdown();
		} catch (NoSuchFileException fe) {
			ErrorPacket fileNotFound = new ErrorPacket(ErrorCode.FILE_NOT_FOUND);
			System.out.println("File does not exist in server");
			sendErrorPacket(fileNotFound);
			System.exit(1);
		} catch (IOException e) {
			System.exit(1);
		}
		// return file as bytes
		return bytes;
	}
	
	/**
	 * Sends error packet to error simulator
	 * @param error, ErrorPacket to be sent
	 */
	public void sendErrorPacket(ErrorPacket error) {
		DatagramPacket errorPacket;
		try {
			DatagramSocket sendSocket = new DatagramSocket();
			errorPacket = new DatagramPacket(error.getBytes(), error.length(), InetAddress.getLocalHost(), 23);
			sendSocket.send(errorPacket);
			sendSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Checks the legality of a packet
	 * @param packet, DatagramPacket that will be checked
	 */
	public void checkLegality(DatagramPacket packet) {
		if(packet.getData()[0] != ZERO || packet.getData()[1] > FOUR) {	
			ErrorPacket illegalOperation = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
			sendErrorPacket(illegalOperation);
			System.out.println("Server Received Illegal TFTP Operation.");
			shutdown();
		}
	}
	
	/**
	 * Getter for server's inetaddress
	 * @return InetAddress of the server
	 */
	private InetAddress getInetAddress(){
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * Setter for instance's multiClient boolean
	 */
	private void setMultiClient(boolean multi){
		this.multiClient = multi;
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
		Scanner sc = new Scanner(System.in);	
		Server s = new Server();
		String msg;
		new Thread(s).start();
		try {
			InetAddress addr = InetAddress.getLocalHost();
			System.out.println("Server: This server is located at " + addr);
		} catch (UnknownHostException ue) {
			ue.printStackTrace();
		}
		
		
		//Check for exit command
		while(true) {
			System.out.println("Will this server allow for multiple concurrent clients? (y/n)");
			String multi;
			boolean multiSelected = false;
			
			while(!multiSelected){
				//server allows for multiple clients
				while(!sc.hasNext()) sc.next();
				multi = sc.nextLine();
				if(multi.equals("y")){
					s.setMultiClient(true);
					multiSelected = true;
				}else if(multi.equals("n")){
					s.setMultiClient(false);
					multiSelected = true;
				}
			}
			
			System.out.println("Server: To exit, enter 'exit'");
			System.out.println("Server: To change directory, enter 'cd'");
			
			if (sc.hasNextLine()) {
				msg = sc.nextLine().toLowerCase();
				if (msg.equals("exit")) {
					sc.close();
					s.shutdown();
					break;
				}
				if (msg.equals("cd")){
					System.out.println("Server: Enter new directory of server");
					while(!sc.hasNext()) sc.next();
					s.directory = sc.nextLine();
				}
			}
		}
	}
}
