package iteration1;
/**
 * Intermediate Host of a client/server TFTP application
 * The Intermediate Host receives a request packet from a client and then
 * sends it to the server
 * It then receives from the server a reply packet back to the original client
 */
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class host {
	private DatagramSocket receiveSocket, sendReceiveSocket;
	private DatagramPacket receivePacket, sendReceivePacket, sendPacket;
	
	public host() {
		try {
			// Construct a socket bounded to port 23
			receiveSocket = new DatagramSocket(23);
			// Construct a socket to sent packets from any available port
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void receiveAndSend() {
		// Repeat "forever"
		while (true) {
			// buffer for receivePacket
			byte[] receive = new byte[20];
			// buffer for sendPacket
			byte[] send = new byte[4];
			
			// Constructs a datagram packet to receive packets 20 bytes long
			receivePacket = new DatagramPacket(receive, receive.length);
			System.out.println("Intermediate Host: Waiting for packet from Client.\n");
			
			// waits until receiveSocket receives a datagram packet from the client
			try {
				System.out.println("Waiting...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			printReceive(receivePacket);
			
			// sends the sendReceivePacket to the server
			try {
				sendReceiveSocket.send(sendReceivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			printSend(sendReceivePacket);
			
			// creates a datagram packet that will contain send data that will sent to port 69
			try {
				sendReceivePacket = new DatagramPacket(send, send.length, InetAddress.getLocalHost(), 69);
				System.out.println("Intermediate Host: Waiting for packet from Server.\n");
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			
			// waits until sendReceivePacket receives datagram packet from server
			try {
				System.out.println("Waiting...");
				sendReceiveSocket.receive(sendReceivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			printReceive(sendReceivePacket);
			
			// creates a datagram packet that will be sent to the same port as receivePacket
			sendPacket = new DatagramPacket(send, send.length, receivePacket.getAddress(), receivePacket.getPort());
			
			// sends sendPacket to the client
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			printSend(sendPacket);
		}
	}
	
	// Print information relating to send request 
	private void printSend(DatagramPacket dp) {
		System.out.println("Host: Sending packet");
		System.out.println("To host: " + dp.getAddress());
		System.out.println("Destination Port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print information relating to receive request
	private void printReceive(DatagramPacket dp) {
		System.out.println("Host: Packet received");
		System.out.println("From host: " + dp.getAddress());
		System.out.println("Port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print information relating to packet
	private void printInfo(DatagramPacket dp) {
		int len = dp.getLength();
		System.out.println("Length: " + len);
		System.out.println("Containing: ");
		
		// prints the contents of packet as bytes
		System.out.println(Arrays.toString(dp.getData()));
		// prints the contents of packet as a String
		String contents = new String(dp.getData(),0,len);
		System.out.println(contents);
	}
	
	public static void main(String[] args) {
		host h = new host();
		h.receiveAndSend();
	}
}
