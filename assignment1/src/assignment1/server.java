package assignment1;
import java.io.IOException;
import java.net.*;

public class server {
	protected DatagramSocket sendSocket, receiveSocket;
	protected DatagramPacket sendPacket, receivePacket;
	
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
			receivePacket = new DatagramPacket(data, data.length);
			try {
				System.out.println("Waiting for request...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("Receive socket timed out. \n" + e);
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
		
	public static void main(String[] args) {
		server s = new server();
		s.loop();
	}
}
