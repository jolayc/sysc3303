package iter2;

/**
 * Enumeration on class ErrorCode
 * @author jackmacdougall
 *
 */
public enum ErrorCode {

	FILE_NOT_FOUND(1),
	ACCESS_VIOLATION(2),
	DISK_FULL_OR_ALLOCATION_EXCEEDED(3),
	FILE_ALREADY_EXISTS(6);
	private int code; //the number that represents each error type
	
	/**
	 * Constructor for enum ErrorCode
	 * @param code, int that is the ErrorCode's ordinal
	 */
	ErrorCode(int code) {
		this.code = code;
	}
	
	/**
	 * Returns the enum that is bounde to the code
	 * @param code, int to identify the enum
	 * @return ErrorCode that has ordinal int
	 * @throws IllegalArgumentException if code is not bounded to an enum
	 */
	static ErrorCode getErrorCode(int code) throws IllegalArgumentException {
		if(code == 1) return FILE_NOT_FOUND;
		if(code == 2) return ACCESS_VIOLATION;
		if(code == 3) return DISK_FULL_OR_ALLOCATION_EXCEEDED;
		if(code == 6) return FILE_ALREADY_EXISTS;
		
		else throw new IllegalArgumentException("Invalid opcode");
	}
	
}
