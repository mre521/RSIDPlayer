package rsidplayer;

public abstract class PeripheralDevice {
	abstract void reset(float clock);
	abstract void clock(int cycles);
	abstract void write(int address, int data);
	abstract int read(int address);
	abstract boolean requestsIRQ();
	
	private String identity;
	protected C64System memory;
	PeripheralDevice(String identity, C64System memory) {
		if(identity == null) {
			this.identity = C64System.ID_UNK;
		}
		else {
			this.identity = identity;
		}
		
		this.memory = memory;
	}
	
	String identify() {
		return identity;
	}
}
