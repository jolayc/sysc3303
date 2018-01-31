package echo;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Server
 * Server side of a simple echo server.
 * The server receives from a client a request packet sent from the intermediate host
 * It then sends a reply packet back to the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

public class Server implements Runnable{
	private final byte zero = 0x00;
	private final byte one = 0x01;

	private boolean read;  //true if request send is a read request
	private boolean write; //true if request send is a write request
	private boolean data;
	private boolean ack;
	
	private final int port = 69;
	
	private final String readMessage = "READ";
	private final String writeMessage = "WRITE";
	
	private boolean running;	

	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket, sendPacket;
	
	
	public Server(){
		running = true;
		try {
			//Constructs a socket to receive packets bounded to port 69
			receiveSocket = new DatagramSocket(port);
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Runs the server
	 */
	public void run() {
		
			System.out.println("Server: print exit to exit");
	
			while(isRunning()){

				ServerThread t;
				byte data[] = new byte[20];					
				
				receivePacket = new DatagramPacket(data, data.length);
				receivePack(receiveSocket, receivePacket);
				checkReadWrite(receivePacket.getData());
				
				if(read){
					new Thread(new ServerThread(receivePacket, readMessage)).start();
				}
				
				else if(write){
					new Thread(new ServerThread(receivePacket, writeMessage)).start();
				}
				
			}
			shutdown();
	}
	
	/**
	 * Receives a packet from a socket
	 * @param socket, DatagramSocket where the packet data will be received from
	 * @param packet, DatagramPacket where the data from the socket will be stored
	 */
	public void receivePack(DatagramSocket socket, DatagramPacket packet) {
		
		try {        
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		printReceive(packet);
	}
	
	/**
	 * Sends a packet to a socket
	 * @param socket, DatagramSocket where the packet will be sent
	 * @param packet, DatagramPacket that will be sent
	 */
	public void sendPack(DatagramSocket socket, DatagramPacket packet) {
		
		printSend(sendPacket);
		try{
			 socket.send(packet);
		 }
		 catch(IOException io){
			 io.printStackTrace();
			 System.exit(1);
		 }
	}

	/**
	 * Parses a byte array and checks if is a read request or a write request
	 * @param data, byte[] that contains the request
	 */
	public void checkReadWrite(byte[] data){
		
		read = false;
		write = false;
		
		if(data[1] == one) read = true;
		else if(data[1] == zero) write = true;
	}
	/**
	 * Stops the running loop
	 */
	public synchronized void stop(){
		running = false;
	}
	
	public synchronized boolean isRunning(){
		return running;
	}
	
	/**
	 * Shuts down the server
	 */
	private synchronized void shutdown() {
		receiveSocket.close();
		System.out.println("Server has stopped taking requests.");
	}
	
	/**
	 * Prints information relating to a send request
	 * @param packet, DatagranPacket that is used in the send request
	 */
	private void printSend(DatagramPacket packet){
		System.out.println( "Server: Sending packet");
	    System.out.println("To host: " + packet.getAddress());
	    System.out.println("Destination host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("Server: Packet received");
	    System.out.println("From host: " + packet.getAddress());
	    System.out.println("Host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a any request
	 * @param packet, DatagramPacket that is used in the request
	 */
	private void printStatus(DatagramPacket packet){
	    int len = packet.getLength();
	    System.out.println("Length: " + len);
	    System.out.print("Containing: " );
	    
	    //prints the bytes of the packet
	    System.out.println(Arrays.toString(packet.getData()));
	    
	    //prints the packet as text
	    String received = new String(packet.getData(),0,len);   
	    System.out.println(received + "\n");
	}
	
	public static void main(String args[]){
		Server server = new Server();
		server.run();

		Scanner scan = new Scanner(System.in);
		if(scan.hasNextLine()){
			String message = scan.nextLine();
			if(message.equals("exit")){
				scan.close();
				server.stop();
			}		
		}
	}
}