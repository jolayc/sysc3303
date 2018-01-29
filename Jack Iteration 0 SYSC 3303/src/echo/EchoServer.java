package echo;

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
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte three = 0x03;
	private final byte four = 0x04;
	
	//Variables used for parsing the byte array
	private boolean read;  //true if request send is a read request
	private boolean write; //true if request send is a write request
	
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
		init();
	}
	
	protected void init(){
		this.read = false;
		this.write = false;
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
			byte response[] = new byte[4];
			
			receivePacket = new DatagramPacket(data, data.length);
			
			receivePack(receiveSocket, receivePacket);
			
			checkReadWrite(receivePacket.getData());
			
			if(read) response = createDataPacket();
			else if(write) response = createACKPacket();
			 
			//constructs a socket to send packets from any available port
			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			 
			//creates a datagram packet that will contain sendBytes that will be ported to the same port as receivePacket
			try {
				sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receivePacket.getPort());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			 
			 sendPack(sendSocket, sendPacket);
			 
			 sendSocket.close();
		}
	}
	
	public void receivePack(DatagramSocket socket, DatagramPacket packet) {
		
		System.out.println("Server: Waiting for Packet.\n");
		try {        
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		printReceive(packet);
	}
	
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
	 * Parses a byte array and checks if is a read request or a write request
	 * @param data, byte[] that contains the request
	 */
	public void checkReadWrite(byte[] data){
		
		if(data[1] == one) read = true;
		else if(data[1] == zero) write = false;
	}
	
	public byte[] createDataPacket(){
		
		byte[] data = new byte[4];
		
		data[0] = zero;
		data[1] = three;
		data[2] = zero;
		data[3] = one;
		return data;
	}
	
	public byte[] createACKPacket(){
		
		byte[] ack = new byte[4];
		
		ack[0] = zero;
		ack[1] = four;
		ack[2] = zero;
		ack[3] = zero;
		return ack;
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
	
	private byte[] toBytes(byte[] b) {
		//TO-DO: return file to be sent as byte array	
		return null;
	}
	
	private void shutdown() {
		sendReceiveSocket.close();
		//System.exit(1); // note: not sure if this will be needed
	}
	public static void main( String args[] ){
		EchoServer s = new EchoServer();
	    s.receiveAndSend();
	}
}
