package iteration1;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
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
	
	private String filename;
	private String mode;
	
	private int blockNum;
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	public client(String filename, String mode){
		
		try {
			//constructs a socket to send and receive packets from any available port
			sendReceiveSocket = new DatagramSocket();
			byte[] data = new byte[4];
		    receivePacket = new DatagramPacket(data, data.length);
		    
		    if(filename.equals(null)) this.filename = "";
		    else this.filename = filename;
		    
		    if(mode.equals(null)) this.mode = "";
		    else this.mode = mode;
		    
		    blockNum = 0;
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void sendRead(){
		sendPacket = createRRQPacket();
		sendPack(sendReceiveSocket, sendPacket);
		receivePacket = receivePack(sendReceiveSocket, sendPacket);
		
	}
	
	public void sendWrite(){
		sendPacket = createWRQPacket();
		sendPack(sendReceiveSocket, sendPacket);
		receivePacket = receivePack(sendReceiveSocket, sendPacket);
		if(checkACK(receivePacket.getData())) {
			sendFile();
		} else {
			
		}
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
		try{
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
	public DatagramPacket createRRQPacket(){
		
		byte[] rrq = new byte[100];
		rrq[0] = zero;
		rrq[1] = one;
		
		finishRRQOrWRQ(rrq);
		
		return createSendPacket(rrq);
	}
	
	/**
	 * Creates write request packet
	 * @return DatagramPacket containing read request
	 */
	public DatagramPacket createWRQPacket(){
		
		byte[] wrq = new byte[20];
		wrq[0] = one;
		wrq[1] = zero;
		
		finishRRQOrWRQ(wrq);
		
		return createSendPacket(wrq);
	}
	
	/**
	 * Finishes the request packet for both read and write
	 * @param rq, byte[] with start of request
	 */
	private void finishRRQOrWRQ(byte[] rq){
		
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
	
	private void sendFile() {
		byte[] data = toBytes(); // the data being sent as a byte array 
		int blockNum = (data.length / 512) + 1; // number of blocks being sent
		// byte[] pack = new byte[4 + data.length]; // the DATA packet [OPCODE (2), BLOCK # (2), DATA (n)]
		while () {
			
		}
	}
	
	
	public static void main(String args[]){
		client c1 = new client("test1.txt", "netascii");
		client c2 = new client("test2.txt", "octet");
		c1.sendRead();
		c2.sendWrite();
	}
}
