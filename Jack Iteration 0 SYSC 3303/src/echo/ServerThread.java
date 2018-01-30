package echo;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class ServerThread implements Runnable{
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte three = 0x03;
	private final byte four = 0x04;

	private final String read = "READ";
	private final String write = "WRITE";
	
	private String message;
	private byte response[] = null;
	
	private DatagramSocket sendSocket;
	private DatagramPacket receivePacket, sendPacket;
	
	
	
	/**
	 * Constructor for ServerThread
	 * @param socket, DatagramSocket where data will be received
	 */
	public ServerThread(DatagramPacket receivePacket, String message){
		this.message = message;
		this.receivePacket = receivePacket;
	}

	/**
	 * Runs the thread
	 */
	public void run() {
	
		if(message.equals(read)) response = createDataPacket();
		else if(message.equals(write)) response = createACKPacket();
	 
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
