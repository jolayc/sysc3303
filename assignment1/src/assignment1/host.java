package assignment1;
import java.net.*;
import java.io.*;

public class host {
	private DatagramSocket receiveSocket;
	private DatagramSocket sendReceiveSocket;
	
	public host() {
		try {
			receiveSocket = new DatagramSocket();
			//sendReceiveSocket
		} catch (SocketException se){
			
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
