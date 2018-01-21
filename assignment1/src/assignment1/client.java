package assignment1;
import java.net.*;
import java.util.Arrays;
import java.io.*;

/*
 * A majority of this class is composed of code from the
 * SimpleEchoClient java class studied in class
 */

public class client {
	protected DatagramSocket sendReceiveSocket;
	protected DatagramPacket sendPacket, receivePacket;

	protected int port = 23;

	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	
	private final String filename = "test.txt"; 
	private final String modename = "netascii";
	
	public client() {
		try {
			// Construct a datagram socket and bind it to any available
			// port and on the local host machine. This socket will be used to 
			// send and receive UDP Datagram packets.

			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void sendAndReceive() {
		int len;
		String received;
		byte[] msg, data;
		
		for (int i = 1; i < 12; i++) {
			msg = getByteArray(i);
			try {
				// Construct a datagram packet to be sent
				sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), port);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// print details about the packet being sent to the console
			printDetailsSend(sendPacket);
			
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Client: Packet sent.\n");
			data = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			
			// Send the datagram packet to the server via the send/receive socket
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			printDetailsReceive(receivePacket);
			len = receivePacket.getLength();
			received = new String(data,0,len);
			System.out.println(received);
		}
		sendReceiveSocket.close();
	}

	// Create byte array for read/write requests
	private byte[] getByteArray(int index) {
		byte[] file = filename.getBytes();
		byte[] mode = modename.getBytes();
		byte[] byteArray = new byte[4 + file.length + mode.length];
		int i;
		
		// Leading zero
		byteArray[0] = zero;
		
		if (index % 2 == 0) {
			// Read request
			byteArray[1] = one;
		} else if (index == 11) {
			// Invalid request
			byteArray[0] = two;
		} else {
			// Write request
			byteArray[1] = two;
		}
		
		for(i = 0; i < file.length; i++) {
			byteArray[2 + i] = file[i];
		}
		
		byteArray[3 + file.length] = zero;
		
		for(i = 0; i < mode.length; i++) {
			byteArray[3 + file.length + i] = mode[i];
		}
		byteArray[3 + file.length + mode.length] = zero;
		return byteArray;
	}
	
	// Print the details of the packet being sent to the console
	private void printDetailsSend(DatagramPacket dp) {
		System.out.println("Client: Sending packet...");
		System.out.println("To host: " + dp.getAddress());
		System.out.println("Destination host port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print the details of the packet being received to the console
	private void printDetailsReceive(DatagramPacket dp) {
		System.out.println("Client: Packet received.");
		System.out.println("From host: " + dp.getAddress());
		System.out.println("Host port: " + dp.getPort());
		printInfo(dp);
	}
	
	// Print the details about the length and contents of the packet
	private void printInfo(DatagramPacket dp) {
		int len = dp.getLength();
		byte[] data = dp.getData();
		System.out.println("Length: " + len);
		System.out.print("Containing: (bytes) ");
		System.out.println(Arrays.toString(dp.getData()));
		String packet = new String(dp.getData(), 0, len);
		String request = data[1] == one ? "Read":"Write";
		System.out.println("(type): " + request + " (filename and mode): " + packet);
	}

	public static void main(String[] args) {
		client c = new client();
		c.sendAndReceive();
	}
}
