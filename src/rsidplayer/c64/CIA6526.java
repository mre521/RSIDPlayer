package rsidplayer.c64;


public class CIA6526 extends PeripheralDevice {
	
	public CIA6526(int identity, C64System memory) {
		super(identity, memory);
		
		if(identity != C64System.ID_UNK) {
			if(identity == C64System.ID_CIA1) {
				ciaType = CIA1;
			}else
			if(identity == C64System.ID_CIA2) {
				ciaType = CIA2;
			}
		}
	}

	private static final int CIA1 = 1;
	private static final int CIA2 = 2;
	
	private int ciaType;
	private int[] registers;
	private boolean unhandledIRQ;
	
	private int timerA, latchA, controlA;
	private int timerB, latchB, controlB;
	private int intData, intMask;
	
	@SuppressWarnings("unused")
	private int portA, portB;
	
	@Override
	void reset(float clock) {
		registers = new int[0x10];
		unhandledIRQ = false;
		
		timerA = 0;
		latchA = 0;
		controlA = 0x00;
		timerB = 0;
		latchB = 0;
		controlB = 0x00;
		
		intData = 0x00;
		intMask = 0x00;
		
		
		portA = 0x7f;//0b01111111;
		portB = 0x7f;//0b01111111;
	}
	
	@Override
	void clock(int cycles) {
		if(cycles < 1) {
			return;
		}
		
				
		int underflowA = 0;
		@SuppressWarnings("unused")
		int underflowB = 0;
		
		// timer A
		if(((controlA&1) == 1) && ((controlA&0x20) == 0)) {
			timerA -= cycles;
			if(timerA < 0) {
				
				if((controlA&0x8) == 0) {
					do {
						timerA += latchA;
						++underflowA;
					}while((timerA < 0) && (latchA>0));

				}
				else {
					++underflowA;
					controlA &= 0xFE;
				}
				
				//underflowA = 0;
				if(((intMask&1) == 1)) {
					switch(ciaType) {
					case CIA1:
						intData |= 0x80 | 0x01;
						if((intMask & 0x01) != 0)
						memory.intFlags |= C64System.IRQ_CIA1;
						break;
					case CIA2:
						intData |= 0x80 | 0x01;
						if((intMask & 0x01) != 0)
						memory.intFlags |= C64System.NMI_CIA2;
						break;
					}
				}
				
				timerA &= 0xFFFF;
			}
			
		}
		
		// timer B
		
		if(((controlB&1) == 1)) {
			// clock via system clock
			if(((controlB&0x20)==0) && ((controlB&0x40)==0)) {
				timerB -= cycles;
			}
			// clock via timer A underflows
			else if(((controlB&0x20)!=0) && ((controlB&0x40)==0)) {
				timerB -= underflowA;
			}
			
			if(timerB < 0) {
				if((controlB&0x8) == 0) {
					do {
						timerB += latchB;
						++underflowB;
					}while(timerB < 0);
				}
				else {
					controlB &= 0xFE;
				}
				
				if((intMask&2) == 2) {
					switch(ciaType) {
					case CIA1:
						intData |= 0x80 | 0x02;
						if((intMask & 0x02) != 0)
							memory.intFlags |= C64System.IRQ_CIA1;
						break;
					case CIA2:
						intData |= 0x80 | 0x02;
						if((intMask & 0x02) != 0)
							memory.intFlags |= C64System.NMI_CIA2;
						break;
					}
				}
				
				timerB &= 0xFFFF;	
			}
			
			/*if(underflowB == 0 || ((intMask&2) == 0)) {
				switch(ciaType) {
				case CIA1:
					memory.intFlags &= (~C64Mem.IRQ_CIA1)&0xFF;
					break;
				case CIA2:
					memory.intFlags &= (~C64Mem.NMI_CIA2)&0xFF;
					break;
				}
			}*/
		}
		
		/*if( (underflowA == 0)) {
			switch(ciaType) {
			case CIA1:
				memory.intFlags &= (~C64Mem.IRQ_CIA1)&0xFF;
				break;
			case CIA2:
				memory.intFlags &= (~C64Mem.NMI_CIA2)&0xFF;
				break;
			}
		}*/
	}

	@Override
	void write(int address, int data) {
		// TODO Auto-generated method stub
		data &= 0xFF;
		address &= 0xFFFF;
		
		registers[address] = data;
		
		switch(address) {
			case 0x4:
				latchA = (latchA&0xFF00)|data;
				break;
			case 0x5:
				latchA = (latchA&0x00FF)|(data<<8);
				break;
			case 0x6:
				latchB = (latchB&0xFF00)|data;
				break;
			case 0x7:
				latchB = (latchB&0x00FF)|(data<<8);
				break;
			case 0xD:
				if((data&0x80) != 0) {
					intMask |= data;	// set flags
				}
				else {
					intMask &= ~data;	// clear flags
				}
				break;
			case 0xE:
				controlA = data;
				if((controlA&0x10) != 0) {
					timerA = latchA;
				}
				break;
			case 0xF:
				controlB = data;
				if((controlB&0x10) != 0) {
					timerB = latchB;
				}
				break;
				
			default:
				break;
		}
	}

	@Override
	int read(int address) {
		// TODO Auto-generated method stub
		int data = 0;
		address &= 0xFF;
		switch(address) {
			case 0x00:
				data = 0;
				break;
			case 0x01:
				data = portB;
				break;
			case 0x0D:
				data = intData;
				intData = 0;
				switch(ciaType) {
				case CIA1:
					memory.intFlags &= (~C64System.IRQ_CIA1)&0xFF;
					break;
				case CIA2:
					memory.intFlags &= (~C64System.NMI_CIA2)&0xFF;
					break;
				}
				break;
		}
		return data&0xFF;
	}

	@Override
	boolean requestsIRQ() {
		if(unhandledIRQ == true) {
			unhandledIRQ = false;
			return true;
		}
		
		return false;
	}

}
