package iter2;

public enum ErrorCode {

	FILE_NOT_FOUND(1),
	ACCESS_VIOLATION(2),
	DISK_FULL_OR_ALLOCATION_EXCEEDED(3),
	FILE_ALREADY_EXISTS(6);
	private int opcode;
	
	ErrorCode(int opcode) {
		this.opcode = opcode;
	}
	
	static ErrorCode getErrorCode(int opcode) throws IllegalArgumentException {
		if(opcode == 1) return FILE_NOT_FOUND;
		if(opcode == 2) return ACCESS_VIOLATION;
		if(opcode == 3) return DISK_FULL_OR_ALLOCATION_EXCEEDED;
		if(opcode == 6) return FILE_ALREADY_EXISTS;
		else throw new IllegalArgumentException("Invalid opcode");
	}
	
	Byte[] getOpcode() {
		Byte[] opcodeByte= new Byte[]{0,opcode};
		return opcodeByte;
	}
	
}
