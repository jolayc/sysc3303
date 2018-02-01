
/**
 * EchoClient
 * Client side of a simple echo server.
 * The client sends a request packet to the intermediate host
 * It then recieves a reply packet from the intermediate host.
 * @author: Jack MacDougall
 * @date: January 18, 2018
 */

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class EchoClient {
	
	private final byte zero = 0x00;
	private final byte one = 0x01;
	private final byte two = 0x02;

	private final String filename = "test.txt";
	private final String mode = "netascii";
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	public EchoClient(){
		
		try{
			//constructs a socket to send and receive packets from any available port
			sendReceiveSocket = new DatagramSocket();

		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Sends a request packet to the intermediate host
	 * Receives a response packet from the intermediate host 
	 */
	public void sendAndReceive(){
		
		byte[] buf;
	
		for(int i = 0; i < 11; i++){
			
			buf = createByteArray(i+1);
				
			//creates a datagram packet that  contains buf will be ported to port 23 on the intermediate host
			try {
				sendPacket = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
				
			printSend(sendPacket);
			    
			//sends the sendPacket to the intermediate host
			try{
				sendReceiveSocket.send(sendPacket);
			}
			catch(IOException e){
				e.printStackTrace();
		        System.exit(1);
			}
			
			System.out.println("Client: Packet sent.\n");
		
			//constructs a datagram packet to receive packets 20 bytes long
			byte[] data = new byte[4];
		    receivePacket = new DatagramPacket(data, data.length);
		    
		    //waits until sendReceiveSocket receives a datagram packet from the intermediate host
		    try{
		    	System.out.println("Waiting...");
				sendReceiveSocket.receive(receivePacket);
		    }
		    catch(IOException io){
		    	io.printStackTrace();
		        System.exit(1);
		    }
			
		    printReceive(receivePacket);
		} 
	}
	
	/**
	 * Creates a byte array that contains a request that will be later send to the intermediate host
	 * @param index, integer representing which number packet is being created
	 * @return the byte[] containing the request
	 */
	public byte[] createByteArray(int index){
		
		//contains the bytes of the global strings
		byte[] filebyte = filename.getBytes();
		byte[] modebyte = mode.getBytes();
		
		//buffer that will contain the request bytes
		byte[] buffer = new byte[filebyte.length + modebyte.length + 4];
		
		buffer[0] = zero;
		
		//odd numbered requests
	    if(index%2 == 1){
			buffer[1] = one;
		}
	    //even numbered requests
		else{
			buffer[1] = two;
		}
	    
	    //copying the bytes from filebyte into the buffer
	    for(int ch = 0; ch < filebyte.length; ch++){
	    	buffer[2+ch] = filebyte[ch];
	    }
	    
	    //the eleventh request will be invalid
	    if(index == 11){ 
	    	buffer[2+filebyte.length] = one;
	    }
	    else{
	    	buffer[2+filebyte.length] = zero;
	    }
	    
	    //copying the bytes from modebyte into the buffer
	    for(int ch = 0; ch < modebyte.length; ch++){
	    	buffer[3+filebyte.length+ch] = modebyte[ch];
	    }
	    
	    buffer[3+filebyte.length+modebyte.length] = zero;
	    return buffer;
	}

	/**
	 * Prints information relating to a send request
	 * @param packet, DatagramPacket that is used in the send request
	 */
	private void printSend(DatagramPacket packet){
		System.out.println("Client: Sending packet");
	    System.out.println("To host: " + packet.getAddress());
	    System.out.println("Destination host port: " + packet.getPort());
	    printStatus(packet);
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("Client: Packet received");
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
		EchoClient c = new EchoClient();
	    c.sendAndReceive();
	}
}

