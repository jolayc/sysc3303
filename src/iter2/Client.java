package iter2;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Client of server/client TFTP application
 * The client sends a request packet to the intermediate host
 * It then receives either a data or ack packet from the intermediate host depending of the request type.
 */


public class Client {
	
	private final byte ZERO = 0x00;
	private final byte ONE = 0x01;
	private final byte TWO = 0x02; 
	private final byte FOUR = 0x04;
	
	private Writer writer;
	
	private int[] blockNum;
	
	private byte[] fileAsBytes;
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	private String relativePath = System.getProperty("user.dir");

	/**
	 * Constructor for client
	 * constructs a socket to send and receive packets from any available port
	 */
	public Client() {
		try {
			sendReceiveSocket = new DatagramSocket();
			byte[] data = new byte[4];//2 Bytes for opcode 2 Bytes for block number
		    receivePacket = new DatagramPacket(data, data.length);
		}
		catch (SocketException se){
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Sends a write request to the server, waits for the ACK packet
	 * and sends DATA packets until complete
	 * @param filename Name of requested file to be written to server
	 */
	public void sendWrite(String filename) {
		System.out.println("Client: Requested to write to server with filename: " + filename);
		byte[] serverACK;

		blockNum = new int[2];
		blockNum[0] = 0;
		blockNum[1] = 0;

		// Prompt user to provide path of file and convert to byte[]
		fileAsBytes = toBytes(filename);
		
		// Create and send request
		DatagramPacket writeRequest = createWRQPacket(filename);
		printSend(writeRequest);
		try {
			sendReceiveSocket.send(writeRequest);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		//Process response from Server
		while(true) {
			// Server responds to Write request with an ACK
			serverACK = new byte[4];
			receivePacket = new DatagramPacket(serverACK, serverACK.length);

			// Receive ACK packet from Server
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			calcBlockNumber();

			// Send a DATA Block to write
			byte[] dataBlock = createDataPacket();

			// Wait for an ACK from Server
			try {
				sendPacket = new DatagramPacket(dataBlock, dataBlock.length, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// check if end of write
			int len = 0;
			for(byte b: dataBlock){
				if(b == 0 && len > 4) break;
				len++;
			}		
			if(len < 512) {
				System.out.println("Client: Read complete, blocks received: " + blockNum[0] + blockNum[1]);
				break;
			}
		}
	}
	
	/**
	 * Sends a read request to the server, waits for a DATA packet
	 * and responds with an ACK
	 * @param filename Name of requested file to be read from server
	 */
	public void sendRead(String filename){
		System.out.println("Client: Requesting to read from server with filename: " + filename);
		byte[] incomingData;
		byte[] ack;
		blockNum = new int[2];
		blockNum[0] = 0;
		blockNum[1] = 1;
		
		// Create and send request
		DatagramPacket readRequest = createRRQPacket(filename);
		printSend(readRequest);
		try {
			sendReceiveSocket.send(readRequest);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Create writer and file with filename in Client folder
		try { 
			File f = new File(relativePath + "\\Client\\" + filename);
			writer = new Writer(f.getPath(), false);
		} catch (FileAlreadyExistsException fe) {
			ErrorPacket fileExists = new ErrorPacket(ErrorCode.FILE_ALREADY_EXISTS);
			sendErrorPacket(fileExists);
			System.out.println("File already exists in Client folder.");
			System.exit(1);;
			this.shutdown();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Process response from Server
		while (true) {
			// Server responds to a Read Request with a DATA packet (save to receivePacket)
			incomingData = new byte[4 + 512]; // 2 for opcode, 2 for block and 512 bytes for max block size
			receivePacket = new DatagramPacket(incomingData, incomingData.length);
			// Receive packet from server
			try {
				sendReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			checkError(receivePacket);
			// Check block number received
			
			// Print contents
			System.out.println("Client: Received DATA block from server: ");
			printStatus(receivePacket);
			
			//removes first four bytes
			byte[] cleanedData = new byte[receivePacket.getData().length - 4];
			
			//Write contents to file in Client folder
			try {
				for(int i = 0; i < cleanedData.length; i++) {
					if(receivePacket.getData()[4+1] == (byte)0) { break; }
					cleanedData[i] = receivePacket.getData()[4+i];
				}
				writer.write(cleanedData);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// Create and send ACK
			ack = createACKPacket(blockNum);
			calcBlockNumber();
			
			try {
				sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			try {
				sendReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// check if end of read
			int len = 0;
			for(byte b: incomingData){
				if(b == 0 && len > 4) break;
				len++;
			}
			
			if(len < 512) {
				System.out.println("Client: Read complete, blocks received: " + blockNum[0] + blockNum[1]);
				break;
			}
		}
	}
	
	/**
	 * Create a data packet
	 * @return byte[516] data packet
	 */
	public byte[] createDataPacket() {
		byte[] data = new byte[512 + 4];
		data[0] = 0;
		data[1] = 3;
		data[2] = (byte)blockNum[0];
		data[3] = (byte)blockNum[1];
		
		int multiplier = 0;
		if(blockNum[1] > 1) multiplier += blockNum[1];
		if(blockNum[0] > 0) multiplier += (10*blockNum[0]);
	

		for(int i = 0; i < fileAsBytes.length; i++){
			if(fileAsBytes.length <= (512*multiplier+i)) break;
			if((fileAsBytes.length > (512*multiplier+i)) && i < 512){
				fileAsBytes[i] = fileAsBytes[512*multiplier+i];
			}		
		}
		
		for(int j = 0; j < fileAsBytes.length; j++){
			if(fileAsBytes.length <= (512*multiplier+j)) break;
			if(j == 512) break;
			data[4+j] = fileAsBytes[j];
		}
		
		return data;
	}
	
	/**
	 * Create ACK packet to be sent to server during a read request
	 * @param block
	 * @return ACK packet {0, 4, block number(hi), block number(lo)}
	 */
	private byte[] createACKPacket(int[] block) {
		byte[] pack = new byte[4];
		// {0, 4} op code
		pack[0] = ZERO;
		pack[1] = FOUR;
		// load block number into ack packet
		pack[2] = (byte)block[0];
		pack[3] = (byte)block[1];
				
		return pack;
	}
	
	/**
	 * Checks if byte[] is an ack packet
	 * @param b, byte[] to be checked
	 * @return true if b is an ack packet
	 * @return false if b is not an an ack packet
	 */
	public boolean checkACK(byte[] b) {
		byte[] tmp = new byte[] {0,4};
		return Arrays.equals(tmp, b);
	}
	
	/**
	 * Sends a packet to a socket
	 * @param socket, DatagramSocket where the packet will be sent
	 * @param packet, DatagramPacket that will be sent
	 */
	public void sendPack(DatagramSocket socket, DatagramPacket packet) {
		printSend(sendPacket);
		try {
			 socket.send(packet);
		}
		catch(IOException io){
			 io.printStackTrace();
			 System.exit(1);
		}
	}
	
	/**
	 * Receives a packet from a socket
	 * @param socket, DatagramSocket where the packet data will be received from
	 * @param packet, DatagramPacket where the data from the socket will be stored
	 */
	public DatagramPacket receivePack(DatagramSocket socket, DatagramPacket packet) {
		System.out.println("Client: Waiting for Packet.\n");
		try {        
	         System.out.println("Waiting...");
	         socket.receive(packet);
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
		printReceive(packet);	
		return packet;
	}

	
	/**
	 * Creates read request packet
	 * @return DatagramPacket containing read request
	 */
	public DatagramPacket createRRQPacket(String filename){
		String mode = "netascii";
		// |Opcode (2 bytes)|
		byte[] rrq = new byte[4 + filename.length() + mode.length()];
		rrq[0] = ZERO;
		rrq[1] = ONE;
		// |Filename|0|Mode|0|
		finishRRQOrWRQ(rrq, filename, mode);
		
		return createSendPacket(rrq);
	}
	
	/**
	 * Creates write request packet
	 * @return DatagramPacket containing read request
	 */
	public DatagramPacket createWRQPacket(String filename){
		String mode = "netascii";
		// |Opcode (2 bytes)|
		byte[] wrq = new byte[4 + filename.length() + mode.length()];
		wrq[0] = ZERO;
		wrq[1] = TWO;
		
		finishRRQOrWRQ(wrq, filename, mode);
		
		return createSendPacket(wrq);
	}
	
	/**
	 * Finishes the request packet for both read and write
	 * @param rq, byte[] with start of request
	 */
	private void finishRRQOrWRQ(byte[] rq, String filename, String mode){
		//contains the bytes of the global strings
	    byte[] filebyte = filename.getBytes();
		byte[] modebyte = mode.getBytes();	
		
		for(int ch = 0; ch < filebyte.length; ch++){
	    	rq[2 + ch] = filebyte[ch];
	    }
		
		rq[3 + filebyte.length] = ZERO;
		
		for(int ch = 0; ch < modebyte.length; ch++){
			rq[3 + filebyte.length + ch] = modebyte[ch];
	    }
		rq[3 + filebyte.length + modebyte.length] = ZERO;
	}
	
	/**
	 * Creates a send packet
	 * @param rq, byte[] with request
	 * @return DatagramPacket with send request
	 */
	public DatagramPacket createSendPacket(byte[] rq){
		DatagramPacket send = null;
		try {
			send = new DatagramPacket(rq, rq.length, InetAddress.getLocalHost(), 23);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		return send;
	}
	
	/**
	 * Creates a send packet
	 * @param block, byte[] with data
	 * @return DatagramPacket with receive request
	 */
	public DatagramPacket createReceivePacket(byte[] block){
		return new DatagramPacket(block, block.length);
	}
	
	public void sendErrorPacket(ErrorPacket error) {
		DatagramPacket errorPacket;
		try {
			errorPacket = new DatagramPacket(error.getBytes(), error.length(), InetAddress.getLocalHost(), 23);
			sendReceiveSocket.send(errorPacket);
		} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
		}
	}
	
	
	/**
	 * Calculates the current block number
	 * @return int[] which is the current block number
	 */
	private int[] calcBlockNumber(){

		//if block number needs another ten value
		if(blockNum[1] == 9) {
			blockNum[0]++;
			blockNum[1] = 0;
		}
		else{
			blockNum[1]++;
		}
		
		return blockNum;
	}
	
	private void checkError(DatagramPacket packet) {
		if(packet.getData()[1] == 5) {
			byte[] message = new byte[packet.getData().length - 5];
			for(int i = 0; i < message.length; i++) {
				if(packet.getData()[4+i] == 0) break;
				message[i] = packet.getData()[4+i];
			}
			System.out.println("Error! " + new String(message,0,message.length));
			shutdown();
		}
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
	
	/**
	 * Converts a file found at user specified path and converts
	 * into a byte[]
	 * @return File as byte[]
	 */
	private byte[] toBytes(String filename) {
		byte[] bytes = null;
		String s = relativePath + "\\Client\\" + filename; // Go into Client folder of user directory and find txt file
		Path path = Paths.get(s);
		try {
			bytes = Files.readAllBytes(path);
		} catch (NoSuchFileException fe) {
			ErrorPacket fileNotFound = new ErrorPacket(ErrorCode.FILE_NOT_FOUND);
			sendErrorPacket(fileNotFound);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		// return file as bytes
		return bytes;
	}
	/**
	 * Shuts down the server by closing the socket used
	 * for receiving
	 */
	private void shutdown() {
		sendReceiveSocket.close();
		System.out.println("Client: Terminated.");
		System.exit(1);
	}
	
	public static void main(String args[]){
		Client c = new Client();
		Scanner sc =  new Scanner(System.in);
		
		while (true) {
			System.out.println("Client: Enter file name or 'exit' to terminate.");
			String in = sc.nextLine().toLowerCase();
			if (in.equals("exit")) break;
			System.out.println("Client: Enter 'r' for read request or 'w' for write request");
			String command = sc.nextLine().toLowerCase();
			if (command.equals("r")) {
				c.sendRead(in);
			} else if (command.equals("w")) {
				c.sendWrite(in);
			} else {
				System.out.println("Command not recognized.");
			}
		}
		sc.close();
		c.shutdown();
	}
}
