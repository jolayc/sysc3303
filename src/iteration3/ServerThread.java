package iteration3;
import java.net.*;
import java.nio.file.FileAlreadyExistsException;
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
	
	private boolean done = false;
	
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
	 * Request is processed by the Server but the DATA and ACK
	 * packets are handled by the Thread
	 */
	public void run() {
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException e1) { 
			e1.printStackTrace();
			System.exit(1);
		}
		
		byte[] data =  receivePacket.getData();
		if (data[0] == 0 && data[1] == 1) handleRead(); // read request
		else if (data[0] == 0 && data[1] == 2) handleWrite(); // write request
	}
	
	private void handleWrite() {
		// response packet
		byte[] response = new byte[512 + 4];
		byte[] data;
		
		// send ACK to write request
		response = createACKPacket();
		try {
			sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receivePacket.getPort());
		} catch (UnknownHostException ue) {
			ue.printStackTrace();
			System.exit(1);
		}
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// process DATA packets
		while (true) {
			data = new byte[512 + 4];
			receivePacket = new DatagramPacket(data, data.length);
			// receive packet from Client
			try { 
				sendReceiveSocket.receive(receivePacket);
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			try {
				handleData(receivePacket.getData());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			// create ACK packet to acknowledge DATA packet
			response = createACKPacket();
			blockNum = calcBlockNumber();
			try {
				sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException ue) {
				ue.printStackTrace();
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
			for(byte b: data){
				if(b == 0 && len > 4) break;
				len++;
			}
			
			if(len < 512) {
				break;
			}
		}
		sendReceiveSocket.close();
	}
	
	private void handleRead() {
		// response packet
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
	 * increments block number 
	 * @return block number as array on integers
	 */
	
	private int[] calcBlockNumber(){

		if(blockNumber[1] == 9) {
			blockNumber[0]++;
			blockNumber[1] = 0;
		}
		else{
			blockNumber[1]++;
		}

		return blockNumber;
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
		if(blockNumber[1] > 1) multiplier += blockNumber[1]-1;
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