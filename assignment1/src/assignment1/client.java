package assignment1;
import java.net.*;
import java.io.*;

/*
 * A majority of this class is composed of code from the
 * SimpleEchoClient java class studied in class
 */

public class client {
	protected DatagramSocket sendReceiveSocket;
	protected DatagramPacket sendPacket, receivePacket;

	protected int port = 23;

	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte two = 0x10;

	public client() {
		try {
			//Construct a datagram socket and bind it to any available
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
		byte[] msg;
		
		try {
			for (int i = 1; i < 12; i++) {
				msg = getByteArray(i);
				sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), port);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Client: Sending packet: ");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		len = sendPacket.getLength();
		System.out.println("Containing: ");
		System.out.println(new String(sendPacket.getData(),0,len));
		//System.out.println(sendPacket.getBytes());
		// Send the datagram packet to the server via the send/receive socket

		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Client: Packet sent.\n");

		// Construct a DatagramPacket for receiving packets up
		// to 100 bytes long

		byte data[] = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);

		try {
			// Block until a datagram is received via sendReceiveSocket
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Process the received datagram
		System.out.println("Client: Packet received:");
		System.out.println("From host: " + receivePacket.getAddress());
		System.out.println("Host port: " + receivePacket.getPort());
		len = receivePacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");

		// Form a String from the byte array
		received = new String(data,0,len);
		System.out.println(received);

		// Close the socket
		sendReceiveSocket.close();
	}

	// Create byte array for read/write requests
	private byte[] getByteArray(int index) {
		byte[] byteArray;
		String filename = "test.txt";
		String mode = "netascii";
		String s = "";
		
		if (index % 2 == 0) {
			// Read request
			s += zero;
			s += one;
			s += filename;
			s += zero;
			s += mode;
			s += zero;
		} else if (index == 11) {
			// Invalid request
			s = "ERROR";
		} else {
			// Write request
			s += zero;
			s += two;
			s += filename;
			s += zero;
			s += mode;
			s += zero;
		}
		byteArray = s.getBytes();
		return byteArray;
	}

	public static void main(String[] args) {
		client c = new client();
		c.sendAndReceive();
	}
}
