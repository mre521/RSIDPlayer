package rsidplayer.c64;

import java.io.IOException;
import java.util.ArrayList;



// implementation of the C64 system from its components
public class C64System extends MemAccess {
	public static final int ID_VICII_NTSC = 1;//"VICII_NTSC";
	public static final int ID_VICII_PAL = 2;//"VICII_PAL";
	public static final int ID_SID = 3;//"SID";
	public static final int ID_CIA1 = 4;//"CIA1";
	public static final int ID_CIA2 = 5;//"CIA2";
	static final int ID_UNK = -1;//"UNKNOWN";
	
	public static final int DDR_6510 = 0x0000;
	public static final int PORT_6510 = 0x0001;
	
	private byte[] ram;	// system ram
	
	private static final byte PLA_ZONE0 = 0;
	private static final byte PLA_ZONE1 = 1;
	private static final byte PLA_ZONE2 = 2;
	private static final byte PLA_ZONE3 = 3;
	private static final byte PLA_ZONE4 = 4;
	private static final byte PLA_ZONE5 = 5;
	private static final byte PLA_ZONE6 = 6;
	
	private static final byte PLA_MAP_RAM = 0;
	private static final byte PLA_MAP_BASIC = 1;
	private static final byte PLA_MAP_KERNAL = 2;
	private static final byte PLA_MAP_CHARROM = 3;
	private static final byte PLA_MAP_IO = 4;
	private static final byte PLA_MAP_CARTL = 5;
	private static final byte PLA_MAP_CARTH = 6;
	private static final byte PLA_MAP_OPEN = 7;
	
	// look up table to emulate the PLA mappings for all values possible
	private static final byte[][] pla_lut = {
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_BASIC,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_KERNAL},
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_BASIC,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_KERNAL},	//1
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_BASIC,  PLA_MAP_RAM, PLA_MAP_CHARROM, PLA_MAP_KERNAL},	//2
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_RAM},		//3
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,  PLA_MAP_RAM, PLA_MAP_CHARROM, PLA_MAP_RAM},		//4
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,  PLA_MAP_RAM, 	 PLA_MAP_RAM, PLA_MAP_RAM},		//5
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_KERNAL},	//6
		{PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,   PLA_MAP_RAM,  PLA_MAP_RAM, PLA_MAP_CHARROM, PLA_MAP_KERNAL},	//7
		{PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_CARTL, PLA_MAP_BASIC,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_KERNAL},	//8
		{PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_CARTL, PLA_MAP_BASIC,  PLA_MAP_RAM, PLA_MAP_CHARROM, PLA_MAP_KERNAL},	//9
		{PLA_MAP_RAM,	PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_CARTH,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_KERNAL},	//10
		{PLA_MAP_RAM,	PLA_MAP_RAM,   PLA_MAP_RAM, PLA_MAP_CARTH,  PLA_MAP_RAM, PLA_MAP_CHARROM, PLA_MAP_KERNAL},	//11
		{PLA_MAP_RAM, 	PLA_MAP_RAM, PLA_MAP_CARTL, PLA_MAP_CARTH,  PLA_MAP_RAM, 	  PLA_MAP_IO, PLA_MAP_KERNAL},	//12
		{PLA_MAP_RAM, 	PLA_MAP_RAM, PLA_MAP_CARTL, PLA_MAP_CARTH,  PLA_MAP_RAM, PLA_MAP_CHARROM, PLA_MAP_KERNAL},	//13
		{PLA_MAP_RAM,  PLA_MAP_OPEN, PLA_MAP_CARTL,  PLA_MAP_OPEN, PLA_MAP_OPEN, 	  PLA_MAP_IO, PLA_MAP_CARTH}	//14
	};
	private static byte[] pla_table = new byte[7];
	private boolean pla_line_game;
	private boolean pla_line_extrom;
	private byte[] pla; // maps certain ram addresses into sections
	private byte[] io_lookup; // maps the IO to certain address ranges
	
	private ArrayList<PeripheralDevice> devices = new ArrayList<PeripheralDevice>();
	
	PeripheralDevice vicII;
	PeripheralDevice sid;
	PeripheralDevice cia1, cia2;
	
	int lastclock;
	
	public C64System() {
		ram = new byte[0x10000];
		pla = new byte[0x10000];
		io_lookup = new byte[0x10000];
		pla_init();
		io_init();
		intFlags = 0;
	}
	
	public void addDevice(PeripheralDevice dev) {
		if(dev != null) {
			devices.add(dev);
		}
	}
	
	// create pla zone table
	void pla_init() {
		for(int addr = 0; addr < 0x10000; ++addr) {
			if(addr <= 0x0FFF) {
				pla[addr] = PLA_ZONE0;
			} 
			else
			if(addr <= 0x7FFF) {
				pla[addr] = PLA_ZONE1;
			} 
			else
			if(addr <= 0x9FFF) {
				pla[addr] = PLA_ZONE2;
			}
			else
			if(addr <= 0xBFFF) {
				pla[addr] = PLA_ZONE3;
			}
			else
			if(addr <= 0xCFFF) {
				pla[addr] = PLA_ZONE4;
			}
			else
			if(addr <= 0xDFFF) {
				pla[addr] = PLA_ZONE5;
			}
			else
			if(addr <= 0xFFFF) {
				pla[addr] = PLA_ZONE6;
			}
		}
		
		pla_line_game = true;
		pla_line_extrom = true;
		
		//pla_write(0);
	}

	void pla_write(int bits) {
		bits = (bits&0x07/*0b111*/)<<2;
		bits |= ((pla_line_game)?(1):(0))<<1;
		bits |= (pla_line_extrom)?(1):(0);
		
		/* format for bits:
		 *	 4		 3		2		1		0
		 * /CHAREN /HIRAM  /LORAM  /GAME   /EXTROM
		 */
		
		switch(bits) {
		case 0x1f/*0b11111*/: // 1
			pla_table = pla_lut[1];
			break;
		case 0x0f/*0b01111*/: // 2
			pla_table = pla_lut[2];
			break;
		case 0x16/*0b10110*/: // 3
		case 0x17/*0b10111*/:
		case 0x14/*0b10100*/:
			pla_table = pla_lut[3];
			break;
		case 0x06/*0b00110*/: // 4
		case 0x07/*0b00111*/:
			pla_table = pla_lut[4];
			break;
		case 0x04/*0b00100*/: // 5
		case 0x02/*0b00010*/:
		case 0x13/*0b10011*/:
			pla_table = pla_lut[5];
			break;
		case 0x1a/*0b11010*/: // 6
		case 0x1b/*0b11011*/:
		case 0x10/*0b10000*/:
		case 0x12/*0b10010*/:
			pla_table = pla_lut[6];
			break;
		case 0x0a/*0b01010*/: // 7
		case 0x0b/*0b01011*/:
		case 0x00/*0b00000*/:
		//case 0b00010:
			pla_table = pla_lut[7];
			break;
		case 0x1e/*0b11110*/: // 8
			pla_table = pla_lut[8];
			break;
		case 0x0e/*0b01110*/: // 9
			pla_table = pla_lut[9];
			break;
		case 0x18/*0b11000*/: // 10
			pla_table = pla_lut[10];
			break;
		case 0x08/*0b01000*/: // 11
			pla_table = pla_lut[11];
			break;
		case 0x1c/*0b11100*/: // 12
			pla_table = pla_lut[12];
			break;
		case 0x0c/*0b01100*/: // 13
			pla_table = pla_lut[13];
			break;
		case 0x01/*0b00001*/: // 14
		case 0x05/*0b00101*/:
		case 0x09/*0b01001*/:
		case 0x0d/*0b01101*/:
		case 0x11/*0b10001*/:
		case 0x15/*0b10101*/:
		case 0x1d/*0b11101*/:
			pla_table = pla_lut[14];
			break;
		}
	}
	
	final byte IO_SID1 = 1;
	final byte IO_VICII = 2;
	final byte IO_CIA1 = 3;
	final byte IO_CIA2 = 4;
	final byte IO_RAM = 5;
	
	//generate io read/write lookup table
	void io_init() {
		for(int address = 0; address < 65536; ++address)
		{
			if((address >= 0xD400) && (address <= 0xD41F)) {
				io_lookup[address] = IO_SID1;
			}else
			if((address >= 0xD000) && (address <= 0xD02E)) {
				io_lookup[address] = IO_VICII;
			}else
			if((address >= 0xDC00) && (address <= 0xDC0F)) {
				io_lookup[address] = IO_CIA1;
			}else
			if((address >= 0xDD00) && (address <= 0xDD0F)) {
				io_lookup[address] = IO_CIA2;
			}else
				io_lookup[address] = IO_RAM;
		}
	}
	
	public void initialize(float clock) throws IOException {
		for(PeripheralDevice device : devices) {
			String warning = null;
			device.reset(clock);
			
			switch(device.identify()) {
				case ID_VICII_NTSC:
				case ID_VICII_PAL:
					if(vicII == null)
						vicII = device;
					else
						warning = "VICII";
					break;
				case ID_SID:
					if(sid == null)
						sid = device;
					else
						warning = "SID";//ID_SID;
					break;
				case ID_CIA1:
					if(cia1 == null)
						cia1 = device;
					else
						warning = "CIA1";//ID_CIA1;
					break;
				case ID_CIA2:
					if(cia2 == null)
						cia2 = device;
					else
						warning = "CIA2";//ID_CIA2;
					break;
				default:
					System.out.println("Warning: Device with unknown ID: " +device.identify()+ " not added to bus");
					break;
			}
			
			if(warning != null) {
				System.out.println("Warning: Device with ID " + warning + " not added to bus; already present.");
			}
		}
		
		if(vicII == null) {
			throw new IOException("No VIC-II added to C64 system");
		}
		if(sid == null) {
			throw new IOException("No SID added to C64 system");	
		}
		if(cia1 == null) {
			throw new IOException("No CIA1 added to C64 system");
		}
		if(cia2 == null) {
			throw new IOException("No CIA2 added to C64 system");
		}
	}
	
	void clock(int cycles) {
		vicII.clock(cycles);
		sid.clock(cycles);
		cia1.clock(cycles);
		cia2.clock(cycles);
		
		lastclock = cycles;
	}
	
	public int lastClockPeriod() {
		return lastclock;
	}
	
	public static final int IRQ_VICII = 0x01;
	public static final int IRQ_CIA1 = 0x02;
	public static final int NMI_CIA2 = 0x04;
	public static final int NMI_CIA2_PREV = 0x08;
	int intFlags;
	
	boolean IRQ() {
		return (intFlags&(IRQ_VICII|IRQ_CIA1))!=0;
	}
	
	boolean NMI() {
		boolean nmiState = ((intFlags&NMI_CIA2)==NMI_CIA2);
		boolean nmiStatePrev = ((intFlags&NMI_CIA2_PREV)==NMI_CIA2_PREV);
		boolean triggerNMI = false;
		
		if((nmiState==true) && (nmiStatePrev==false)) {
			triggerNMI = true;
		}
		
		intFlags = (intFlags&(0xFF&(~NMI_CIA2_PREV))) | ((intFlags<<1)&NMI_CIA2_PREV);
		
		return triggerNMI;
	}
	
	boolean haltCPU() {
		return ((VIC656X)vicII).BA == 0;
	}
	
	@Override
	public
	// uses huge switch case with lookup tables to speed up execution
	void WriteMemory(int address, int value) {
		address &= 0xFFFF;
		switch(pla[address]) {
		case PLA_ZONE0:
			ram[address] = (byte) (value&0xFF);
			if(address == PORT_6510)
				pla_write(value&ram[DDR_6510]);
			break;
		case PLA_ZONE1:
			switch(pla_table[PLA_ZONE1]) {
			case PLA_MAP_RAM:
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_OPEN:
				break;
			}
			break;
		case PLA_ZONE2:
			switch(pla_table[PLA_ZONE2]) {
			case PLA_MAP_RAM:
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_CARTL:
				// write to RAM under the ROM
				ram[address] = (byte) (value&0xFF);
				break;
			}
			break;
		case PLA_ZONE3:
			switch(pla_table[PLA_ZONE3]) {
			case PLA_MAP_RAM:
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_CARTH:
				// write to RAM under the ROM
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_BASIC:
				// write to RAM under the ROM
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_OPEN:
				break;
			}
			break;
		case PLA_ZONE4:
			switch(pla_table[PLA_ZONE4]) {
			case PLA_MAP_RAM:
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_OPEN:
				break;
			}
			break;
		case PLA_ZONE5:
			switch(pla_table[PLA_ZONE5]) {
			case PLA_MAP_IO:
				switch(io_lookup[address]) {
				case IO_SID1:
						sid.write(address&0xFF, value&0xFF);
						break;
				case IO_VICII:
						vicII.write(address&0xFF, value&0xFF);
						break;
				case IO_CIA1:
						cia1.write(address&0xFF,  value&0xFF);
						break;
				case IO_CIA2:
						cia2.write(address&0xFF,  value&0xFF);
						break;
				default:
					ram[address] = (byte) (value&0xFF);
					break;
				}
				break;
			case PLA_MAP_CHARROM:
				ram[address] = (byte) (value&0xFF);
				break;
			}
			
			break;
		case PLA_ZONE6:
			switch(pla_table[PLA_ZONE6]) {
			case PLA_MAP_RAM:
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_KERNAL:
				ram[address] = (byte) (value&0xFF);
				break;
			case PLA_MAP_CARTH:
				ram[address] = (byte) (value&0xFF);
				break;
			}
			break;
		}
		
		//if(address == 0x01)
		//	System.out.println("6510 Port set to $" + Integer.toHexString(value));
	}

	@Override
	public
	int ReadMemory(int address) {
		address &= 0xFFFF;
	
		switch(pla[address]) {
		case PLA_ZONE0:
			return ram[address]&0xFF;
		//	break;
		case PLA_ZONE1:
			switch(pla_table[PLA_ZONE1]) {
			case PLA_MAP_RAM:
				return ram[address]&0xFF;
			//	break;
			case PLA_MAP_OPEN:
				return 0xFF;
			//	break;
			}
			break;
		case PLA_ZONE2:
			switch(pla_table[PLA_ZONE2]) {
			case PLA_MAP_RAM:
				return ram[address]&0xFF;
			//	break;
			case PLA_MAP_CARTL:
				// read from cartL
				return 0xFF;
			//	break;
			}
			break;
		case PLA_ZONE3:
			switch(pla_table[PLA_ZONE3]) {
			case PLA_MAP_RAM:
				return ram[address]&0xFF;
			//	break;
			case PLA_MAP_CARTH:
				// read from cart high memory
				return 0xFF;
			//	break;
			case PLA_MAP_BASIC:
				// read from BASIC rom
				return Basic.data[address&0x1FFF];
			//	break;
			case PLA_MAP_OPEN:
				return 0xFF;
			//	break;
			}
			break;
		case PLA_ZONE4:
			switch(pla_table[PLA_ZONE4]) {
			case PLA_MAP_RAM:
				return ram[address]&0xFF;
			//	break;
			case PLA_MAP_OPEN:
				return 0xFF;
			//	break;
			}
			break;
		case PLA_ZONE5:
			switch(pla_table[PLA_ZONE5]) {
			case PLA_MAP_IO:
				switch(io_lookup[address]) {
				case IO_SID1:
					return sid.read(address&0xFF);
				case IO_VICII:
					return vicII.read(address&0xFF);
				case IO_CIA1:
					return cia1.read(address&0xFF);
				case IO_CIA2:
					return cia2.read(address&0xFF);
				default:
					return ram[address];
				}
				//break;
			//	break;
			case PLA_MAP_CHARROM:
				// read from character rom
				return CharRom.data[address&0x0FFF];
			//	break;
			}
			
			break;
		case PLA_ZONE6:
			switch(pla_table[PLA_ZONE6]) {
			case PLA_MAP_RAM:
				return ram[address]&0xFF;
			//	break;
			case PLA_MAP_KERNAL:
				// read from KERNAL rom
				return Kernal.data[address&0x1FFF];
			//	break;
			case PLA_MAP_CARTH:
				// read from card high memory
				return 0xFF;
			//	break;
			}
			break;
		}
		// somehow the address isn't mapped, so just read ram
		return ram[address]&0xFF;
	}
}
