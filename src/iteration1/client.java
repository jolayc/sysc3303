package iteration1;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Client
 * Client side of a simple echo server.
 * The client sends a request packet to the intermediate host
 * It then receives a reply packet from the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */


public class client {
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte four = 0x04;
	
	private String filename;
	private String mode;
	
	private int blockNum;
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	public client(){
		// String filename, String mode
		try {
			//constructs a socket to send and receive packets from any available port
			sendReceiveSocket = new DatagramSocket();
			byte[] data = new byte[4];
		    receivePacket = new DatagramPacket(data, data.length);
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Sends a read request to the server, waits for a DATA packet
	 * and responds with an ACK
	 * @param filename
	 */
	public void sendRead(String filename){
		System.out.println("Requesting to read from server with filename: " + filename);
		byte[] incomingData;
		byte[] ack;
		int curBlockNum = 1;
		
		// Create and send request
		DatagramPacket readRequest = createRRQPacket(filename);
		printSend(readRequest);
		try {
			sendReceiveSocket.send(readRequest);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Process response from Server
		while (true) {
			// Server responds to a Read Request with a DATA packet (save to receivePacket)
			incomingData = new byte[4 + 512]; // 2 for opcode, 2 for block and 512 bytes for max block size
			receivePacket = new DatagramPacket(incomingData, incomingData.length);
			
			// Receive packet from server
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// Check block number received
			int dataBlockNum = getBlockNum(receivePacket.getData());
			
			// Compare block
			if (curBlockNum != dataBlockNum) {
				System.out.println("Unmatching block numbers, exiting.");
				System.exit(1);
			}
			
			// Print contents
			System.out.println("Client: Received DATA block from server: ");
			printStatus(receivePacket);
			
			// Create and send ACK
			ack = createACKPacket(curBlockNum);
			sendPacket = new DatagramPacket(ack, ack.length);
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// check if end of read
			if(incomingData.length < 512) {
				System.out.println("Client: Read complete, blocks received: " + curBlockNum);
				break;
			}
			
			// increment current block number
			curBlockNum++;
		}
	}
	
	/**
	 * Create ACK packet to be sent to server during a read request
	 * @param block
	 * @return ACK packet {0, 4, block number(hi), block number(lo)}
	 */
	private byte[] createACKPacket(int block) {
		byte[] pack = new byte[4];
		byte[] num = new byte[2];
		// {0, 4} op code
		pack[0] = zero;
		pack[1] = four;
		// convert block number to bytes
		num[0] = (byte)(block & 0xFF);
		num[1] = (byte)((block >> 8) & 0xFF);
		// load block number into ack packet
		pack[2] = num[0];
		pack[3] = num[1];
				
		return pack;
	}
	
	private void printFromServer(byte[] data) {
		
	}
	
	/**
	 * Returns the block number as an integer
	 * @param data byte[] containing opcode, block num (2 bytes) and data
	 * @return int Block number
	 */
	private int getBlockNum(byte[] data) {
		return ((data[2] & 0xff) << 8) | (data[3] & 0xff);
	}
	
	public void sendWrite(String filename){
		
	}
	
	public boolean checkACK(byte[] b) {
		byte[] tmp = new byte[] {0,4};
		return Arrays.equals(tmp, b);
	}
	
	/**
	 * Sends a packet to a socket
	 * @param socket, DatagramSocket where the packet will be sent
	 * @param packet, DatagramPacket that will be sent
	 */
	public void sendPack(DatagramSocket socket, DatagramPacket packet) {
		printSend(sendPacket);
		try {
			 socket.send(packet);
		 }
		 catch(IOException io){
			 io.printStackTrace();
			 System.exit(1);
		 }
	}
	
	/**
	 * Receives a packet from a socket
	 * @param socket, DatagramSocket where the packet data will be received from
	 * @param packet, DatagramPacket where the data from the socket will be stored
	 */
	public DatagramPacket receivePack(DatagramSocket socket, DatagramPacket packet) {
		System.out.println("Client: Waiting for Packet.\n");
		try {        
	         System.out.println("Waiting...");
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		printReceive(packet);
		return packet;
	}
	
	/**
	 * Creates read request packet
	 * @return DatagramPacket containing read request
	 */
	public DatagramPacket createRRQPacket(String filename){
		
		// |Opcode (2 bytes)|
		byte[] rrq = new byte[100];
		rrq[0] = zero;
		rrq[1] = one;
		// |Filename|0|Mode|0|
		finishRRQOrWRQ(rrq, filename, "netascii");
		
		return createSendPacket(rrq);
	}
	
	/**
	 * Creates write request packet
	 * @return DatagramPacket containing read request
	 */
	public DatagramPacket createWRQPacket(String filename){
		
		byte[] wrq = new byte[20];
		wrq[0] = one;
		wrq[1] = zero;
		
		finishRRQOrWRQ(wrq, filename, "netascii");
		
		return createSendPacket(wrq);
	}
	
	/**
	 * Finishes the request packet for both read and write
	 * @param rq, byte[] with start of request
	 */
	private void finishRRQOrWRQ(byte[] rq, String filename, String mode){
		
		int offset = 0;
		
		//contains the bytes of the global strings
	    byte[] filebyte = filename.getBytes();
		byte[] modebyte = mode.getBytes();	
		
		for(int ch = 0; ch < filebyte.length; ch++){
	    	rq[2+ch] = filebyte[ch];
	    	offset = 3 + ch;
	    }
		rq[offset] = zero;
		
		for(int ch = 0; ch < modebyte.length; ch++){
	    	rq[offset + ch] = modebyte[ch];
	    	if (ch == modebyte.length - 1) offset = offset + ch;
	    }
		rq[offset] = zero;
	}
	
	/**
	 * Creates a send packet
	 * @param rq, byte[] with request
	 * @return DatagramPacket with send request
	 */
	public DatagramPacket createSendPacket(byte[] rq){
		DatagramPacket send = null;
		try {
			send = new DatagramPacket(rq, rq.length, InetAddress.getLocalHost(), 23);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		return send;
	}
	
	/**
	 * Creates a send packet
	 * @param block, byte[] with data
	 * @return DatagramPacket with receive request
	 */
	public DatagramPacket createReceivePacket(byte[] block){
		return new DatagramPacket(block, block.length);
	}

	/**
	 * Prints information relating to a send request
	 * @param packet, DatagramPacket that is used in the send request
	 */
	private void printSend(DatagramPacket packet){
		System.out.println("Client: Sending packet");
	    System.out.println("To host: " + packet.getAddress());
	    System.out.println("Destination host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("Client: Packet received");
	    System.out.println("From host: " + packet.getAddress());
	    System.out.println("Host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a any request
	 * @param packet, DatagramPacket that is used in the request
	 */
	private void printStatus(DatagramPacket packet){
	    int len = packet.getLength();
	    System.out.println("Length: " + len);
	    System.out.print("Containing: ");
	    
	    //prints the bytes of the packet
	    System.out.println(Arrays.toString(packet.getData()));
	    
	    //prints the packet as text
		String received = new String(packet.getData(),0,len);
		System.out.println(received);
	}
	
	private byte[] toBytes() {
		Path path = Paths.get("C:\\Users\\karlschnalzer\\Desktop"); /* CHANGE THIS TO PATH ON LAB MACHINE */
		byte[] fb = null;
		try {
			fb = Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return fb;
	}
	
	private void shutdown() {
		sendReceiveSocket.close();
		System.out.println("Client: Terminated.");
		System.exit(1);
	}
	
	public static void main(String args[]){
		client c = new client();
		Scanner sc =  new Scanner(System.in);
		
		while (true) {
			System.out.println("Client: Enter file name or 'exit' to terminate.");
			String in = sc.nextLine().toLowerCase();
			if (in.equals("exit")) break;
			System.out.println("Client: Enter 'r' for read request or 'w' for write request");
			String command = sc.nextLine().toLowerCase();
//			try {
//				if (command.equals("r")) {
//					c.sendRead(in);
//				} else if (command.equals("w")) {
//					c.sendWrite(in);
//				} else {
//					System.out.println("Command not recognized.");
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			if (command.equals("r")) {
				c.sendRead(in);
			} else if (command.equals("w")) {
				c.sendWrite(in);
			} else {
				System.out.println("Command not recognized.");
			}
		}
		sc.close();
		c.shutdown();
	}
}
