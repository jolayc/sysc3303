package iteration4;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.nio.file.AccessDeniedException;
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
	private final byte FIVE = 0x05;
	private Writer writer;
	
	private int[] blockNum;
	private int numberOfTimeout=0;
	private byte[] fileAsBytes;
	
	private int simPort = 23;
	private int serverPort;
	private int receivePort;

	private InetAddress address;
	
	private boolean quiet;
	private boolean foreignServer;
	private boolean multiClient;
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	private String relativePath = System.getProperty("user.dir");
	private String directory;
	private String mode;

	/**
	 * Constructor for client
	 * constructs a socket to send and receive packets from any available port
	 */
	public Client() {
		try {
			// Bind socket to any available port (The Client port)
			// which will be used for both sending and receiving
			sendReceiveSocket = new DatagramSocket();
			byte[] data = new byte[4];//2 Bytes for opcode 2 Bytes for block number
		    receivePacket = new DatagramPacket(data, data.length);
		    directory = relativePath + "\\Client\\";
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
		
		if(multiClient) serverPort = 69;
			
		try {
			sendReceiveSocket.setSoTimeout(10000);
		} catch (SocketException e2) {
			e2.printStackTrace();
			System.exit(1);
		}

		System.out.println("Client: Requested to write to server with filename: " + filename);
		// Buffer for Server ACK
		byte[] serverACK;

		// Status Flags
		boolean first = true;
		boolean received = false;
		boolean sentEmptyData = false;
		boolean finished = false;

		blockNum = new int[2];
		blockNum[0] = 0;
		blockNum[1] = 0;

		// Prompt user to provide path of file and convert to byte[]
		fileAsBytes = toBytes(filename);

		// Create and send request
		DatagramPacket writeRequest = createWRQPacket(filename);
		if(foreignServer) writeRequest.setAddress(address);
		try {
			sendReceiveSocket.send(writeRequest); // Client Port to Error Sim Port (23)
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		// Print the request packet being sent
		printSend(writeRequest);

		// Process response from Server
		while (true) {
			// Server responds to Write request with an ACK
			serverACK = new byte[516];
			received = false;
			receivePacket = new DatagramPacket(serverACK, serverACK.length);

			// Receive ACK packet from Server
			// and handle timeouts
			while (!received) {
				try {
					sendReceiveSocket.receive(receivePacket); // Port 23 (Error Sim) to Port Client

					// check for legality
					if (!checkLegality(receivePacket)) {
						ErrorPacket illegalOperation = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
						sendErrorPacket(illegalOperation);
						System.out.println("Illegal TFTP Operation.");
						shutdown();
					}

					if (first) {
						receivePort = receivePacket.getPort();
						first = false;
					}
					
					if(multiClient) serverPort = receivePort;

					// check for port
					if (receivePacket.getPort() != receivePort) {
						ErrorPacket wrongPort = new ErrorPacket(ErrorCode.UNKNOWN_TRANSFER_ID);
						sendErrorPacket(wrongPort);
						System.out.println("Packet received from unknown port.");
						shutdown();
					}

					checkError(receivePacket);
					received = true;
					// Socket Timeout handling
				} catch (SocketTimeoutException se) {
					while (numberOfTimeout < 2) {
						numberOfTimeout++;
						if (sendPacket == null) {
							if (numberOfTimeout == 2) {
								try {
									// Retransmit
									sendReceiveSocket.send(writeRequest);

								} catch (IOException e) {
									e.printStackTrace();
									System.exit(1);
								}
							}
						} else {
							if (numberOfTimeout == 2) {
								try {
									sendReceiveSocket.send(sendPacket);
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(1);
								}
							}
						}
					}
					numberOfTimeout = 0;
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				if(first){
					receivePort = receivePacket.getPort();
					if(foreignServer) serverPort = receivePort;
					first = false;
				}
				
				//check for port
				if(receivePacket.getPort() != receivePort){
			}

			printReceive(receivePacket);
			
			// If empty DATA block has been sent
			// it has been acknowledge at this point, so handle
			// things here and/or break
			if(sentEmptyData) {
				if (receivePacket.getPort() != receivePort) {
					ErrorPacket wrongPort = new ErrorPacket(ErrorCode.UNKNOWN_TRANSFER_ID);
					sendErrorPacket(wrongPort);
					System.out.println("Packet received from unknown port.");
					shutdown();
				}
				System.out.println("Client: Read complete, blocks received: " + blockNum[0] + blockNum[1]);
				return;
			}
			
			calcBlockNumber();
			// Duplicate packet checking
			if (getBlockIntegerValue(blockNum[0], blockNum[1]) < getBlockIntegerValue(receivePacket.getData()[2], receivePacket.getData()[3])) {
				receivePacket = new DatagramPacket(serverACK, serverACK.length);
				receivePack(sendReceiveSocket, receivePacket);
			}

			// Send a DATA Block to write
			byte[] dataBlock;
			if(!finished) {
				dataBlock = createDataPacket();
			} else {
				// Finished sending DATA blocks
				// send an empty block
				dataBlock = createEmptyDataPacket();
				sentEmptyData = true;
			}

			// Create and Send DATA block to Server
		
			try {
				if(foreignServer && multiClient) sendPacket = new DatagramPacket(dataBlock, dataBlock.length, address, serverPort);
				else if(foreignServer) sendPacket = new DatagramPacket(dataBlock, dataBlock.length, address, simPort);
				else if(multiClient) sendPacket = new DatagramPacket(dataBlock, dataBlock.length, InetAddress.getLocalHost(), serverPort);
				else sendPacket = new DatagramPacket(dataBlock, dataBlock.length, InetAddress.getLocalHost(), simPort);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			
			try {
				sendReceiveSocket.send(sendPacket); // Client Port to Port 23 (Error Sim)
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// end of transfer
			if(!sentEmptyData) {
				int len = 0;
				for (byte b: dataBlock) {
					if (b == 0 && len > 4) break;
					len++;
				}
				if (len < 512) finished = true;
			}
			}}
	}
	
	/**
	 * Creates an empty DATA packet filled with 0s
	 * @return	byte[] empty DATA packet (byte array)
	 */
	private byte[] createEmptyDataPacket() {
		byte[] data = new byte[516];
		data[0] = 0;
		data[1] = 3;
		for (int i = 2; i < data.length; i++) {
			data[i] = 0;
		}
		try {
			if(foreignServer && multiClient) sendPacket = new DatagramPacket(data, data.length, address, serverPort);
			else if(foreignServer) sendPacket = new DatagramPacket(data, data.length, address, simPort);
			else if(multiClient) sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), serverPort);
			else sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), simPort);
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
		for (int i = 2; i < data.length; i++) data[i] = 0;
		return data;
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
		
		// Status flags
		boolean first = true;
		
		if(multiClient) serverPort = 69;
		boolean finished = false;
		boolean emptyDataReceived = false;
		
		// Create and send request
		DatagramPacket readRequest = createRRQPacket(filename);
		printSend(readRequest);
		try {
			sendReceiveSocket.send(readRequest);
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
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if(first) receivePort = receivePacket.getPort();
			
			// check if empty DATA packet (finished transferring)
			// e.g. receivePacket = [0, 3, 0 ... 0]
			if (receivePacket.getData()[1] == 3 && receivePacket.getData()[2] == 0
					&& receivePacket.getData()[5] == 0) {
				emptyDataReceived = true;
			}
			
			if (emptyDataReceived) finished = true;
			//check for port
			if(receivePacket.getPort() != receivePort){
				ErrorPacket wrongPort = new ErrorPacket(ErrorCode.UNKNOWN_TRANSFER_ID);
				sendErrorPacket(wrongPort);
				System.out.println("Packet received from unknown port.");
				shutdown();
			}
			
			//check for legality
			if(!checkLegality(receivePacket)) {
				ErrorPacket illegalOperation = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
				sendErrorPacket(illegalOperation);
				System.out.println("Client: Received Illegal TFTP Operation.");
				shutdown();
			}
			
			checkError(receivePacket);
			// Check block number received
			
			// Print contents
			System.out.println("Client: Received DATA block from server: ");
			printStatus(receivePacket);
			
			if(first) {
				// Create writer and file with filename in Client folder
				try { 
					File f = new File(directory + "//" + filename);
					if(f.exists()) {
						ErrorPacket fileExists = new ErrorPacket(ErrorCode.FILE_ALREADY_EXISTS);
						sendErrorPacket(fileExists);
						System.out.println("File already exists in Client folder.");
						shutdown();
					}
					writer = new Writer(f.getPath(), false);
					first = false;
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		
			if(multiClient) serverPort = receivePort;
			
			//removes first four bytes
			byte[] cleanedData = new byte[receivePacket.getData().length - 4];
			
			//Write contents to file in Client folder
			try {
				for(int i = 0; i < cleanedData.length; i++) {
					if(receivePacket.getData()[4+1] == (byte)0) { break; }
					cleanedData[i] = receivePacket.getData()[4+i];
				}
				writer.write(cleanedData);
			} catch (AccessDeniedException e) {
				ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
				sendErrorPacket(fileAccessDenied);
				System.out.println("Access Violation.");
				shutdown();
			} catch (IOException e) {
				ErrorPacket diskFull = new ErrorPacket(ErrorCode.DISK_FULL_OR_ALLOCATION_EXCEEDED);
				sendErrorPacket(diskFull);
				System.out.println("The disk is full.");
				shutdown();
			}
			
			// Create and send ACK
			ack = createACKPacket(blockNum);
			
			calcBlockNumber();
			
			// Duplicate packet checking
			if(getBlockIntegerValue(blockNum[0], blockNum[1]) < getBlockIntegerValue(receivePacket.getData()[2], receivePacket.getData()[3])) {
				receivePacket = new DatagramPacket(incomingData, incomingData.length);
				receivePack(sendReceiveSocket, receivePacket);
			}
			
			try {
				sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), serverPort);

				if(foreignServer && multiClient) sendPacket = new DatagramPacket(ack, ack.length, address, serverPort);
				else if(foreignServer) sendPacket = new DatagramPacket(ack, ack.length, address, simPort);
				else if(multiClient) sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), serverPort);
				else sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), simPort);

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

			//end of transfer
			if(finished) {
				System.out.println("Client: Read complete, blocks received: " + ((blockNum[0]*10) + blockNum[1] - 1));
				return;
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
		if(blockNum[1] > 1) multiplier += blockNum[1]-1;
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
	         printReceive(packet);	
		} catch(SocketTimeoutException e){
			return null;
	    } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	    }
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
			if(foreignServer && multiClient) send = new DatagramPacket(rq, rq.length, address, serverPort);
			else if(foreignServer) send = new DatagramPacket(rq, rq.length, address, simPort);
			else if(multiClient) send = new DatagramPacket(rq, rq.length, InetAddress.getLocalHost(), serverPort);
			else send = new DatagramPacket(rq, rq.length, InetAddress.getLocalHost(), simPort);
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
			errorPacket = new DatagramPacket(error.getBytes(), error.length(), InetAddress.getLocalHost(), simPort);
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
	
	/**
	 * Get integer value of block number
	 * @param a, byte[2] in packet
	 * @param b, byte[3] in packet
	 * @return int
	 */
	private int getBlockIntegerValue(int a, int b) {
		return (a*10) + b;
	}
	
	/**
	 * Checks legality of packet
	 * @param packet, datagram packet that will be checked
	 * @return true if legal, false if not
	 */
	public boolean checkLegality(DatagramPacket packet) {
		
		//if first byte in packet is not zero
		if(packet.getData()[0] != ZERO) return false;
		
		//if second byte in packet is greater than 5
		if(packet.getData()[1] > FIVE) return false;
		
		else return true;
	}
	
	/**
	 * Checks if the packet is an error packet
	 * @param packet, DatagramPacket that will be checked
	 */
	private void checkError(DatagramPacket packet) {
		
		//if packet is an error packet
		if(packet.getData()[1] == 5) {
			System.out.println(Arrays.toString(packet.getData()));
			byte[] message = new byte[packet.getData().length - 5];
			
			//extract message
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
	private void printSend(DatagramPacket packet) {
		System.out.println("Client: Sending packet");
		if (!quiet) {
		    System.out.println("To host: " + packet.getAddress());
		    System.out.println("Destination host port: " + packet.getPort());
		    printStatus(packet);
		}
	}
	
	/**
	 * Prints information relating to a receive request
	 * @param packet, DatagramPacket that is used in the receive request
	 */
	private void printReceive(DatagramPacket packet){
		System.out.println("Client: Packet received");
		if(!quiet) {
		    System.out.println("From host: " + packet.getAddress());
		    System.out.println("Host port: " + packet.getPort());
		    printStatus(packet);
		}
	}
	
	/**
	 * Prints information relating to a any request
	 * @param packet, DatagramPacket that is used in the request
	 */
	private void printStatus(DatagramPacket packet) {
		if (!quiet) {
			byte[] data = packet.getData();
			PacketType type = PacketType.getPacketType((int)data[1]);
			int len = packet.getLength();
			System.out.println("Length: " + len);
			System.out.println("Packet Type: " + type);
			System.out.print("Containing: ");

			// prints the bytes of the packet
			System.out.println(Arrays.toString(data));

			// prints the packet as text
			String received = new String(packet.getData(), 0, len);
			System.out.println(received);
		}
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
		} catch (AccessDeniedException e) {
			ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
			sendErrorPacket(fileAccessDenied);
			System.out.println("Access Violation.");
			shutdown();
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
	
	/**
	 * Sets the mode to 'Quiet' or 'Verbose'
	 * @param mode	True for Quiet, False for Verbose
	 */
	private void setQuiet(boolean quiet) {
		this.quiet = quiet;	
	}
	
	/**
	 * Sets the foreignServer to true or false
	 * @param foreign True for server on foreign computer, False if on the same
	 */
	private void setForeign(boolean foreign){
		this.foreignServer = foreign;
	}
	
	/**
	 * Sets the address of the foreign server
	 * @param address, String with address of foreign server
	 */
	private void setForeignAddress(String address){
		try {
			this.address = InetAddress.getByName(address);
			System.out.println(address.toString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}		
	}
	
	/**
	 * Sets multiClient to true or false
	 * @param multiClient, boolean
	 */
	private void setMultiClient(boolean multiClient){
		this.multiClient = multiClient;
	}
	
	/**
	 * Sets the mode that the request will have
	 * @param mode, String with the mode
	 */
	private void setMode(String mode){
		this.mode = mode;
	}
	
	
	public static void main(String args[]){
		Client c = new Client();
		Scanner sc =  new Scanner(System.in);
		boolean textSelected = false;
		boolean modeSelected = false;
		boolean serverSelected = false;
		boolean clientSelected = false;
		boolean directorySelected = false;
		String textMode, mode, server, host, client, in, command, change;
		
		while (true) {
			
			while(!directorySelected){
				
				System.out.println("Do you want to change the location of the client? (y/n)");
				while(!sc.hasNext()) sc.next();
				change = sc.nextLine();
				if(change.equals("y")){
					System.out.println("Enter directory of client");
					while(!sc.hasNext()) sc.next();
					c.directory = sc.nextLine();
					directorySelected = true;
				}else if(change.equals("n"))directorySelected = true;
			}
			
			while(!textSelected) {
				// Select quiet or verbose mode
				System.out.println("Client: Enter q for 'quiet' mode or v for 'verbose'");
				while(!sc.hasNext()) sc.next();
				textMode = sc.nextLine();
				if(textMode.equals("q")) {
					c.setQuiet(true);
					textSelected = true;
				} else if (textMode.equals("v")) {
					c.setQuiet(false);
					textSelected = true;
				} 
			}
			
			while(!modeSelected){
				//selected netascii or octet mode
				System.out.println("Client: Enter n for netascii mode or o for octet mode");
				while(!sc.hasNext()) sc.next();
				mode = sc.nextLine();
				if(mode.equals("n")){
					c.setMode("netascii");
					modeSelected = true;
				} else if(mode.equals("o")){
					c.setMode("octet");
					modeSelected = true;
				}
			}
			
			while(!serverSelected){
				// Select normal or foreign server
				System.out.println("Client: Enter n for 'normal server' mode or f for 'foreign server' mode");
				while(!sc.hasNext()) sc.next();
				server = sc.nextLine();
				if(server.equals("n")){
					c.setForeign(false);
					serverSelected = true;
				}else if(server.equals("f")){
					c.setForeign(true);
					// Select port of foreign server
					System.out.println("Client: Enter host of foreign server");
					while(!sc.hasNextLine()) sc.nextLine();
					host = sc.nextLine();;
					c.setForeignAddress(host);
					serverSelected = true;
					}
			}
			
			while(!clientSelected){
				//for allowing one or multiple clients
				System.out.println("Client: enter s for 'only allow one client' mode or m for 'allow multiple clients' mode");
				while(!sc.hasNext()) sc.next();
				client = sc.nextLine();
				if(client.equals("s")){
					c.setMultiClient(false);
					clientSelected = true;
				}else if(client.equals("m")){
					c.setMultiClient(true);
					clientSelected = true;
				}
			}
			
			directorySelected = false;
			modeSelected = false;
			
			System.out.println("Client: Enter file name or 'exit' to terminate.");
			while(!sc.hasNext()) sc.next();
			in = sc.nextLine();
			if (in.equals("exit")) break;
			
			System.out.println("Client: Enter 'r' for read request or 'w' for write request");
			while(!sc.hasNext()) sc.next();
			command = sc.nextLine().toLowerCase();
			
			if (command.equals("r")) c.sendRead(in);
			else if (command.equals("w")) c.sendWrite(in);
			else System.out.println("Client: Command not recognized.");
		}
		sc.close();
		c.shutdown();
	}
}
