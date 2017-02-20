package rsidplayer.c64;
import java.awt.Color;
//import java.awt.color.*;


public class VIC656X extends PeripheralDevice {

	@SuppressWarnings("unused")
	private static final int VID_NTSC = 0;
	@SuppressWarnings("unused")
	private static final int VID_PAL = 1;
	
	// spritees
	int M0X;
	int M0Y;
	int M1X;
	int M1Y;
	int M2X;
	int M2Y;
	int M3X;
	int M3Y;
	int M4X;
	int M4Y;
	int M5X;
	int M5Y;
	int M6X;
	int M6Y;
	int M7X;
	int M7Y;
	
	// control reg 1
	boolean ECM;
	boolean BMM;
	boolean DEN;
	boolean RSEL;
	int YSCROLL;
	
	// raster
	int RASTER;
	
	// lightpen
	int LPX;
	int LPY;
	
	// sprite enables
	boolean M0E;
	boolean M1E;
	boolean M2E;
	boolean M3E;
	boolean M4E;
	boolean M5E;
	boolean M6E;
	boolean M7E;
	
	// control reg 2
	boolean RES;
	boolean MCM;
	boolean CSEL;
	int XSCROLL;
	
	// pointers
	int VM;
	int CB;
	
	// interrupt register
	boolean IRQ;
	boolean ILP;
	boolean IMMC;
	boolean IMBC;
	boolean IRST;
	
	// interrupt enabled
	boolean ELP;
	boolean EMMC;
	boolean EMBC;
	boolean ERST;
	
	// sprite priority
	boolean M0DP;
	boolean M1DP;
	boolean M2DP;
	boolean M3DP;
	boolean M4DP;
	boolean M5DP;
	boolean M6DP;
	boolean M7DP;
	
	// sprite multicolor
	boolean M0MC;
	boolean M1MC;
	boolean M2MC;
	boolean M3MC;
	boolean M4MC;
	boolean M5MC;
	boolean M6MC;
	boolean M7MC;
	
	// sprite-sprite collision
	boolean M0M;
	boolean M1M;
	boolean M2M;
	boolean M3M;
	boolean M4M;
	boolean M5M;
	boolean M6M;
	boolean M7M;
	
	// sprite-data collision
	boolean M0D;
	boolean M1D;
	boolean M2D;
	boolean M3D;
	boolean M4D;
	boolean M5D;
	boolean M6D;
	boolean M7D;
	
	// border color
	int EC;
	
	// BG colors
	int B0C;
	int B1C;
	int B2C;
	int B3C;
	
	// sprite multicolor
	int MM0;
	int MM1;
	
	// sprite color
	int M0C;
	int M1C;
	int M2C;
	int M3C;
	int M4C;
	int M5C;
	int M6C;
	int M7C;
	
	// bus available; 1 = high = true, 0 = low = false
	// cpu is halted when BA = false;
	public int BA;
	
	private static final int NUM_PIXELS_PER_CYCLE = 8;
	
	private static final int NUM_LINES_NTSC = 263;
	private static final int NUM_CYCLES_PER_LINE_NTSC = 65;
	private static final int NUM_PIXELS_PER_LINE_NTSC = NUM_CYCLES_PER_LINE_NTSC * NUM_PIXELS_PER_CYCLE;
	
	private static final int NUM_LINES_PAL = 312;
	private static final int NUM_CYCLES_PER_LINE_PAL = 63;
	private static final int NUM_PIXELS_PER_LINE_PAL = NUM_CYCLES_PER_LINE_PAL * NUM_PIXELS_PER_CYCLE;
	
	int rasterLines;
	int rasterLineCycles;
	int rasterLinePixels;
	int curRasterLine;
	int curRasterCycle;
	
	
	int displayWindowFirstRaster;
	int displayWindowLastRaster;
	
	int displayWindowFirstX;
	int displayWindowLastX;
	
	@SuppressWarnings("unused")
	private Color[] palette = {
		Color.black, 				 Color.white, 				  new Color(0x68, 0x37, 0x2B), new Color(0x70, 0xA4, 0xB2),
		new Color(0x6F, 0x3D, 0x86), new Color(0x58, 0x8D, 0x43), new Color(0x35, 0x28, 0x79), new Color(0xB8, 0xC7, 0x6F),
		new Color(0x6F, 0x4F, 0x25), new Color(0x43, 0x39, 0x00), new Color(0x9A, 0x67, 0x59), new Color(0x44, 0x44, 0x44),
		new Color(0x6C, 0x6C, 0x6C), new Color(0x9A, 0xD2, 0x84), new Color(0x6C, 0x5E, 0xB5), new Color(0x95, 0x95, 0x95)
	};
	public VIC656X(int idViciiPal, C64System memory) {
		super(idViciiPal, memory);
		switch(idViciiPal) {
			case C64System.ID_VICII_NTSC:
			rasterLines = NUM_LINES_NTSC;
				rasterLineCycles = NUM_CYCLES_PER_LINE_NTSC;
				rasterLinePixels = NUM_PIXELS_PER_LINE_NTSC;
				break;
			case C64System.ID_VICII_PAL:
			rasterLines = NUM_LINES_PAL;
				rasterLineCycles = NUM_CYCLES_PER_LINE_PAL;
				rasterLinePixels = NUM_PIXELS_PER_LINE_PAL;
				break;
		}
	}

	@Override
	void reset(float clock) {
		write(0x16, 0xC0);
		write(0x18, 0x01);
		write(0x19, 0x71);
		write(0x1A, 0xF0);
		
		curRasterLine = 0;
		curRasterCycle = 0;
		BA = 1;
	}

	@Override
	//gonna have to make a lut for the graphics drawing
	void clock(int cycles) {
		
		int position = curRasterCycle * NUM_PIXELS_PER_CYCLE;
		
		int endCycle = curRasterCycle + cycles;
		int nextLineCycles = endCycle - rasterLineCycles;
		int endPosition;
				
		// do we have additional cycles for the next line?
		if( nextLineCycles > 0)
		{
			curRasterCycle = nextLineCycles;
			++curRasterLine;//endRaster;
			//curRasterCycle -= rasterLineCycles;
			if(curRasterLine > rasterLines)
			{
				curRasterLine = 0;
			}
			
			//curRasterLine = endRaster;
			
			if(ERST && RASTER==curRasterLine) {
				IRST = true;
				memory.intFlags |= C64System.IRQ_VICII;
			}
			
			
			int curPosition;
			
			// currentRasterLine
			for(curPosition = position; curPosition < rasterLinePixels; ++curPosition) {
				
			}
			
			endPosition = nextLineCycles * NUM_PIXELS_PER_CYCLE;
			// we wrapped to the next one
			for(curPosition = 0; curPosition < endPosition; ++curPosition) {
				
			}
		}
		else
		{
			curRasterCycle = endCycle;
			endPosition = endCycle * NUM_PIXELS_PER_CYCLE;
			// only current raster line we didn't wrap
			for(int curPosition = position; curPosition < endPosition; ++curPosition) {
				
			}
		}
		
		//memory.intFlags &= (~C64Mem.IRQ_VICII)&0xFF;
	/*	if((curRasterCycle+=cycles)>=rasterLineCycles) {
			curRasterCycle-=rasterLineCycles;
			if((++curRasterLine)>rasterLines) {
				curRasterLine = 0;
			}
			if(ERST && RASTER==curRasterLine) {
				IRST = true;
				memory.intFlags |= C64System.IRQ_VICII;
			}
		}*/
		
	}

	@Override
	void write(int address, int data) {
		switch(address) {
		case 0x00:
			M0X 	= data;
			break;
		case 0x01:
			M0Y 	= data;
			break;
		case 0x02:
			M1X 	= data;
			break;
		case 0x03:
			M1Y 	= data;
			break;
		case 0x04:
			M2X 	= data;
			break;
		case 0x05:
			M2Y 	= data;
			break;
		case 0x06:
			M3X 	= data;
			break;
		case 0x07:
			M3Y 	= data;
			break;
		case 0x08:
			M4X 	= data;
			break;
		case 0x09:
			M4Y 	= data;
			break;
		case 0x0A:
			M5X 	= data;
			break;
		case 0x0B:
			M5Y 	= data;
			break;
		case 0x0C:
			M6X 	= data;
			break;
		case 0x0D:
			M6Y 	= data;
			break;
		case 0x0E:
			M7X 	= data;
			break;
		case 0x0F:
			M7Y 	= data;
			break;
		case 0x10:
			M0X 	= (M0X&0xFF)|((data<<1)&0x100);
			M1X 	= (M1X&0xFF)|((data<<1)&0x100);
			M2X 	= (M2X&0xFF)|((data<<1)&0x100);
			M3X 	= (M3X&0xFF)|((data<<1)&0x100);
			M4X 	= (M4X&0xFF)|((data<<1)&0x100);
			M5X 	= (M5X&0xFF)|((data<<1)&0x100);
			M6X 	= (M6X&0xFF)|((data<<1)&0x100);
			M7X 	= (M7X&0xFF)|((data<<1)&0x100);
			break;
		case 0x11:
			RASTER	= (RASTER&0xFF)|((data<<1)&0x100);
			ECM  	= (data&0x40/*0b01000000*/)!=0;
			BMM  	= (data&0x20/*0b00100000*/)!=0;
			DEN  	= (data&0x10/*0b00010000*/)!=0;
			RSEL 	= (data&0x08/*0b00001000*/)!=0;
			YSCROLL = (data&0x04/*0b00000111*/);
			break;
		case 0x12:
			RASTER 	= (RASTER&0x100)|(data&0xFF);
			break;
		case 0x13:
			LPX 	= data;
			break;
		case 0x14:
			LPY 	= data;
			break;
		case 0x15:
			M0E 	= (data&0x01/*0b00000001*/)!=0;
			M1E 	= (data&0x02/*0b00000010*/)!=0;
			M2E 	= (data&0x04/*0b00000100*/)!=0;
			M3E 	= (data&0x08/*0b00001000*/)!=0;
			M4E 	= (data&0x10/*0b00010000*/)!=0;
			M5E 	= (data&0x20/*0b00100000*/)!=0;
			M6E 	= (data&0x40/*0b01000000*/)!=0;
			M7E 	= (data&0x80/*0b10000000*/)!=0;
			break;
		case 0x16:
			RES 	= (data&0x20/*0b00100000*/)!=0;
			MCM 	= (data&0x10/*0b00010000*/)!=0;
			CSEL	= (data&0x08/*0b00001000*/)!=0;
			XSCROLL = (data&0x07/*0b00000111*/);
			break;
		case 0x17:
			/*M0Y 	= (M0X&0xFF)|((data<<1)&0x100);
			M1Y 	= (M1X&0xFF)|((data<<1)&0x100);
			M2Y 	= (M2X&0xFF)|((data<<1)&0x100);
			M3Y 	= (M3X&0xFF)|((data<<1)&0x100);
			M4Y 	= (M4X&0xFF)|((data<<1)&0x100);
			M5Y 	= (M5X&0xFF)|((data<<1)&0x100);
			M6Y 	= (M6X&0xFF)|((data<<1)&0x100);
			M7Y 	= (M7X&0xFF)|((data<<1)&0x100);*/
			break;
		case 0x18:
			VM 		= (VM & 0x3FF)|((data<<6)&0x3C00);
			CB 		= (CB & 0x7FF)|((data<<10)&0x3800);
			break;
		case 0x19:
			IRQ 	= (data&0x80/*0b10000000*/)!=0;
			ILP		= (data&0x08/*0b00001000*/)!=0;
			IMMC	= (data&0x04/*0b00000100*/)!=0;
			IMBC	= (data&0x02/*0b00000010*/)!=0;
			IRST	= (data&0x01/*0b00000001*/)!=0;
			break;
		case 0x1A:
			ELP		= (data&0x08/*0b00001000*/)!=0;
			EMMC	= (data&0x04/*0b00000100*/)!=0;
			EMBC	= (data&0x02/*0b00000010*/)!=0;
			ERST	= (data&0x01/*0b00000001*/)!=0;
			break;
		case 0x1B:
			M0DP 	= (data&0x01/*0b00000001*/)!=0;
			M1DP 	= (data&0x02/*0b00000010*/)!=0;
			M2DP 	= (data&0x04/*0b00000100*/)!=0;
			M3DP 	= (data&0x08/*0b00001000*/)!=0;
			M4DP 	= (data&0x10/*0b00010000*/)!=0;
			M5DP 	= (data&0x20/*0b00100000*/)!=0;
			M6DP 	= (data&0x40/*0b01000000*/)!=0;
			M7DP 	= (data&0x80/*0b10000000*/)!=0;
			break;
		case 0x1C:
			M0MC 	= (data&0x01/*0b00000001*/)!=0;
			M1MC 	= (data&0x02/*0b00000010*/)!=0;
			M2MC 	= (data&0x04/*0b00000100*/)!=0;
			M3MC 	= (data&0x08/*0b00001000*/)!=0;
			M4MC 	= (data&0x10/*0b00010000*/)!=0;
			M5MC 	= (data&0x20/*0b00100000*/)!=0;
			M6MC 	= (data&0x40/*0b01000000*/)!=0;
			M7MC 	= (data&0x80/*0b10000000*/)!=0;
			break;
		case 0x1D:
			/*M0Y 	= (M0X&0xFF)|((data<<1)&0x100);
			M1Y 	= (M1X&0xFF)|((data<<1)&0x100);
			M2Y 	= (M2X&0xFF)|((data<<1)&0x100);
			M3Y 	= (M3X&0xFF)|((data<<1)&0x100);
			M4Y 	= (M4X&0xFF)|((data<<1)&0x100);
			M5Y 	= (M5X&0xFF)|((data<<1)&0x100);
			M6Y 	= (M6X&0xFF)|((data<<1)&0x100);
			M7Y 	= (M7X&0xFF)|((data<<1)&0x100);*/
			break;
		case 0x1E:
			// can not be written
			break;
		case 0x1F:
			// can not be written
			break;
		case 0x20:
			EC 		= data&0x0F;
			break;
		case 0x21:
			B0C 	= data&0x0F;
			break;
		case 0x22:
			B1C 	= data&0x0F;
			break;
		case 0x23:
			B2C 	= data&0x0F;
			break;
		case 0x24:
			B3C 	= data&0x0F;
			break;
		case 0x25:
			MM0		= data&0x0F;
			break;
		case 0x26:
			MM1		= data&0x0F;
			break;
		case 0x27:
			M1C		= data&0x0F;
			break;
		case 0x28:
			M2C		= data&0x0F;
			break;
		case 0x29:
			M3C		= data&0x0F;
			break;
		case 0x2A:
			M4C		= data&0x0F;
			break;
		case 0x2B:
			M5C		= data&0x0F;
			break;
		case 0x2C:
			M6C		= data&0x0F;
			break;
		case 0x2E:
			M7C		= data&0x0F;
			break;
			
		default: break;		
		}
	}

	@Override
	int read(int address) {
		switch(address) {
		case 0x00:
			return M0X;
		case 0x01:
			return M0Y;
		case 0x02:
			return M1X;
		case 0x03:
			return M1Y;
		case 0x04:
			return M2X;
		case 0x05:
			return M2Y;
		case 0x06:
			return M3X;
		case 0x07:
			return M3Y;
		case 0x08:
			return M4X;
		case 0x09:
			return M4Y;
		case 0x0A:
			return M5X;
		case 0x0B:
			return M5Y;
		case 0x0C:
			return M6X;
		case 0x0D:
			return M6Y;
		case 0x0E:
			return M7X;
		case 0x0F:
			return M7Y;
		case 0x10:
			return ((M7X&0x100)>>1) | 
					((M6X&0x100)>>2) | 
					((M5X&0x100)>>3) | 
					((M4X&0x100)>>4) | 
					((M3X&0x100)>>5) |
					((M2X&0x100)>>6) |
					((M1X&0x100)>>7) |
					((M0X&0x100)>>8);
		case 0x11:
			return ((curRasterLine>>1)&0x80) | 
					((ECM)?(1<<6):(0)) |
					((BMM)?(1<<5):(0)) |
					((DEN)?(1<<4):(0)) |
					((RSEL)?(1<<3):(0)) |
					(YSCROLL&0x07);
			//return (curRasterLine>>1)&0x80;
		case 0x12:
			return curRasterLine&0xFF;
		case 0x13:
			return LPX;
		case 0x14:
			return LPY;
		case 0x15:
			return ((M7E)?(1<<7):(0)) | 
					((M6E)?(1<<6):(0)) | 
					((M5E)?(1<<5):(0)) | 
					((M4E)?(1<<4):(0)) | 
					((M3E)?(1<<3):(0)) |
					((M2E)?(1<<2):(0)) |
					((M1E)?(1<<1):(0)) |
					((M0E)?(1):(0));
		case 0x16:
			return ((RES)?(1<<5):(0)) |
					((MCM)?(1<<4):(0)) |
					((CSEL)?(1<<3):(0)) |
					(XSCROLL&0x07);
		case 0x17:
			/*M0Y 	= (M0X&0xFF)|((data<<1)&0x100);
			M1Y 	= (M1X&0xFF)|((data<<1)&0x100);
			M2Y 	= (M2X&0xFF)|((data<<1)&0x100);
			M3Y 	= (M3X&0xFF)|((data<<1)&0x100);
			M4Y 	= (M4X&0xFF)|((data<<1)&0x100);
			M5Y 	= (M5X&0xFF)|((data<<1)&0x100);
			M6Y 	= (M6X&0xFF)|((data<<1)&0x100);
			M7Y 	= (M7X&0xFF)|((data<<1)&0x100);*/
			break;
		case 0x18:
		/*	VM 		= (VM & 0x3FF)|((data<<6)&0x3C00);
			CB 		= (CB & 0x7FF)|((data<<10)&0x3800);*/
			return 0;
		case 0x19:
			int ret = ((IRQ)?(1<<7):(0)) | 
			((ILP)?(1<<3):(0)) |
			((IMMC)?(1<<2):(0)) |
			((IMBC)?(1<<1):(0)) |
			((IRST)?(1):(0));
			IRQ = false;
			ILP = false;
			IMMC = false;
			IMBC = false;
			IRST = false;
			memory.intFlags &= (~C64System.IRQ_VICII)&0xFF;
			return ret;
		case 0x1A:
			return ((ELP)?(1<<3):(0)) |
					((EMMC)?(1<<2):(0)) |
					((EMBC)?(1<<1):(0)) |
					((ERST)?(1):(0));
		case 0x1B:
			return ((M7DP)?(1<<7):(0)) | 
					((M6DP)?(1<<6):(0)) | 
					((M5DP)?(1<<5):(0)) | 
					((M4DP)?(1<<4):(0)) | 
					((M3DP)?(1<<3):(0)) |
					((M2DP)?(1<<2):(0)) |
					((M1DP)?(1<<1):(0)) |
					((M0DP)?(1):(0));
		case 0x1C:
			return ((M7MC)?(1<<7):(0)) | 
					((M6MC)?(1<<6):(0)) | 
					((M5MC)?(1<<5):(0)) | 
					((M4MC)?(1<<4):(0)) | 
					((M3MC)?(1<<3):(0)) |
					((M2MC)?(1<<2):(0)) |
					((M1MC)?(1<<1):(0)) |
					((M0MC)?(1):(0));
		case 0x1D:
			/*M0Y 	= (M0X&0xFF)|((data<<1)&0x100);
			M1Y 	= (M1X&0xFF)|((data<<1)&0x100);
			M2Y 	= (M2X&0xFF)|((data<<1)&0x100);
			M3Y 	= (M3X&0xFF)|((data<<1)&0x100);
			M4Y 	= (M4X&0xFF)|((data<<1)&0x100);
			M5Y 	= (M5X&0xFF)|((data<<1)&0x100);
			M6Y 	= (M6X&0xFF)|((data<<1)&0x100);
			M7Y 	= (M7X&0xFF)|((data<<1)&0x100);*/
			break;
		case 0x1E:
			// can not be written
			break;
		case 0x1F:
			// can not be written
			break;
		case 0x20:
			return EC;
		case 0x21:
			return B0C;
		case 0x22:
			return B1C;
		case 0x23:
			return B2C;
		case 0x24:
			return B3C;
		case 0x25:
			return MM0;
		case 0x26:
			return MM1;
		case 0x27:
			return M1C;
		case 0x28:
			return M2C;
		case 0x29:
			return M3C;
		case 0x2A:
			return M4C;
		case 0x2B:
			return M5C;
		case 0x2C:
			return M6C;
		case 0x2E:
			return M7C;
			
		default: break;		
		}
		return 0;
	}

	@Override
	boolean requestsIRQ() {
		if(IRQ) {
			IRQ = false;
			return true;
		}
		return false;
	}

}
