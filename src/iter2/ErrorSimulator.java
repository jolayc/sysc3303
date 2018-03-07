package iter2;
import java.awt.Window.Type;
/**
 * Error Simulator of a client/server TFTP application
 * The Error Simulator receives a request packet from a client and then
 * sends it to the server
 * It then receives from the server a reply packet back to the original client
 */
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class ErrorSimulator {
	
	private final byte ZERO = 0;
	private final byte ONE = 1;
	private final byte TWO = 2;
	private final byte THREE = 3;
	private final byte FOUR = 4;
	
	private DatagramSocket receiveSocket, sendReceiveSocket;
	private DatagramPacket receivePacket, sendReceivePacket, sendPacket;
	private DatagramPacket simulatorPacket;
	private static ErrorType type;
	private static PacketType packet;
	private static int packetNumber;
	private static int delay;
	private static int duplicateOffset;
	
	/**
	 * Constructor for host
	 * creates a Datagram Socket to receive packets from client on port 23.
	 * creates Datagram Socket to send and receive packets from server on any avalible port
	 */
	public ErrorSimulator(){
		try {
			//Constructs a socket to receive packets bounded to port 23
			receiveSocket = new DatagramSocket(23);
			
			//Constructs a socket to send packets from any available port
			sendReceiveSocket = new DatagramSocket();
			
			//Set timeout
			//sendReceiveSocket.setSoTimeout(5000);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Receives a request packet from the client
	 * Sends the request to the server
	 * Receives a response packet from the server
	 * Sends the response to the client 
	 */
	public void receiveAndSend(){
		int count = 1;
		//will repeat "forever"
		while(true){
			boolean stop = false;
			//buffer for the receive packet
			byte receiveData[] = new byte[4 + 512];
			
			//buffer for the response packet to be sent to the intermediatehost
			byte sendData[] = new byte[4 + 512];
		    
			//constructs a datagram packet to receive packets 20 bytes long
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    
		    //waits until receiveSocket receives a datagram packet from the client
		    receivePack(receiveSocket, receivePacket);
		
		    //if user requested error for data or ack
		    if(receivePacket.getData()[1] == THREE && receivePacket.getData()[1] == FOUR) {
		    	if(count == packetNumber) {
		    		if(packet.name().equals("DATA") && receivePacket.getData()[1] == THREE) simulatorPacket = receivePacket;
		    		if(packet.name().equals("ACK") && receivePacket.getData()[1] == FOUR) simulatorPacket = receivePacket;
		    	}
		    	else count++;
		    }
		  
		    if(receivePacket.getData()[1] == THREE && receivePacket.getData()[515] == ZERO){
		    	break; //might need to fix this
		    }
		    
		    //if the user requested error for rrq
		    if(receivePacket.getData()[1] == ONE && packet.name().equals("RRQ")) {
		    	simulatorPacket = receivePacket;
		    }
		    
		    //if the user requested error for wrq
		    if(receivePacket.getData()[1] == TWO && packet.name().equals("WRQ")) {
		    	simulatorPacket = receivePacket;
		    }
		    
		    //creates packet to send to server from the packet sent from client
		    try {
				sendReceivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(), 69);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
		    
		    //sends the sendReceivePacket to the intermediate host
		    sendPack(sendReceiveSocket, sendReceivePacket);
		    printSend(sendReceivePacket);
		    
		    if(!simulateError()) {
		    	//creates a datagram packet that will contain sendData that will be ported to port 69
		    	try {
		    		sendReceivePacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), 69);
		    	} catch (UnknownHostException e1) {
		    		e1.printStackTrace();
		    		System.exit(1);
		    	}
		    	
		    	//waits until sendReceivePacket receives a datagram packet from the server
		    	receivePack(sendReceiveSocket, sendReceivePacket);	
		    	
		    	//if the user requested error for data
			    if(receivePacket.getData()[1] == THREE && packet.name().equals("DATA")) {
			    	if(count == packetNumber) {
			    		simulatorPacket = receivePacket;
			    	}
			    }
			    
			    //if the user requested error for ack
			    if(receivePacket.getData()[1] == FOUR && packet.name().equals("ACK")) {
			    	if(count == packetNumber) {
			    		simulatorPacket = receivePacket;
			    	}
			    }
		    
		    	//creates a datagram packet that will be ported to wherever receivePacket is ported to
		    	sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
		    	
		    	//sends sendPacket to the client
		    	sendPack(sendReceiveSocket, sendPacket);	
		    }
		}
	}
	
	/**
	 * A method used to check if an error is being simulated
	 * @return	true if an error has been simulated, false otherwise
	 */
	private boolean simulateError() {
		if(type.name().equals("LOSE_PACKET")) {
			return simulateLosePacket();
		} else if(type.name().equals("DUPLICATE_PACKET")) {
			return simulateDuplicatePacket();
		} else if(type.name().equals("DELAY_PACKET")) {
			return simulateDelayPacket();
		} else { // normal operation
			return false;
		}
	}
	
	/**
	 * A method that simulates a packet loss,
	 * this method checks the packets received
	 * and throws away the requested packet
	 */
	private boolean simulateLosePacket() {
		System.out.println("ErrorSim: Dropping packet...");
		//Don't send the packet/Do nothing
		return true;
	}
	
	/**
	 * A method that simulates a packet delay,
	 * this method delays sending a packet by
	 * a specified amount of time
	 */
	private boolean simulateDelayPacket() {
		System.out.println("ErrorSim: Delaying packet...");
		try {
			TimeUnit.SECONDS.sleep(delay);
			sendReceiveSocket.send(simulatorPacket);
		} catch (InterruptedException ie) {
			// sleep interrupted
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return true;
	}
	
	/**
	 * A method that simulates a duplicate packet,
	 * this method sends a duplicate packet depending
	 * on the specified space
	 */
	private boolean simulateDuplicatePacket() {
		System.out.println("ErrorSim: Duplicating packet...");
		// TO-DO
		return true;
	}
	
	/**
	 * Sends a packet to a socket
	 * @param socket, DatagramSocket where the packet will be sent
	 * @param packet, DatagramPacket that will be sent
	 */
	public void sendPack(DatagramSocket socket, DatagramPacket packet) {
	
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
	public void receivePack(DatagramSocket socket, DatagramPacket packet) {
		
		System.out.println("Host: Waiting for Packet.\n");
		try {        
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		
		printReceive(packet);
	}
	
	/**
	 * Prints information relating to a send request
	 * @param packet, DatagramPacket that is used in the send request
	 */
	private void printSend(DatagramPacket packet){
		System.out.println("Host: Sending packet");
	    System.out.println("To host: " + packet.getAddress());
	    System.out.println("Destination host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("Host: Packet received");
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
		ErrorSimulator sim = new ErrorSimulator();
		Scanner sc = new Scanner(System.in);
		boolean validType = false;
		boolean validPacket = false;
		
		while(!validType) {
			System.out.println("ErrorSim: Enter 0 for normal operation, 1 for lose a packet, 2 for delay a packet and 3 for duplicate a packet.");
			while(!sc.hasNextInt()) sc.next();
			int errorType = sc.nextInt();
			if(errorType >= 0 && errorType <= 3) {//if errorType is a valid ordinal for PacketType
				type = ErrorType.getErrorType(errorType);
				validType = true;
			}
			else System.out.println("ErrorSim: Invalid type entered");
		}
		while(!validPacket) {
			System.out.println("ErrorSim: Enter 0 for RRQ, 1 for WRQ, 2 for DATA, 3 for ACK");
			while(!sc.hasNextInt()) sc.next();
			
			int packetType = sc.nextInt();
			if(packetType >= 0 && packetType <= 3) {//if packetType is a valid ordinal for PacketType
				packet = PacketType.getPacketType(packetType);
				validPacket = true;
			}
			else System.out.println("ErrorSim: Invalid request entered");
		}
		
		
		if(packet.ordinal() == 2 || packet.ordinal() == 3) {//to determine the nth. DATA or ACK packet
			if(packet.ordinal() == 2 ) System.out.println("ErrorSim: Enter the DATA packet that will be affected: ");
			else System.out.println("ErrorSim: Enter the ACK packet that will be affected: ");
			
			boolean positive = false;
			while(!positive) {
				while(!sc.hasNextInt()) sc.next();
				if(sc.nextInt() > 0) {
					packetNumber = sc.nextInt();
					positive = true;
				}
				else System.out.println("ErrorSim: Number must be greater than 0");
			}
		}
		
		if(type.ordinal() == 2) {//for delay packet
			System.out.println("ErrorSim: Enter the delay, in seconds: ");
			while(!sc.hasNextInt()) sc.next();
			delay = 1000 * sc.nextInt();
		}
		
		if(type.ordinal() == 3) {//for duplicate packet
			System.out.println("ErrorSim: Enter the duplicate offsets: ");
			while(!sc.hasNextInt()) sc.next();
			duplicateOffset = sc.nextInt();
		}
		System.out.println("ErrorSim: Running...");
		sim.receiveAndSend();
		sc.close();
	}
	
	public enum ErrorType {
		
		NORMAL_OPERATION(0),
		LOSE_PACKET(1),
		DELAY_PACKET(2),
		DUPLICATE_PACKET(3);
		private int type;
		
		ErrorType(int type){
			this.type = type;
		}
		
		static ErrorType getErrorType(int type) throws IllegalArgumentException {
			if(type == 0) return NORMAL_OPERATION;
			if(type == 1) return LOSE_PACKET;
			if(type == 2) return DELAY_PACKET;
			if(type == 3) return DUPLICATE_PACKET;
			else throw new IllegalArgumentException("Invalid type");
		}
	}
	
	public enum PacketType {
		
		RRQ(0),
		WRQ(1),
		DATA(2),
		ACK(3);
		private int packet;
		
		PacketType(int packet){
			this.packet = packet;
		}
		
		static PacketType getPacketType(int packet) throws IllegalArgumentException {
			if(packet == 0) return RRQ;
			if(packet == 1) return WRQ;
			if(packet == 2) return DATA;
			if(packet == 3) return ACK;
			else throw new IllegalArgumentException("Invalid request");
		}
	}

}
