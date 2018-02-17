package iter2;

/**
 * Class for ErrorPacket class
 * Constructs an error packet
 * @author jackmacdougall
 *
 */
public class ErrorPacket{
	
	private final byte ZERO = 0x00;
	private final byte FIVE = 0x05;
	
	private byte[] errorPacket = new byte[100];//buffer for the errorPacket
	
	/**
	 * Constructor for ErrorPacket
	 * Calls a function to create the packet
	 * @param code, ErrorCode to determine the type of error for the packet
	 */
	public ErrorPacket(ErrorCode code) {
		createErrorPacket(code);
	}
	
	/**
	 * Creates the ErrorPacket
	 * @param code, ErrorCode to determine the type of error for the packet
	 */
	private void createErrorPacket(ErrorCode code) {
		
		//opcode for evert error packet
		errorPacket[0] = ZERO;
		errorPacket[1] = FIVE;
		
		//error code bytes
		errorPacket[2] = ZERO;
		errorPacket[3] = (byte)code.ordinal();
		
		//convert the code name to bytes
		byte[] message = code.name().getBytes();
		
		//put the message bytes in the packet
		for(int i = 0; i < message.length; i++) {
			errorPacket[4+i] = message[i];
			
			//the last byte will be zero
			if(i == message.length-1) errorPacket[4+i+1] = ZERO;
		}
	}
	
	/**
	 * Getter for the errorPacket
	 * @return byte[] errorPacket, which contains the bytes
	 */
	public byte[] getBytes() {
		return errorPacket;
	}
	
	/**
	 * Getter for the length of the errorPacket
	 * @return int, length of errorPacket
	 */
	public int length() {
		return errorPacket.length;
	}
}
