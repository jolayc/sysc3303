package iteration3;

public enum PacketType {
	DEFAULT(0),
	RRQ(1),
	WRQ(2),
	DATA(3),
	ACK(4),
	ERROR(5);
	private int code;
	
	PacketType(int code) {
		this.code = code;
	}
	
	static PacketType getPacketType(int code) throws IllegalArgumentException {
		if(code == 0) return DEFAULT;
		if(code == 1) return RRQ;
		if(code == 2) return WRQ;
		if(code == 3) return DATA;
		if(code == 4) return ACK;
		if(code == 5) return ERROR;
		else throw new IllegalArgumentException("Invalid packet type");
	}
}
