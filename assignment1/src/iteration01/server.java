package iteration01;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class server {
	protected DatagramSocket sendSocket, receiveSocket;
	protected DatagramPacket sendPacket, receivePacket;
	
	private final byte zero = 0;
	private final byte one = 1;
	private final byte two = 2;
	
	protected int serverPort = 69;
	
	public server() { 
		try {
			// Create a DatagramSocket to use to receive (port 69)
			receiveSocket = new DatagramSocket(serverPort);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	private void loop() {
		byte[] data = new byte[100];
		boolean valid;
		byte[] response = new byte[100];
		
		while(true) {
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
			valid = checkPacket(data, data.length);
			if (!valid) {
				System.out.println("Invalid request detected. Server will terminate.");
				throw new IllegalArgumentException("Invalid request exception");
			}
			
			printInfo(receivePacket);
			
			if (data[1] == one) {
				response = new byte[] {0,3,0,1};
			} else if (data[1] == two) {
				response = new byte[] {0,4,0,0};
			}
			
			sendPacket = new DatagramPacket(response, response.length, receivePacket.getAddress(), receivePacket.getPort());
			
			try {
				sendSocket = new DatagramSocket();
				sendSocket.send(sendPacket);
				sendSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Server: Sending response: " + response);
			System.out.println("Containing: ");
			for (byte b : response) {
				System.out.print(b);
			}
			System.out.println();
		}
	}
	
	// Checks if data received is valid format/contents
	private boolean checkPacket(byte[] data, int len) {
		String mode = "";
		String filename = "";
		int i;
		// First zero
		if (data[0] != zero) {
			return false;
		}
		// Request byte
		if (data[1] != one && data[1] != two) {
			return false;
		}
		i = 2;
		// Valid filename
		while (data[i] != zero && i < len - 1) {
			filename += (char)data[i++];
		}
		if (filename == "") {
			return false;
		}
		// Middle zero
		if (data[i++] != zero) {
			return false;
		}
		// Valid mode
		while(data[i] != zero && i < len - 1) {
			mode += (char)data[i++];
		}
		mode = mode.toLowerCase();
		if (!mode.equals("netascii") && !mode.equals("octet")) {
			return false;
		}
		if (data[i++] != zero) {
			return false;
		}
		// EOF zero
		while (i < len) {
			if (data[i++] != zero) {
				return false;
			}
		}
		return true;
	}
	
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
		server s = new server();
		s.loop();
	}
}
