package iteration4;

public enum ErrorCode {

	FILE_NOT_FOUND(1),
	ACCESS_VIOLATION(2),
	DISK_FULL_OR_ALLOCATION_EXCEEDED(3),
	ILLEGAL_TFTP_OPERATION(4),
	UNKNOWN_TRANSFER_ID(5),
	FILE_ALREADY_EXISTS(6);
	private int code;
	
	ErrorCode(int code) {
		this.code = code;
	}
	
	static ErrorCode getErrorCode(int code) throws IllegalArgumentException {
		if(code == 1) return FILE_NOT_FOUND;
		if(code == 2) return ACCESS_VIOLATION;
		if(code == 3) return DISK_FULL_OR_ALLOCATION_EXCEEDED;
		if(code == 4) return ILLEGAL_TFTP_OPERATION;
		if(code == 5) return UNKNOWN_TRANSFER_ID;
		if(code == 6) return FILE_ALREADY_EXISTS;
		else throw new IllegalArgumentException("Invalid opcode");
	}
	
}
