package assignment1;
import java.net.*;
import java.io.*;

public class host {
	protected DatagramSocket receiveSocket, sendReceiveSocket, sendSocket;
	protected DatagramPacket sendPacket, receivePacket;
	
	public host() {
		try {
			sendReceiveSocket = new DatagramSocket();
			receiveSocket = new DatagramSocket(23);
			sendSocket = new DatagramSocket();
		} catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	public void loop() {
		byte[] data;
		
		while(true) {
			data = new byte[100];
			receivePacket = new DatagramPacket(data, data.length);
			
			// Attempt to retrieve data from socket
			try {
				System.out.println("Waiting to receive...");
				receiveSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("Receive socket timed out. \n" + e);
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	public static void main(String[] args) {
		host h = new host();
		h.loop();
	}

}
