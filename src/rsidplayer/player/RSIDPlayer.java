package rsidplayer.player;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.io.IOException;

import javax.swing.JFrame;
import rsidplayer.c64.C64System;
import rsidplayer.c64.CIA6526;
import rsidplayer.c64.CPU6510;
import rsidplayer.c64.CPU6510.CPUBreakException;
import rsidplayer.c64.SID6581;
import rsidplayer.c64.SID8580;
import rsidplayer.c64.SIDAbstract;
import rsidplayer.c64.VIC656X;

public class RSIDPlayer {
	// sampling rate to play at (Hz)
	int sampleRate = 192000;
	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}
	// CIA1 TimerA stuff
	//private static final int CIA1_TIMERA = 0xDC04;				// address of CIA1 timerA low byte
	//private static final int CIA1_TIMER_DEFAULT_NTSC = 0x4295;	// ~60 Hz at NTSC clock rate
	//private static final int CIA1_TIMER_DEFAULT_PAL = 0x4025; 	// ~60 Hz at PAL clock rate

	// C64 master clock (Hz):
	private static final float C64_CLOCK_NTSC = 1022727.14f;	// NTSC
	private static final float C64_CLOCK_PAL = 985248.4f;		// PAL

	// video standard flags
	private static final int PAL = 0;
	private static final int NTSC = 1;
	
	int videoStandard;
	
	
	// timing source flags
	private static final int VBI = 0;
	private static final int CIA = 1;
	
	int timingSource;
	
	
	// the clock speed of this player's system
	float clock_speed = C64_CLOCK_PAL;	// PAL is default, most tunes are
	
	// .sid file which this player has loaded
	public SIDFile sidFile;
	
	// the components of the c64 system
	//---------------------
	private CPU6510 cpu;
	C64System c64;
	VIC656X vic;
	SIDAbstract sid1;
	CIA6526 cia1, cia2;
	//---------------------
	
	// SID model flags
	private static final int SID_8580 = 0;
	private static final int SID_6581 = 1;
	// which SID model the song would like to use
	int preferredSID;
	
	public RSIDPlayer() {
		sid1 = new SID6581(C64System.ID_SID, c64);
		graphicsInit(sid1);
	}
	
	public CPU6510 getCpu() {
		return cpu;
	}

	public void setCpu(CPU6510 cpu) {
		this.cpu = cpu;
	}

	public void showVisuals() {
		graphicsWindowFrame.setVisible(true);
	}
	
	public void hideVisuals() {
		graphicsWindowFrame.setVisible(false);
	}
	
	class PlayThread implements Runnable {
		
		byte[] buffer;
		float cyclesPerSample;
		SimpleSoundOutput output;
		CPU6510 cpu;
		C64System c64;
		SIDAbstract sid;
		boolean playing = true;
		
		PlayThread(byte[] buffer, float cyclesPerSample, SimpleSoundOutput output, CPU6510 cpu, C64System c64, SIDAbstract sid) {
			this.buffer = buffer;
			this.cyclesPerSample = cyclesPerSample;
			this.output = output;
			this.cpu = cpu;
			this.c64 = c64;
			this.sid = sid;
		}
		public void stopIt() {playing = false;}
		
		@Override
		public void run() {
			float cycles = 0;
			int prevsamp = 0;
			while(playing) {
				int height = graphicsWindowDisplay.getHeight();
				int height_2 = graphicsWindowDisplay.getHeight()/2;
				int width = graphicsWindowDisplay.getWidth();
				//int width_2 = graphicsWindowDisplay.getWidth()/2;
				
				float h = (float)height/(65536.0f/2.0f);
				float w = ((float)width/(float)(buffer.length>>1));
				
				displayGraphics = (Graphics2D)displayBufferStrategy.getDrawGraphics();
				displayGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				displayGraphics.setColor(Color.black);
				
				displayGraphics.fillRect(0,  0, width, height);
				
				displayGraphics.setColor(Color.white);
				for(int i = 0; i < buffer.length>>1; ++i) {
					while(cycles < cyclesPerSample) {
						try {
							cpu.InstructionStep();
						} catch (CPUBreakException e) {
							e.printStackTrace();
							return;
						}
						cycles +=c64.lastClockPeriod();
					}
					
					cycles -= cyclesPerSample;
					
					int sample = sid.outputSample((int)(cyclesPerSample));
					buffer[i*2] = (byte)((sample>>8)&0xFF);
					buffer[(i*2)+1] = (byte)((sample)&0xFF);
					
					displayGraphics.drawLine((int)(w * i), (int)(height_2+prevsamp * h),  (int)(w * (i+1.0f)), (int)(height_2+sample * h)); 
					prevsamp = sample;
				}
				
				//g.setTransform(prev);
				
				displayGraphics.setColor(Color.green);
				//int i= 0;
				for(int z = 0; z < 3; ++z) {
					SIDAbstract.SIDVoice v = sid.voice[z];
					displayGraphics.drawString("OSC " + z + ": " + "Gate = " + ((v.control&0x01)==0x01) + ", Sync = " + ((v.control&0x02)==0x02) + ", RingMod = " + ((v.control&0x04)==0x04) + ", Test = " + ((v.control&0x08)==0x08) + ", Freq = " + v.freq + (((v.control&0x10)==0x10)?" TRI":" ") + (((v.control&0x20)==0x20)?" SAW":" ") + (((v.control&0x40)==0x40)?" PULSE":"") + (((v.control&0x80)==0x80)?" NOISE":""), 10, 100 + 20*z);
				}
				
				displayGraphics.drawString("Filter: " + "FC = " + (int)sid.getActualCutoff() + "Hz, " + "Res = " + (sid.rFilt>>4) + ", " + (((sid.rVolFilt&0x10)==0x10)?" LP":" ") + (((sid.rVolFilt&0x20)==0x20)?" BP":" ") + (((sid.rVolFilt&0x40)==0x40)?" HP":" ") , 10, 100 + 20*4);
			//	++i;
				displayBufferStrategy.show();
				displayGraphics.dispose();
				
				output.writeSamples(buffer, buffer.length);
				
			}
		}
		
	}
	
	SimpleSoundOutput output;
	PlayThread playThread;
	Thread t;
	
	public void setSID(SIDFile file) throws IOException {

		sidFile = file;
		if(file == null) throw new IOException("Please don't give a null SIDFile!");
		
		videoStandard = PAL;
		timingSource = VBI;
		
		if (((sidFile.speed >> (sidFile.startSong - 1)) & 1) == 1) {
			videoStandard = PAL;
			timingSource = CIA;
		} else {
			videoStandard = PAL;
			timingSource = VBI;
		}

		if (((sidFile.flags >> 2) & 0x3) != 2) {
			videoStandard = PAL;
		} else {
			videoStandard = NTSC;
		}

		if (videoStandard == NTSC) {
			clock_speed = C64_CLOCK_NTSC;
		} else {
			clock_speed = C64_CLOCK_PAL;
		}
		
		if(((sidFile.flags>>4)& 0x3) == 0x02/*0b10*/)
			preferredSID = SID_8580;
		else
			preferredSID = SID_6581;
	}
	
	public synchronized void initTune(int song) throws Exception {
		if(sidFile == null) {
			throw new Exception("Load a SID file first !");
		}
		
		
		c64 = new C64System();
		if(preferredSID == SID_8580)
			sid1 = new SID8580(C64System.ID_SID, c64);
		else
			sid1 = new SID6581(C64System.ID_SID, c64);
		
		graphicsKeyListener.updateSIDObject(sid1);
		
		c64.addDevice(sid1);
		cia1 = new CIA6526(C64System.ID_CIA1, c64); c64.addDevice(cia1);
		cia2 = new CIA6526(C64System.ID_CIA2, c64); c64.addDevice(cia2);
		
		if(videoStandard == NTSC) {
			vic = new VIC656X(C64System.ID_VICII_NTSC, c64);
		} else
		if(videoStandard == PAL) {
			vic = new VIC656X(C64System.ID_VICII_PAL, c64);
		}
		else {
			vic = new VIC656X(C64System.ID_VICII_PAL, c64);
		}
		
		c64.addDevice(vic);
		
		c64.initialize(clock_speed);
		sidFile.loadIntoMemory(c64);
		setCpu(new CPU6510(c64, null));
		
		/* 
		 * target - the target memory
		 * 	location for the SID tune
		 * 	driver
		 */
		int target;	
		
		// check for driver reloc. info
		if(sidFile.startPage !=  0) {
			// use it if it's there
			target = sidFile.startPage<<8;
		} else {
			// install driver after the tune
			//	if reloc. is not there
			target = sidFile.loadAddress + sidFile.dataSize+2;
		}
		
		// setup memory banks properly
				c64.WriteMemory(C64System.DDR_6510, 0xFF);
				c64.WriteMemory(C64System.PORT_6510, 0x05/*0b00000101*/);
		
		// lets write some machine code into ram:
		/*
		 * RESET routine
		 */
		c64.WriteMemory(target, 0xA9);c64.WriteMemory(target+1, 0x00);	// lda #$00 - operand overwritten in play loop by subtune #
		
		c64.WriteMemory(target+2, 0x20);		// jsr sid.initAddress
			c64.WriteMemory(target+3, sidFile.initAddress&0xFF);
			c64.WriteMemory(target+4, (sidFile.initAddress>>>8)&0xFF);
		
		c64.WriteMemory(target+5, 0x58);		// cli - enable IRQs
		c64.WriteMemory(target+6, 0x4C);		// jmp target+6
			c64.WriteMemory(target+7, (target+6)&0xFF);		// LOW(target+6)
			c64.WriteMemory(target+8, ((target+6)>>8)&0xFF);// HIGH(target+6)
		
		/*
		 * IRQ routine - for PSID files
		 */
		c64.WriteMemory(target+9, 0xAD);		// lda $DC0D - ack. CIA1 IRQ
			c64.WriteMemory(target+10, 0x0D);	// LOW($DD0D)
			c64.WriteMemory(target+11, 0xDC);	// HIGH($DD0D)
		
		c64.WriteMemory(target+12, 0xAD);		// lda $D019 - ack. VIC-II IRQ
			c64.WriteMemory(target+13, 0x19);	// LOW($D019)
			c64.WriteMemory(target+14, 0xD0);	// HIGH($D019)
		
		c64.WriteMemory(target+15, 0x20);		// jsr sid.playAddress - jump to play routine for PSID
			c64.WriteMemory(target+16, sidFile.playAddress&0xFF);		// LOW(sid.playAddress)
			c64.WriteMemory(target+17, (sidFile.playAddress>>8)&0xFF);	// HIGH(sid.playAddress)
		
		c64.WriteMemory(target+18, 0x4C);		// jmp $EA81 - KERNAL interrupt return code
			c64.WriteMemory(target+19, 0x81);	// LOW($EA81)
			c64.WriteMemory(target+20, 0xEA);	// HIGH($EA81)
			
		c64.WriteMemory(0xEA81, 0x40);			// rti - in case that KERNAL is not banked in
		
		/*
		 * NMI routine - just in case one is called
		 */
		c64.WriteMemory(target+21, 0x48);		// pha - save A reg on stack
		c64.WriteMemory(target+22, 0xAD);		// lda $DD0D - ack. CIA2 NMI
			c64.WriteMemory(target+23, 0x0D);	// LOW($DD0D)
			c64.WriteMemory(target+24, 0xDD);	// HIGH($DD0D)
		c64.WriteMemory(target+25, 0x68);		// pla - restore A reg from stack
		c64.WriteMemory(target+26, 0x40);		// rti
		
		
		/*
		 * RESET vector - PSID song init and inf. loop
		 */
		c64.WriteMemory(0xFFFC, (target)&0xFF);
		c64.WriteMemory(0xFFFD, ((target)>>8)&0xFF);
		
		/*
		 * IRQ vector - PSID song play routine
		 */
		c64.WriteMemory(0xFFFE, (target+9)&0xFF);
		c64.WriteMemory(0xFFFF, ((target+9)>>8)&0xFF);
		
		/*
		 * KERNAL IRQ vector - PSID song play routine
		 * 	when KERNAL banked in
		 */
		c64.WriteMemory(0x0314, (target+9)&0xFF);
		c64.WriteMemory(0x0315, ((target+9)>>8)&0xFF);
		
		
		/*
		 * KERNAL NMI vector - dummy NMI handler
		 * 	just to be safe
		 */
		c64.WriteMemory(0x0318, (target+21)&0xFF);
		c64.WriteMemory(0x0319, ((target+21)>>8)&0xFF);
		
		c64.WriteMemory(0xFFFA, (target+21)&0xFF);
		c64.WriteMemory(0xFFFB, ((target+21)>>8)&0xFF);
		
		// patching some KERNAL ram vector which isn't initialized
		c64.WriteMemory(0x028F, 0x48);
		c64.WriteMemory(0x0290, 0xEB);
		
		/* setup CIA1 */
		c64.WriteMemory(0xDC00, 0x7F);
		c64.WriteMemory(0xDC01, 0x7F);
		c64.WriteMemory(0xDC02, 0xFF);
		c64.WriteMemory(0xDC03, 0x00);
		
		c64.WriteMemory(0xDC0D, 0x7f/*0b01111111*/);
		c64.WriteMemory(0xDC0D, 0x81/*0b10000001*/);
		
		/* set CIA1 timer period */
		c64.WriteMemory(0xDC04, ((int)(clock_speed/60))&0xFF);
		c64.WriteMemory(0xDC05, (((int)(clock_speed/60))>>8)&0xFF);
		
		c64.WriteMemory(0xDC06, 0xFF);
		c64.WriteMemory(0xDC07, 0xFF);
		
		c64.WriteMemory(0xDC0E, 0x11);
		c64.WriteMemory(0xDC0F, 0x08);
		
		/* setup CIA2 */
		c64.WriteMemory(0xDD00, 0x17);
		c64.WriteMemory(0xDD01, 0x7F);
		c64.WriteMemory(0xDD02, 0x3F);
		c64.WriteMemory(0xDD03, 0x00);
		
		c64.WriteMemory(0xDD0D, 0x7f/*0b01111111*/);
		
		c64.WriteMemory(0xDD04, 0xFF);
		c64.WriteMemory(0xDD05, 0xFF);
		
		c64.WriteMemory(0xDD06, 0xFF);
		c64.WriteMemory(0xDD07, 0xFF);
		
		c64.WriteMemory(0xDD0D, 0x7F);
		c64.WriteMemory(0xDD0E, 0x08);
		c64.WriteMemory(0xDD0F, 0x08);
		
		c64.WriteMemory(0xD01A, 0x00);
		
		/* set RASTER to compare for interrupt */
		c64.WriteMemory(0xD011, 0x1<<7);
		c64.WriteMemory(0xD012, 0x37);
		
		/* 6510 Data Direction Register Default */
		c64.WriteMemory(C64System.DDR_6510, 0x2f/*0b00101111*/);
		
		/*
		 * PSID timer setup
		 */
		if(sidFile.isPSID()) { 
			/* CIA timing */
			if(timingSource == CIA) {
				c64.WriteMemory(0xDC0D, 0x82/*0b10000010*/);// enable cia 1a tmr interrupt
				c64.WriteMemory(0xD01A, 0x00);		// disable raster interrupt
			}else
			/* VIC Raster Int. timing */
			if(timingSource == VBI) {
				c64.WriteMemory(0xDC0D, 0x7f/*0b01111111*/);// disable cia interrupts
				
				/* set RASTER compare to zero
				 * 	because that's what PSIDs
				 * 	use.
				 */
				c64.WriteMemory(0xD011, 0x00);	/* RASTER bit 8 */
				c64.WriteMemory(0xD012, 0x00);	/* RASTER bits 0-7 */
				
				/* Enable the VIC-II raster interrupt. */
				c64.WriteMemory(0xD01A, 0x01);
				
			}
			
		}

		/* Set sub-tune number; see above
		 * 	in the machine code.
		 */
		c64.WriteMemory(target+1, song);
		
		/*
		 * Reset the cpu; load through
		 *  RESET vector which initializes
		 *  the tune and sets up necessary
		 *  interrupts.
		 */
		getCpu().Reset();
		
		/*
		 * Check for RSID and whether
		 * 	the environment needs to 
		 * 	be tailored for it.
		 */
		if(sidFile.isRSID()) {
			/* Disable VIC interrupts */
			c64.WriteMemory(0xD01A, 0x00);
			/* Set bank register to default */
			c64.WriteMemory(C64System.PORT_6510, 0x37);
		}
	}
	
	public void play() throws Exception {
		
		byte[] buffer = new byte[sampleRate/100*2];
		float cyclesPerSample = clock_speed / (float)sampleRate;
		//float cycles = 0;
		
		output = new SimpleSoundOutput();
		output.open(sampleRate, 16, 1, true, true, sampleRate/50*10);
		
		playThread = new PlayThread(
				buffer,
				cyclesPerSample,
				output,
				getCpu(),
				c64,
				sid1
		);
		
		t = new Thread(playThread);
		t.start();
	}
	
	public void stop() {
		if(playThread == null) return;
		playThread.stopIt();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		output.close();
		
		playThread = null;
		t = null;
		output = null;
		
		graphicsClear();
	}
	
	
	JFrame graphicsWindowFrame;
	Canvas graphicsWindowDisplay;
	BufferStrategy displayBufferStrategy;
	Graphics2D displayGraphics;
	
	class SIDKeyListener implements KeyListener {

		SIDAbstract sidDevice;
		SIDKeyListener(SIDAbstract passedDevice) {
			sidDevice = passedDevice;
		}
		
		public void updateSIDObject(SIDAbstract passedSID) {
			sidDevice = passedSID;
		}
		
		public void keyPressed(KeyEvent keyEvent) {
			switch(keyEvent.getKeyChar()) {
			case '1':
			case '2':
			case '3':
				sidDevice.toggleVoice(keyEvent.getKeyChar()-'1');
				break;
			
			case 'f':
				sidDevice.toggleFilter();
				break;
				
			default:
				break;
			}
		}

		public void keyReleased(KeyEvent keyEvent) {
		}

		public void keyTyped(KeyEvent keyEvent) {
		}
	};
	
	SIDKeyListener graphicsKeyListener;
	
	void graphicsInit(SIDAbstract passedSidDevice) {
		graphicsWindowFrame = new JFrame();
		graphicsWindowFrame.setTitle("SID Player Visuals");
		graphicsWindowFrame.setResizable(true);
		
		
		graphicsWindowDisplay = new Canvas();
		graphicsWindowDisplay.setBounds(0, 0, 640, 240);
		graphicsWindowFrame.getContentPane().add(graphicsWindowDisplay);
		graphicsWindowFrame.pack();
		
		//graphicsWindowFrame.setVisible(true);
		
		while(true) {
			try {
				graphicsWindowDisplay.createBufferStrategy(2);
				break;
			}
			catch(IllegalStateException e) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {
				}
				continue;
			}
		}
		displayBufferStrategy = graphicsWindowDisplay.getBufferStrategy();
		graphicsWindowDisplay.setBackground(new Color(0, 0, 0, 255));
		graphicsKeyListener = new SIDKeyListener(passedSidDevice);
		
		graphicsWindowDisplay.addKeyListener(graphicsKeyListener);
	}
	
	void graphicsClear() {
		Graphics2D displayGraphics = (Graphics2D)displayBufferStrategy.getDrawGraphics();
		displayGraphics.clearRect(0, 0, graphicsWindowDisplay.getWidth(), graphicsWindowDisplay.getHeight());
		displayBufferStrategy.show();
		displayGraphics.dispose();
	}
	
	void graphicsDestroy() {
		if(displayBufferStrategy != null) displayBufferStrategy.dispose();
		if(graphicsWindowFrame != null) {
			graphicsWindowFrame.setVisible(false);
			graphicsWindowFrame.dispose();
		}
	}
}
