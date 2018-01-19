package assignment1;
import java.io.IOException;
import java.net.*;

public class server {
	protected DatagramSocket sendSocket, receiveSocket;
	protected DatagramPacket sendPacket, receivePacket;
	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	
	public server() { 
		try {
			receiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void loop() {
		byte[] data;
		data = new byte[100];
		
		while(true) {
			boolean readReq = false;
			boolean writeReq = false;
			receivePacket = new DatagramPacket(data, data.length);
			try {
				// Server waits to receive a request
				System.out.println("Waiting for request...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("Receive socket timed out. \n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			
			// Parse through information
			try {
				checkPacket(receivePacket);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// Print out information it has received
			
			// If packet is valid read req, send 0 3 0 1
			
			// If packet is valid write req, send 0 4 0 0
			
			// Prints out the response packet info
			
			// Create DatagramSocket for response
			
			// Sends packet via new socket to the port it received the request from
			
			// Close the socket it just created
			
		}
	}
	
	private void checkPacket(DatagramPacket dp) throws Exception {
		byte[] data = dp.getData();
		int i;
		
		// Check for first zero
		if (data[0] != zero) {
			throw new Exception("First byte is invalid");
		}
		// Check for request type
		if (data[1] != one || data[1] != two) {
			throw new Exception("Request byte is invalid");
		}
		// Check for file contents
		
		// Check for second zero
		
		// Check for mode type
		
		// Check for last zero
		if (data[data.length - 1] != zero) {
			throw new Exception("Packet does not have 0 as last byte or is too long");
		}
	}
	
	private void printInfo(DatagramPacket dp) {
		
	}
		
	public static void main(String[] args) {
		server s = new server();
		s.loop();
	}
}
