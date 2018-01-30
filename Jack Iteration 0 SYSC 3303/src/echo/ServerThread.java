package echo;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class ServerThread implements Runnable {
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte three = 0x03;
	private final byte four = 0x04;

	private boolean read;  //true if request send is a read request
	private boolean write; //true if request send is a write request
	

	private DatagramSocket receiveSocket, sendSocket;
	private DatagramPacket receivePacket, sendPacket;
	
	/**
	 * Constructor for ServerThread
	 * @param socket, DatagramSocket where data will be received
	 */
	public ServerThread(DatagramSocket socket){
		this.receiveSocket = socket;
		this.read = false;
		this.write = false;
	}

	/**
	 * Runs the thread
	 */
	public void run() {
		
		byte response[] = new byte[4];

	
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
	
	/**
	 * Receives a packet from a socket
	 * @param socket, DatagramSocket where the packet data will be received from
	 * @param packet, DatagramPacket where the data from the socket will be stored
	 */
	public void receivePack(DatagramSocket socket, DatagramPacket packet) {
		
		try {        
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		printReceive(packet);
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
	 * Creates a data packet
	 * @return the data packet that was created
	 */
	public byte[] createDataPacket(){
		
		byte[] data = new byte[4];
		
		data[0] = zero;
		data[1] = three;
		data[2] = zero;
		data[3] = one;
		return data;
	}
	
	/**
	 * Creates an acknowledgment packet
	 * @return the acknowledgement packet that was created
	 */
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
}
