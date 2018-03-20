package iteration3;
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
			
			count = 0;
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
	private void receiveAndSend() {
		if(type.name().equals("NORMAL_OPERATION")) {
			// buffers for send and receive packets
			byte[] receiveData = new byte[512 + 4];
			byte[] sendData = new byte[512 + 4];
			
			// status flag
			boolean transferring = true;
			
			// port number
			// 69 for RQ, 23 for DATA and ACK
			int port = 69;
			
			// repeat forever
			while(true) {
				// CLIENT TO SERVER
				// receive packet from client
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				receivePack(receiveSocket, receivePacket); // receive packets at port 23
				
				// Check if finished transferring
				if (receivePacket.getData()[1] == 3 && receivePacket.getData()[4] == 0) {
					transferring = false;
				}
				
				// send packet from client to server
				// first packet (the request) should be sent to port 69
				try {
					sendReceivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(), port);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
				sendPack(sendReceiveSocket, sendReceivePacket);
				printSend(sendReceivePacket);
				
				// SERVER TO CLIENT
				if (transferring) {
		
					sendReceivePacket = new DatagramPacket(sendData, sendData.length);
					receivePack(receiveSocket, sendReceivePacket); 
					
					port = sendReceivePacket.getPort();
					
					sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
					sendPack(sendReceiveSocket, sendPacket);	
				}
			}	
		} else if (type.name().equals("LOSE_PACKET")) {
			findPacket();
		} else if (type.name().equals("DELAY_PACKET")) {
			findPacket();
		} else if (type.name().equals("DUPLICATE_PACKET")) {
			/* FIX THIS */
			simulateDuplicatePacket(simulatorPacket.getData(), sendReceiveSocket);
			type = ErrorType.getErrorType(0);
			receiveAndSend();
		}
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
		System.out.println("ErrorSim: Delaying packet...");
		try {
			TimeUnit.SECONDS.sleep(delay);
			System.out.println("Finished");
		} catch (InterruptedException ie) {
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
					
		// port number
		// 69 for RQ, 23 for DATA and ACK
		int port = 69;
		int oldPort;
					
		// repeat forever
		while(true){
			// CLIENT TO SERVER
			// receive packet from client
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
			receivePack(receiveSocket, receivePacket);
						
			// check if finished sending and receiving between client and server
			if (receivePacket.getData()[1] == 3 && receivePacket.getData()[515] == 0) {
				stop = true;
			}
			
			if(receivePacket.getData()[1] == THREE) {
				count++;
				if(packet.name().equals("DATA") && count == packetNumber) simulatorPacket = receivePacket;
			}
			
			if(receivePacket.getData()[1] == FOUR && packet.name().equals("ACK")) {
				if(count == packetNumber) simulatorPacket = receivePacket;
			}
			
			if(checkError(receivePacket)){
				if(type.name().equals("LOSE_PACKET")){
					receivePacket = new DatagramPacket(receiveData, receiveData.length);
					receivePack(receiveSocket, receivePacket);
				}
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
				receivePack(receiveSocket, sendReceivePacket);
				
				if(sendReceivePacket.getData()[1] == THREE) {
					count++;
					if(packet.name().equals("DATA") && count == packetNumber) simulatorPacket = receivePacket;
				}
				
				if(sendReceivePacket.getData()[1] == FOUR && packet.name().equals("ACK")) {
					if(count == packetNumber) simulatorPacket = receivePacket;
				}
					
				// this should change the port to the thread port
				oldPort = port;
				port = sendReceivePacket.getPort();

			
				if(checkError(sendReceivePacket)){
					if(type.name().equals("LOSE_PACKET")){
						try {
							sendReceivePacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), oldPort);
							receivePack(receiveSocket, sendReceivePacket);
						} catch (UnknownHostException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}
				}
							
				// send the packet
				sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
				sendPack(sendReceiveSocket, sendPacket);
				}
			}	
	}
	

	private boolean checkError(DatagramPacket pack) {
		
		if(pack.getData()[1] == THREE || pack.getData()[1] == FOUR) {
			if(count == packetNumber) {
				if(packet.name().equals("DATA") && pack.getData()[1] == THREE) {
					simulateError();
					return true;
				}
				if(packet.name().equals("ACK") && pack.getData()[1] == FOUR) {
					simulateError();
					return true;
				}
			}
		}return false;
	}
	

	/** A method used to check if an error is being simulated
	 * @return	true if an error has been simulated, false otherwise
	 */
	private void simulateError() {
		switch(type.name()) {
			case "LOSE_PACKET":
				System.out.println("ErrorSim: Dropping packet...");
				break;
			case "DUPLICATE_PACKET":
				simulatorPacket = receivePacket;
				break;
			case "DELAY_PACKET": 
				simulatorPacket = receivePacket;
				simulateDelayPacket();
				break;
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
			delay = sc.nextInt();
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
