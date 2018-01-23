
/**
 * EchoIntermediateHost
 * Intemediate Host of a simple echo server.
 * The intermediate host receives from a client a request packet and then sends it to the server
 * It then receives from the server a reply packet back to the original client
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class EchoIntermediateHost {
	
	private DatagramSocket receiveSocket, sendReceiveSocket;
	private DatagramPacket receivePacket, sendReceivePacket, sendPacket;
	
	public EchoIntermediateHost(){
		try {
			//Constructs a socket to receive packets bounded to port 23
			receiveSocket = new DatagramSocket(23);
			
			//Constructs a socket to send packets from any available port
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Receives a request packet from the client
	 * Sends the request to the server
	 * Receives a response packet from the server
	 * Sends the response to the client 
	 */
	public void receiveAndSend(){
	
		//will repeat "forever"
		while(true){
			
			//buffer for the receive packet
			byte receiveData[] = new byte[20];
			
			//buffer for the response packet to be sent to the intermediatehost
			byte sendData[] = new byte[4];
		    
			//constructs a datagram packet to receive packets 20 bytes long
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
		    System.out.println("IntermediateHost: Waiting for Packet from client.\n");
		    
		    //waits until receiveSocket receives a datagram packet from the client
		    try {        
		         System.out.println("Waiting..."); 
		         receiveSocket.receive(receivePacket);
		      } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
		    
		    printReceive(receivePacket);
		    
		    //waits until receiveSocket receives a datagram packet from the client
		    try {
				sendReceivePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(), 69);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
		    
		    //sends the sendReceivePacket to the intermediate host
		    try {
				sendReceiveSocket.send(sendReceivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		    
		    printSend(sendReceivePacket);
		    
		    //creates a datagram packet that will contain sendData that will be ported to port 69
		    try {
				sendReceivePacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), 69);
				System.out.println("IntermediateHost: Waiting for Packet from server.\n");
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
		    
		    //waits until sendReceivePacket receives a datagram packet from the server
		    try {        
		         System.out.println("Waiting..."); 
		         sendReceiveSocket.receive(sendReceivePacket);
		     } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		     }
		    
		    printReceive(sendReceivePacket);
		    
		    //creates a datagram packet that will be ported to wherever receivePacket is ported to
		    sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
		    
		    //sends sendPacket to the client
		    try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}  
		    
		    printSend(sendPacket);
		}
	}
	
	/**
	 * Prints information relating to a send request
	 * @param packet, DatagramPacket that is used in the send request
	 */
	private void printSend(DatagramPacket packet){
		System.out.println("IntermediateHost: Sending packet");
	    System.out.println("To host: " + packet.getAddress());
	    System.out.println("Destination host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("IntermediateHost: Packet received");
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
	    System.out.print("Containing: ");
	    
	    //prints the bytes of the packet
	    System.out.println(Arrays.toString(packet.getData()));
	    
	    //prints the packet as text
		String received = new String(packet.getData(),0,len);
		System.out.println(received);
	}
	
	public static void main(String args[]){
		EchoIntermediateHost h = new EchoIntermediateHost();
		h.receiveAndSend();
	}

}
