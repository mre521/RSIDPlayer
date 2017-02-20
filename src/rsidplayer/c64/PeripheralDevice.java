package rsidplayer.c64;


public abstract class PeripheralDevice {
	abstract void reset(float clock);
	abstract void clock(int cycles);
	abstract void write(int address, int data);
	abstract int read(int address);
	abstract boolean requestsIRQ();
	
	private int identity;
	protected C64System memory;
	PeripheralDevice(int identity, C64System memory) {
		if( (identity > 5) || (identity == 0) || (identity < -1) ) {
			this.identity = C64System.ID_UNK;
		}
		else {
			this.identity = identity;
		}
		
		this.memory = memory;
	}
	
	int identify() {
		return identity;
	}
}
