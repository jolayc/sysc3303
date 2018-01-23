
/**
 * EchoServer
 * Server side of a simple echo server.
 * The server receives from a client a request packet sent from the intermediate host
 * It then sends a reply packet back to the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class EchoServer {
	
	//Variables used for parsing the byte array
	private int zeroes; 
	private boolean read;  //true if request send is a read request
	private boolean write; //true if request send is a write request
	private boolean file; //true if the server  is reading/has read the filename 
	private boolean mode ; //true if the server  is reading/has read the mode
	
	
	private DatagramSocket receiveSocket, sendSocket;
	private DatagramPacket receivePacket, sendPacket;
	
	public EchoServer(){
		
		try{
			//Constructs a socket to receive packets bounded to port 69
			receiveSocket = new DatagramSocket(69);
		}
		catch(SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Initializes the parsing variables
	 */
	public void init(){
	    zeroes = 0;
		read = false;
		write = false;
		file = false;
		mode = false;	
	}
	
	/**
	 * Receives a request packet from the intermediate host
	 * Sends a response packet back to the intermediate host 
	 */
	public void receiveAndSend(){
		
		//will repeat "forever"
		while(true){
			
			//constructs a datagram packet to receive packets 20 bytes long
			byte data[] = new byte[20];	
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Server: Waiting for Packet.\n");
			
			//wait until receiveSocket receives a datagram packet from the intermediate host
			try {        
		         System.out.println("Waiting...");
		         receiveSocket.receive(receivePacket);
		    } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		    }
			
			printReceive(receivePacket);
			 
			//parse the received packet to see if it's valid
			try {
				checkReadWrite(receivePacket.getData());
			} catch (Exception e) {
				System.out.println("Invalid Packet");
				System.exit(1);
			}
			 
			//constructs a socket to send packets from any available port
			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			
			byte sendBytes[] = createSendBytes(); //contains a response to the sent request
			 
			//creates a datagram packet that will contain sendBytes that will be ported to the same port as receivePacket
			try {
				sendPacket = new DatagramPacket(sendBytes, sendBytes.length, InetAddress.getLocalHost(), receivePacket.getPort());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			 
			 printSend(sendPacket);
			 
			 //sends the packet containing sendBytes to the intermediate host
			 try{
				 sendSocket.send(sendPacket);
			 }
			 catch(IOException io){
				 io.printStackTrace();
				 System.exit(1);
			 }
			 sendSocket.close();
		}
	}
	/**
	 * Parses a byte array and checks if is a read request or a write request
	 * @param data, byte[] that contains the request
	 * @throws Exception if the request is invalid
	 */
	public void checkReadWrite(byte[] data) throws Exception{
		
		init();
		for(int i = 0; i < data.length; i++){
			
			if(data[i] == 0x00){
				zeroes++;
				if(zeroes > 3) throw new Exception("Invalid Packet");
			}
			else if(data[i] == 0x01){
				if(zeroes == 1 && (!read && !write)) read = true;
				else throw new Exception("Invalid Packet");
			}
			else if(data[i] == 0x02){
				if(zeroes == 1 && (!read && !write)) write = true;
				else throw new Exception("Invalid Packet");
			}
			else if(!file){
				if(zeroes == 1 && (read^write)) file = true;
				else if(!file && zeroes > 1) throw new Exception("Inavlid Packet");
			}
			else if(!mode){
				if(zeroes == 2 && file) mode = true;
				else if(!mode && zeroes > 2) throw new Exception("Inavlid Packet");
			}
		}
		if(zeroes != 3) throw new Exception("Invalid Packet");
	}
	
	/**
	 * Creates a packet of four bytes to be later sent to the intermediate host
	 * @return byte[] that contains the packet of four bytes
	 */
	public byte[] createSendBytes(){
		
		byte[] sendBytes = new byte[4];
		
		//bytes are the same for both read and write requests
		sendBytes[0] = 0x00;
		sendBytes[2] = 0x00;
		
		//if a read request
		if(read){
			sendBytes[1] = 0x03;
			sendBytes[3] = 0x01;
		}
		
		//if a write request
		else if(write){
			sendBytes[1] = 0x04;		
			sendBytes[3] = 0x00;
		}	
		return sendBytes;
	}
	
	/**
	 * Prints information relating to a send request
	 * @param packet, DatagranPacket that is used in the send request
	 */
	private void printSend(DatagramPacket packet){
		System.out.println( "Server: Sending packet");
	    System.out.println("To host: " + packet.getAddress());
	    System.out.println("Destination host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("Server: Packet received");
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
	    System.out.print("Containing: " );
	    
	    //prints the bytes of the packet
	    System.out.println(Arrays.toString(packet.getData()));
	    
	    //prints the packet as text
	    String received = new String(packet.getData(),0,len);   
	    System.out.println(received + "\n");
	}
	
	public static void main( String args[] ){
		EchoServer s = new EchoServer();
	    s.receiveAndSend();
	}
}
