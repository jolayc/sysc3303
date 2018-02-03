package iteration1;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;


/**
 * A server thread that will handle read/write requests received by server, sent by client 
 */

public class serverThread extends Thread implements Runnable {
	
	private DatagramSocket sendSocket;
	private DatagramPacket receivePacket, sendPacket;
	
	private int[] blockNumber;
	
	// String identifiers
	private String message;
	private String read = "READ";
	private String write = "WRITE";
	
	private byte[] path;
	
	
	public serverThread(DatagramPacket receivePacket, byte[] path, String message, int[] blockNumber) {
		this.message = message;
		this.receivePacket = receivePacket;
		this.path = path;
		this.blockNumber = blockNumber;
	}
	
	public void run() {
	
		byte response[] = new byte[512+4];
		// create response packet
		// send data packet when receiving a read request
		if (message.equals(read)) {
			response = createDataPacket();
			if(response[5] == 0){
				return;
			}
		}

		// send a acknowledge packet when receiving a write request or data packet
		else if(message.equals(write)) response = createACKPacket();
		
		// Construct a socket to send packets to any available port
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		// create a datagram packet that will contain sendBytes that will be ported to the same
		// port as receivePacket
		try {
			sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receivePacket.getPort());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		sendPack(sendSocket, sendPacket);
		// close socket when done sending the packet and stop thread
		sendSocket.close();
	}
	
	private void sendPack(DatagramSocket sock, DatagramPacket dp) {
		printSend(dp);
		try {
			sock.send(dp);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Create a data packet containing {0,3,0,1}
	 * @return byte[4] data packet
	 */
	public byte[] createDataPacket() {
		byte[] data = new byte[512 + 4];
		data[0] = 0;
		data[1] = 3;
		data[2] = (byte)blockNumber[0];
		data[3] = (byte)blockNumber[1];
		
		int multiplier = 0;
		if(blockNumber[1] > 1) multiplier += blockNumber[1];
		if(blockNumber[0] > 0) multiplier += (10*blockNumber[0]);

		
		for(int i = 0; i < path.length; i++){
			if(path.length <= (512*multiplier+i)) break;
			if((path.length > (512*multiplier+i)) && i < 512){
				path[i] = path[512*multiplier+i];
			}
			
		}
		
		for(int j = 0; j < path.length; j++){
			if(path.length <= (512*multiplier+j)) break;
			if(j == 512) break;
			data[4+j] = path[j];
			
		}
		
		return data;
	}
	
	/**
	 * Create an acknowledge packet containing {0,4,0,0}
	 * @return byte[4] acknowledge packet
	 */
	public byte[] createACKPacket() {
		byte[] data = new byte[4];
		data[0] = 0;
		data[1] = 4;
		data[2] = (byte)blockNumber[0];
		data[3] = (byte)blockNumber[1];
		
		return data;
	}
	
	// Print information relating to send request 
	private void printSend(DatagramPacket dp) {
		System.out.println("Server: Sending packet");
		System.out.println("To host: " + dp.getAddress());
		System.out.println("Destination Port: " + dp.getPort());
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
}
