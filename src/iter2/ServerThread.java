package iter2;
import java.net.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.AccessDeniedException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * A server thread that will handle read/write requests received by server, sent by client 
 */

public class ServerThread extends Thread implements Runnable {
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket, sendPacket;
	// The directory where files will be written to
	private String relativePath = System.getProperty("user.dir");
	private Writer writer;
	private int[] blockNumber;
	
	// String identifiers
	private String message;
	private String read = "READ";
	private String write = "WRITE";
	private File file;
	
	private byte[] path;
	private int[] blockNum;
	
	private final byte ZERO = 0x00;
	private final byte ONE = 0x01;
	private final byte TWO = 0x02; 
	private final byte FOUR = 0x04;
	/**
	 * Constructor for ServerThread
	 * @param receivePacket packet received from host
	 * @param path is the file name that the server writing to 
	 * @param message is the read or write request
	 * @param blockNumber is the current block number of the file 
	 */

	public ServerThread(DatagramPacket receivePacket, byte[] path, File file, String message, int[] blockNumber) {
		this.message = message;
		this.receivePacket = receivePacket;
		this.file = file;
		this.path = path;
		this.blockNumber = blockNumber;
	}
	/**
	 * Run handles the packets received from the host. 
	 */
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
		else if(message.equals(write)){
			response = createACKPacket();
		}
		
		// Construct a socket to send packets to any available port
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		if (message.equals(read)) {
			response = createDataPacket();
			if(response[5] == 0) {
				return;
			}
			// create a datagram packet that will contain sendBytes that will be ported to the same
			// port as receivePacket
			try {
				sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receivePacket.getPort());
			} catch (AccessDeniedException e) {
				ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
				sendErrorPacket(fileAccessDenied);
				System.out.println("Access Violation.");
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			sendPack(sendReceiveSocket, sendPacket);
		}
		if (message.equals(write)) {
			byte[] data = receivePacket.getData();
			try {
				switch(data[1]) {
				case (byte)3:
					// Handle Data packet received
					handleData(data);
				default:
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			response = createACKPacket();
			try {
				sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receivePacket.getPort());
			} catch (AccessDeniedException e) {
				ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
				sendErrorPacket(fileAccessDenied);
				System.out.println("Access Violation.");
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			sendPack(sendReceiveSocket, sendPacket);
		}
		sendReceiveSocket.close();
	}
	
	

	/**
	 * Creates a file (on machine) with requested file name to be written to
	 * if it does not already exist
	 * @param data
	 */
	private void handleData(byte[] data) throws IOException {
		byte[] cleanedData = new byte[data.length - 4];
		try {
			writer = new Writer(file.getPath(), true);
			for(int i = 0; i < cleanedData.length; i++) {
				if(data[i+4] == 0) break;
				cleanedData[i] = data[4+i];
			}
			writer.write(cleanedData);
		} catch (AccessDeniedException e) {
			ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
			sendErrorPacket(fileAccessDenied);
			System.out.println("Access Violation.");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (data.length < 516) {
			writer.close();
			System.out.println("Server: File transfer/write complete.");

		}
	}
	
	/**
	 * Sends a packet to a socket
	 * @param socket, DatagramSocket where the packet will be sent
	 * @param packet, DatagramPacket that will be sent
	 */
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
	 * Create a data packet
	 * @return byte[516] data packet
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
	
	public byte[] createACKPacket(int[] block) {
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
	 *  Print information relating to send request 
	 * @param dp datagram Packet being printed
	 */
	private void printSend(DatagramPacket dp) {
		System.out.println("Server: Sending packet");
		System.out.println("To host: " + dp.getAddress());
		System.out.println("Destination Port: " + dp.getPort());
		printInfo(dp);
	}
	
	/**
	 *  Print information relating to packet
	 * @param dp datagram Packet being printed
	 */
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
