package echo;


/**
 * EchoClient
 * Client side of a simple echo server.
 * The client sends a request packet to the intermediate host
 * It then recieves a reply packet from the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Client {
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	
	private String filename;
	private String mode;
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	public Client(String filename, String mode){
		
		try{
			//constructs a socket to send and receive packets from any available port
			sendReceiveSocket = new DatagramSocket();
			byte[] data = new byte[4];
		    receivePacket = new DatagramPacket(data, data.length);
		    
		    if(filename.equals(null)) this.filename = "";
		    else this.filename = filename;
		    
		    if(mode.equals(null)) this.mode = "";
		    else this.mode = mode;
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Sends a request packet to the intermediate host
	 * Receives a response packet from the intermediate host 
	 */
	public void sendAndReceive(){
				
			sendPacket = createRRQPacket();
			sendPack(sendReceiveSocket, sendPacket);
			sendPacket = createWRQPacket();
			sendPack(sendReceiveSocket, sendPacket);
			receivePack(sendReceiveSocket, receivePacket);
			receivePack(sendReceiveSocket, receivePacket);
	} 

	/**
	 * Sends a request packet to the intermediate host
	 * Receives a response packet from the intermediate host 
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
	
	public void receivePack(DatagramSocket socket, DatagramPacket packet) {
		
		System.out.println("Client: Waiting for Packet.\n");
		try {        
	         System.out.println("Waiting...");
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		printReceive(packet);
	}
	
	public DatagramPacket createRRQPacket(){
		
		byte[] rrq = new byte[100];
		rrq[0] = zero;
		rrq[1] = one;
		
		finishRRQOrWRQ(rrq);
		
		return createSendPacket(rrq);
	}
	
	public DatagramPacket createWRQPacket(){
		
		byte[] wrq = new byte[20];
		wrq[0] = one;
		wrq[1] = zero;
		
		finishRRQOrWRQ(wrq);
		
		return createSendPacket(wrq);
	}
	
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
	
	public static void main(String args[]){
		Client c1 = new Client("test1.txt", "netascii");
		Client c2 = new Client("test2.txt", "octet");
		c1.sendAndReceive();
		c2.sendAndReceive();
	}
}

