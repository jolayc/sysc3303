package iteration4;
import java.net.*;
import java.nio.file.AccessDeniedException;
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
	private int numberOfTimeout=0;
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
		
		// process DATA packets
		while (true) {
			data = new byte[512 + 4];
			receivePacket = new DatagramPacket(data, data.length);
			// receive packet from Client
			try { 
				sendReceiveSocket.receive(receivePacket);
				
				if(!checkLegality(receivePacket)) {
					ErrorPacket illegalOperation = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
					sendErrorPacket(illegalOperation);
					System.out.println("Illegal TFTP Operation.");
					System.exit(1);
				}
				checkError(receivePacket);
			}
			catch(IOException e) {
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
			blockNum = calcBlockNumber();
			response = createACKPacket();
			
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
			
			boolean received = false;
			try {
				sendReceiveSocket.setSoTimeout(10000);
			} catch (SocketException e1) {
				
				e1.printStackTrace();
				System.exit(1);
			}
			
			// response packet
			byte[] data = new byte[512 + 4];
			byte[] response;
				
			// send DATA to read request
			data = createDataPacket();
			try {
				sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 23);
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
				
			// process ACK packets
			while (true) {
				response = new byte[512 + 4];
				receivePacket = new DatagramPacket(response, response.length);
				// receive packet from Client
				
				while(!received){
				try { 
					sendReceiveSocket.receive(receivePacket);
					
					if(!checkLegality(receivePacket)) {
						ErrorPacket illegalOperation = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
						sendErrorPacket(illegalOperation);
						System.out.println("Illegal TFTP Operation.");
						System.exit(1);
					}
					checkError(receivePacket);
					received = true;
				} catch (SocketTimeoutException se){
					while(numberOfTimeout < 2){
					numberOfTimeout++;
					if (numberOfTimeout==2){
						try {
							sendReceiveSocket.send(sendPacket);
						} catch (IOException e) {
							e.printStackTrace();
							System.exit(1);
						}
					}}numberOfTimeout = 0;
				}
				catch(IOException e) {
					e.printStackTrace();
					System.exit(1);
				}}
				
				received = false;
				// create DATA packet after receiving ACK packet
				blockNum = calcBlockNumber();
				data = createDataPacket();
				try {
					sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 23);
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
				
				// check if end of read
				int len = 0;
				//System.out.println(Arrays.toString(data));
				for(byte b: data){
					if(b == 0 && len > 4) break;
					len++;
				}
				//System.out.println(len);
				
				if(len < 512) {
					break;
				}
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
			writer.close();
		} catch (AccessDeniedException e) {
			ErrorPacket fileAccessDenied = new ErrorPacket(ErrorCode.ACCESS_VIOLATION);
			sendErrorPacket(fileAccessDenied);
			System.out.println("Access Violation.");
			System.exit(1);
		} catch (IOException e) {
			ErrorPacket diskFull = new ErrorPacket(ErrorCode.DISK_FULL_OR_ALLOCATION_EXCEEDED);
			sendErrorPacket(diskFull);
			System.out.println("The disk is full.");
			System.exit(1);
		}
		if (data.length < 516) {
			//writer.close();
			System.out.println("Server: File transfer/write complete.");

		}
	}
	
	
	private void sendErrorPacket(ErrorPacket error) {
		DatagramPacket errorPacket;
		try {
			errorPacket = new DatagramPacket(error.getBytes(), error.length(), InetAddress.getLocalHost(), 23);
			sendReceiveSocket.send(errorPacket);
		} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
		}
	}
	
	public boolean checkLegality(DatagramPacket packet) {
		if(packet.getData()[0] != ZERO) return false;
		if(packet.getData()[1] > FOUR) return false;
		else return true;
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
	
	private void checkError(DatagramPacket packet) {

		if(packet.getData()[1] == 5) {
			byte[] message = new byte[packet.getData().length - 5];
			for(int i = 0; i < message.length; i++) {
				if(packet.getData()[4+i] == 0) break;
				message[i] = packet.getData()[4+i];
			}
			System.out.println("Error! " + new String(message,0,message.length));
			System.exit(1);
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
	
}
