package iteration4;
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
	private int count;
	
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
	private void receiveAndSend() {
		if(type.name().equals("NORMAL_OPERATION")) {
			// buffers for send and receive packets
			byte[] receiveData = new byte[512 + 4];
			byte[] sendData = new byte[512 + 4];
			
			// status flag
			boolean transfering = true;
			
			// port number
			// Server is waiting for Requests on port 69
			int port = 69;
			
			// repeat forever
			while(true) {
				// CLIENT TO SERVER
				// Create and Receive datagram packet from Client
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				receivePack(receiveSocket, receivePacket); // PACKET RECEIVED FROM CLIENT
				if (receivePacket.getData()[1] == 3 && receivePacket.getData()[515] == 0) {
					transfering = false;
					port = 69;
				}
				
				// ERROR SIM TO SERVER
				// Request packet sent to port 69
				// Create and Send datagram packet to Server
				try {
					sendReceivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(), port); 
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
				sendPack(sendReceiveSocket, sendReceivePacket);
				printSend(sendReceivePacket);
				
				// Reassign port number after request is sent
				// Port number is Client's port number
				port = receivePacket.getPort();
				
				// SERVER TO CLIENT
				if (transfering) {
					// Create and receive packet from Server
					try {
						sendReceivePacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), port);
					} catch (UnknownHostException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
					receivePack(sendReceiveSocket, sendReceivePacket);
					
					// Send received packet back to Client
					sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
					sendPack(sendReceiveSocket, sendPacket);
					printSend(sendPacket);
				}
			}
		} else if (type.name().equals("LOSE_PACKET")) {
			simulateLosePacket();
			type = ErrorType.getErrorType(0); // after done simulating error, swap type to normal operation and rerun receiveAndSend
			receiveAndSend();
		} else if (type.name().equals("DELAY_PACKET")) {
			simulateDelayPacket();
			type = ErrorType.getErrorType(0);
			receiveAndSend();
		} else if (type.name().equals("DUPLICATE_PACKET")) {
			/* FIX THIS */
			simulateDuplicatePacket(simulatorPacket.getData(), sendReceiveSocket);
			type = ErrorType.getErrorType(0);
			receiveAndSend();
		}
	}
	
	
	/*
	 * A method that throws away the packet sent
	 */
	private void simulateLosePacket() {
		findPacket();
		System.out.println("ErrorSim: Dropping packet...");
		// don't send the packet/do nothing to force a timeout
	} 
	
	private void simulateDuplicatePacket(byte[] data, DatagramSocket socket) {
		findPacket();
		DatagramPacket duplicate = simulatorPacket;
		
		packetNumber = duplicateOffset - packetNumber;
		findPacket();
		
		if(data[1] == THREE && packet.name().equals("DATA")) {
			System.out.println("ErrorSim: Sending a duplicate packet...");
			try {
				socket.send(duplicate);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		if(data[1] == FOUR && packet.name().equals("ACK")) {
			System.out.println("ErrorSim: Sending a duplicate packet...");
			try {
				socket.send(duplicate);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	private void simulateDelayPacket() {
		findPacket();
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
	}
	
	/**
	 * Find packet to simulate error and save it in simulatorPacket
	 */
	private void findPacket() {
		// buffers for send and receive packets
		byte[] receiveData = new byte[512 + 4];
		byte[] sendData = new byte[512 + 4];
					
		// status flag
		boolean stop = false;
		boolean firstSend = true;
					
		// port number
		// 69 for RQ, 23 for DATA and ACK
		int port = 69;
		int count = 0;
					
		// repeat forever
		while(true) {
			// CLIENT TO SERVER
			// receive packet from client
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
			receivePack(receiveSocket, receivePacket);
						
			// check if finished sending and receiving between client and server
			if (receivePacket.getData()[1] == 3 && receivePacket.getData()[515] == 0) {
				stop = true;
				port = 69;
			}
				
			if(!firstSend) count++;
			firstSend = false;
			
			if(receivePacket.getData()[1] == THREE && packet.name().equals("DATA")) {
				if(count == packetNumber) simulatorPacket = receivePacket;
			}
			
			if(receivePacket.getData()[1] == FOUR && packet.name().equals("ACK")) {
				if(count == packetNumber) simulatorPacket = receivePacket;
			}
			
			// send receive packet from client to server
			// the first packet (which should be a RQ) should be sent to port 69
			try {
				sendReceivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(), port);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
						
			// sends the sendReceivePacket to the server
			sendPack(sendReceiveSocket, sendReceivePacket);
			printSend(sendReceivePacket);
						
			// SERVER TO CLIENT
			if (!stop) {
				try {
					sendReceivePacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), port);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
							
				// waits until sendReceivePacket receives a packet from the server
				receivePack(sendReceiveSocket, sendReceivePacket);
				count++;
				
				if(receivePacket.getData()[1] == THREE && packet.name().equals("DATA")) {
					if(count == packetNumber) simulatorPacket = receivePacket;
				}
				
				if(receivePacket.getData()[1] == FOUR && packet.name().equals("ACK")) {
					if(count == packetNumber) simulatorPacket = receivePacket;
				}
							
				// this should change the port to 23
				port = receivePacket.getPort();
							
				// send the packet
				sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), port);
				sendPack(sendReceiveSocket, sendPacket);
				}
			}	
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
			if(errorType == 0) {
				type = ErrorType.getErrorType(0);
				validType = true;
				validPacket = true;
				packet = PacketType.getPacketType(0);
			}
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
				packetNumber = sc.nextInt();
				if(packetNumber >= 0) positive = true;
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
