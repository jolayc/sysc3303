package assignment1;
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
	
	public static void main(String[] args) {
		
	}
}
