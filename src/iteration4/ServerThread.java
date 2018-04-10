package iteration4;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * A server thread that will handle read/write requests received by server, sent by client 
 */

public class ServerThread implements Runnable {
	
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket receivePacket, sendPacket;
	// The directory where files will be written to
	private String relativePath = System.getProperty("user.dir");
	private Writer writer;
	private int[] blockNumber;
	
	// String identifiers
	private String message;
	private File file;
	
	private byte[] path;
	private int[] blockNum, oldNum;
	private int numberOfTimeout=0;
	private int receivePort;
	private int sendPort;
	
	private boolean done = false;
	private boolean read = false;
	private boolean write = false;
	private boolean multi;
	
	private final byte ZERO = 0x00;
	private final byte ONE = 0x01;
	private final byte TWO = 0x02; 
	private final byte FOUR = 0x0;
	private final byte FIVE = 0x05;
	private final int errorSim = 23;

	/**
	 * Constructor for ServerThread
	 * @param receivePacket packet received from host
	 * @param path is the file name that the server writing to 
	 * @param message is the read or write request
	 * @param blockNumber is the current block number of the file 
	 */

	public ServerThread(DatagramPacket receivePacket, byte[] path, File file, String message, int[] blockNumber, boolean multi) {
		this.message = message;
		this.receivePacket = receivePacket;
		this.file = file;
		this.path = path;
		this.blockNumber = blockNumber;
		this.multi = multi;
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
		
		receivePort = receivePacket.getPort();
		
		if(!multi) sendPort = errorSim;
		else sendPort = receivePort;
		
		if(data[0] == 0 && data[1] == 1) handleRead();
		else if(data[0] == 0 && data[1] == 2) handleWrite();
		sendReceiveSocket.close();
	}
	
	/**
	 * For handling write requests
	 */
	private void handleWrite() {
		// Response packet
		byte[] response = new byte[512 + 4];
		byte[] data;
		
		// Status flags
		boolean finished = false;
		boolean emptyDataReceived = false;
		
		// Send ACK to write request
		response = createACKPacket();
		try {
			sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), sendPort);
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
			receivePacket.setData(data);
			// receive packet from Client
			try { 
				sendReceiveSocket.receive(receivePacket);	
				checkPort(receivePacket);
				checkError(receivePacket);
				checkLegality(receivePacket);
			}
			catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// empty DATA block is received so transfer is over after final ACK
			if(emptyDataReceived) finished = true;
			
			// check if empty DATA packet (finished transferring)
			// e.g. receivePacket = [0, 3, 0 ... 0]
			if (receivePacket.getData()[1] == 3 && receivePacket.getData()[2] == 0
					&& receivePacket.getData()[515] == 0) {
				emptyDataReceived = true;
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
				sendPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), sendPort);
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

			// check if finished
			if(finished) break;
		}
	}
	
	/**
	 * For handling read requests
	 */
	private void handleRead() {
		try {
			sendReceiveSocket.setSoTimeout(10000);
		} catch (SocketException e1) {

			e1.printStackTrace();
			System.exit(1);
		}

		// Status flags
		boolean received = false;
		boolean sentEmptyData = false;
		boolean finished = false;

		// response packet
		byte[] data = new byte[512 + 4];
		byte[] response;

		// send DATA to read request
		data = createDataPacket();
		try {
			sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), sendPort);
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

			while (!received) {
				try {
					sendReceiveSocket.receive(receivePacket);
					checkPort(receivePacket);
					checkError(receivePacket);
					checkLegality(receivePacket);
					received = true;
				} catch (SocketTimeoutException se) {
					while (numberOfTimeout < 2) {
						numberOfTimeout++;
						if (numberOfTimeout == 2) {
							try {
								sendReceiveSocket.send(sendPacket);
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
						}
					}
					numberOfTimeout = 0;
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}

			received = false;
			
			if(sentEmptyData) {
				break;
			}
			// create DATA packet after receiving ACK packet
			blockNum = calcBlockNumber();
			if(!finished) {
				data = createDataPacket();
			} else {
				data = createEmptyDataPacket();
				sentEmptyData = true;
			}
			
			// Create and send DATA packet to Client
			try {
				sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), sendPort);
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
			
			if(!sentEmptyData) { 
				int len = 0;
				for (byte b : data) {
					if (b == 0 && len > 4) break;
					len++;
				}
				if (len < 512) finished = true;
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
	
	/**
	 * Creates an empty DATA packet filled with 0s
	 * @return	byte[] empty DATA packet (byte array)
	 */
	private byte[] createEmptyDataPacket() {
		byte[] data = new byte[516];
		data[0] = 0;
		data[1] = 3;
		for (int i = 2; i < data.length; i++) data[i] = 0;
		return data;
	}
	
	/**
	 * Sends an error packet
	 * @param error, ErrorPacket that wil be sent
	 */
	private void sendErrorPacket(ErrorPacket error) {
		DatagramPacket errorPacket;
		try {
			errorPacket = new DatagramPacket(error.getBytes(), error.length(), InetAddress.getLocalHost(), sendPort);
			sendReceiveSocket.send(errorPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Checks the legality of a packet
	 * @param packet, DatagramPacket that will be checked
	 */
	public void checkLegality(DatagramPacket packet) {
		
		System.out.println("packet length: " + packet.getData().length);
		//checks if packet size is too big
		if(packet.getData().length > 516){
			ErrorPacket tooBig = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
			sendErrorPacket(tooBig);
			System.out.println("Server Received Illegal TFTP Operation.");
			System.exit(1);
		}
				
		//checks if packet size is too small
		if(packet.getData().length < 4){
			ErrorPacket tooSmall = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
			sendErrorPacket(tooSmall);
			System.out.println("Server Received Illegal TFTP Operation.");
			System.exit(1);
		}
	
		//checks for bad opcode
		if(packet.getData()[0] != ZERO || packet.getData()[1] > FIVE) {
			ErrorPacket illegalOperation = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
			sendErrorPacket(illegalOperation);
			System.out.println("Server Recevied Illegal TFTP Operation.");
			System.exit(1);
		}
		
		//checks for wrong block number for packets in a write
		if((((packet.getData()[2]*10) + packet.getData()[3]) > ((blockNumber[0] * 10) + blockNumber[1] + 1)) && write == true){
			ErrorPacket illegalBlockNumber = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
			sendErrorPacket(illegalBlockNumber);
			System.out.println("Server Received Illegal Block Number.");
			System.exit(1);
		}
		
		//checks for wrong block number for packets in a write
		if((((packet.getData()[2]*10) + packet.getData()[3]) > ((blockNumber[0] * 10) + blockNumber[1])) && read == true){
			ErrorPacket illegalBlockNumber = new ErrorPacket(ErrorCode.ILLEGAL_TFTP_OPERATION);
			sendErrorPacket(illegalBlockNumber);
			System.out.println("Server Received Illegal Block Number.");
			System.exit(1);
		}
	}
	
	/**
	 * increments block number 
	 * @return block number as array on integers
	 */
	
	private int[] calcBlockNumber(){

		//add number in tenth's place
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
	 * checks if the packet is an error packet
	 * if it is, it then prints it's message
	 * @param packet, DatagramPacket that will be checked
	 */
	private void checkError(DatagramPacket packet) {

		//check if error packet
		if(packet.getData()[1] == 5) {
			byte[] message = new byte[packet.getData().length - 5];
			
			//extracting message
			for(int i = 0; i < message.length; i++) {
				if(packet.getData()[4+i] == 0) break;
				message[i] = packet.getData()[4+i];
			}
			System.out.println("Error! " + new String(message,0,message.length));
			System.exit(1);
		}
	}
	
	/**
	 * checks if the port where the receive packet came from is known
	 * @param receive, the DatagramPacket whose port will be checked
	 */
	private void checkPort(DatagramPacket receive){

		//if the port is from an unknown source
		if(receive.getPort() != receivePort){
			ErrorPacket wrongPort = new ErrorPacket(ErrorCode.UNKNOWN_TRANSFER_ID);
			sendErrorPacket(wrongPort);
			System.out.println("Packet received from unknown port.");
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
	
	/**
	 * Creates an acknowledge packet containing the number in block
	 * @param block, int[] containing clock number
	 * @return byte[4] containing acknowledge packet
	 */
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
