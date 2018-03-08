package iteration3;

public class ErrorPacket{
	
	private final byte ZERO = 0x00;
	private final byte FIVE = 0x05;
	
	private byte[] errorPacket = new byte[100];
	
	public ErrorPacket(ErrorCode code) {
		createErrorPacket(code);
	}
	
	private void createErrorPacket(ErrorCode code) {
		errorPacket[0] = ZERO;
		errorPacket[1] = FIVE;
		errorPacket[2] = ZERO;
		errorPacket[3] = (byte)code.ordinal();
		
		byte[] message = code.name().getBytes();
		
		for(int i = 0; i < message.length; i++) {
			errorPacket[4+i] = message[i];
			if(i == message.length-1) errorPacket[4+i+1] = ZERO;
		}
	}
	
	public byte[] getBytes() {
		return errorPacket;
	}
	
	public int length() {
		return errorPacket.length;
	}
}
