package rsidplayer;

public abstract class SIDAbstract extends PeripheralDevice {
	
	class SIDVoice {
		int freq;
		int pw;
		int control;
		int ad;
		int sr;
		int phase;	//24 bit phase accumulator
		
		boolean msb_rising;
		int sync_offset;
		
		SIDEnvelope env;
		
		int noisepos;
		int noiseval;
		int noiseout;
		int triout;
		int sawout;
		int pulseout;
		int out;
		
		boolean enable;
		
		static final int ATTACK = 0;
		static final int DECAY = 1;
		static final int SUSTAIN = 2;
		static final int RELEASE = 3;
		
		SIDVoice() {
			freq = pw = control = ad = sr = phase = out = 0;
			noisepos = 0;
			noiseval = 0x7FFFF8;
			noiseout = 0;
			triout = 0;
			sawout = 0;
			pulseout = 0;
			
			enable = true;
			env = new SIDEnvelope();
		}
	}
	
	public SIDVoice[] voice = new SIDVoice[3];
	
	int rFC;
	int rFilt;
	int rVolFilt;
	boolean filterEnabled = true;
	
	SIDFilter filter = new SIDFilter();
	
	float localClock;
	
	SIDAbstract(String identity, C64System memory) {
		super(identity, memory);
	}
	
	private int get_bit(int word, int bit) {
		return (word>>bit)&1;
	}
	
	private void update() {
		int prevphase;
		int phase;
		
		for(int i = 0; i < 3; ++i) {
			SIDVoice v = voice[i];
			int refosc = (i>0)?i-1:2;
			if((v.control & 0x02) == 0x02) {
				if(voice[refosc].msb_rising == true)
					v.phase = 0;
			}
			if((v.control & 0x08) != 0x08) {
				prevphase = v.phase;
				v.phase += v.freq;
				//v.phase &= 0xFFFFFF;
				if(v.phase > 0x00FFFFFF) {
					v.phase -= 0x00FFFFFF;
				}
				
				// msb changed
				if(/*((v.phase ^ prevphase)&0x00800000) != 0*/prevphase > v.phase) {
					v.sync_offset = 0;
					v.msb_rising = true;
				}
				else
					v.msb_rising = false;
			}else
			{
				phase = 0;
				v.phase = 0;
				v.noisepos = 0;
				v.noiseval = 0x7FFFF8;
				continue;
			}
			
			if(v.noisepos != ((v.phase>>19)&1)) {
				v.noisepos = (v.phase>>19)&1;
				v.noiseval = (v.noiseval << 1) |
                (get_bit(v.noiseval,22) ^ get_bit(v.noiseval,17));
				v.noiseout = (get_bit(v.noiseval,22) << 7) |
                        (get_bit(v.noiseval,20) << 6) |
                        (get_bit(v.noiseval,16) << 5) |
                        (get_bit(v.noiseval,13) << 4) |
                        (get_bit(v.noiseval,11) << 3) |
                        (get_bit(v.noiseval, 7) << 2) |
                        (get_bit(v.noiseval, 4) << 1) |
                        (get_bit(v.noiseval, 2) << 0);
				v.noiseout <<= 4;
			}
			
			v.env.clock(); 
		}
	}
	
	private void update(int cycles) {
		int prevphase;
		int phase;
		
		int cycles2;
		for(int i = 0; i < 3; ++i) {
			cycles2 = cycles;
			SIDVoice v = voice[i];
			int refosc = (i>0)?i-1:2;
			if((v.control & 0x02) == 0x02) {
				if(voice[refosc].msb_rising == true) {
					v.phase = v.freq*v.sync_offset;
				}
			}
			if((v.control & 0x08) != 0x08) {
				prevphase = v.phase;
				v.phase += v.freq*cycles;
				//v.phase &= 0xFFFFFF;
				if(v.phase > 0x00FFFFFF) {
					v.phase -= 0x00FFFFFF;
				}
				
				// msb changed
				if(/*((v.phase ^ prevphase)&0x00800000) != 0*/prevphase > v.phase) {
					v.sync_offset = (((0x00FFFFFF-prevphase)+v.phase)/v.freq)-1;
					v.msb_rising = true;
				}
				else
					v.msb_rising = false;
			}else
			{
				phase = 0;
				v.phase = 0;
				v.noisepos = 0;
				v.noiseval = 0x7FFFF8;
				continue;
			}
			
			while(cycles2-- > 0) {
				if(v.noisepos != ((v.phase>>19)&1)) {
					v.noisepos = (v.phase>>19)&1;
					v.noiseval = (v.noiseval << 1) |
	                (get_bit(v.noiseval,22) ^ get_bit(v.noiseval,17));
					v.noiseout = (get_bit(v.noiseval,22) << 7) |
	                        (get_bit(v.noiseval,20) << 6) |
	                        (get_bit(v.noiseval,16) << 5) |
	                        (get_bit(v.noiseval,13) << 4) |
	                        (get_bit(v.noiseval,11) << 3) |
	                        (get_bit(v.noiseval, 7) << 2) |
	                        (get_bit(v.noiseval, 4) << 1) |
	                        (get_bit(v.noiseval, 2) << 0);
					v.noiseout <<= 4;
				}
				
				//v.env.clock();
			}
			
			v.env.clock(cycles); 
		}
	}
	
	abstract void resonance(int r);
	abstract void cutoff(int fc);
	abstract float getActualCutoff();
	abstract int filter(int in, int cycles, boolean lp, boolean hp, boolean bp);
	
	
	int outputSample(int cycles) {
		
		int out = 0, fout = 0;
		
		for(int i = 0; i < 3; ++i) {
			SIDVoice v = voice[i];
			
			int refosc = (i>0)?i-1:2;
				
			int phase = v.phase>>12;
			
			/*v.pulseout = ((phase) <= v.pw) ? (0xFFF) : (0);
			v.sawout = phase;
			v.triout = phase<<1;
			if((phase>=0x800))
				v.triout = (v.triout ^ 0xFFF);
			
			if((v.control & 0x04) == 0x04) {
				if((voice[refosc].phase & 0x00800000) == 0x00800000)
					v.triout ^= 0xFFF;	
			}*/
			
			//v.triout<<=1;
			
			v.out = 0xFFF;
			
			switch(v.control & 0xF0)
			{
			case 0x10:	//triangle
				v.triout = phase<<1;
				if((phase>=0x800))
					v.triout = (v.triout ^ 0xFFF);
				
				if((v.control & 0x04) == 0x04) {
					if((voice[refosc].phase & 0x00800000) == 0x00800000)
						v.triout ^= 0xFFF;	
				}
				v.out &= v.triout;
				break;
			case 0x20:	//sawtooth
				v.out &= phase;//v.sawout;
				break;
			case 0x30:	//tri+saw
				v.triout = phase<<1;
				if((phase>=0x800))
					v.triout = (v.triout ^ 0xFFF);
				
				if((v.control & 0x04) == 0x04) {
					if((voice[refosc].phase & 0x00800000) == 0x00800000)
						v.triout ^= 0xFFF;	
				}
				v.out &= v.triout & phase;
				break;
			case 0x40:	//pulse
				v.out &= (((phase) <= v.pw) ? (0xFFF) : (0));/*v.pulseout*/;
				break;
			case 0x50:	//tri+pulse
				v.triout = phase<<1;
				if((phase>=0x800))
					v.triout = (v.triout ^ 0xFFF);
				
				if((v.control & 0x04) == 0x04) {
					if((voice[refosc].phase & 0x00800000) == 0x00800000)
						v.triout ^= 0xFFF;	
				}
				v.out &= v.triout & (((phase) <= v.pw) ? (0xFFF) : (0));/*v.pulseout*/;
				break;
			case 0x60:	//saw+pulse
				v.out &= phase & (((phase) <= v.pw) ? (0xFFF) : (0));
				break;
			case 0x70:	//tri+saw+pulse
				v.triout = phase<<1;
				if((phase>=0x800))
					v.triout = (v.triout ^ 0xFFF);
				
				if((v.control & 0x04) == 0x04) {
					if((voice[refosc].phase & 0x00800000) == 0x00800000)
						v.triout ^= 0xFFF;	
				}
				v.out &= v.triout & phase & (((phase) <= v.pw) ? (0xFFF) : (0));
				break;
			case 0x80:	//noise
				v.out &= v.noiseout;
				break;
			case 0x90:	//tri+noise?
				v.triout = phase<<1;
				if((phase>=0x800))
					v.triout = (v.triout ^ 0xFFF);
				
				if((v.control & 0x04) == 0x04) {
					if((voice[refosc].phase & 0x00800000) == 0x00800000)
						v.triout ^= 0xFFF;	
				}
				v.out &= v.triout & v.noiseout;
				break;
			case 0xE0:	//saw+pulse+?noise?
				v.out &= phase & (((phase) <= v.pw) ? (0xFFF) : (0)) & v.noiseout;
				break;
			case 0xF0:	//tri+saw+pulse+?noise?
				v.triout = phase<<1;
				if((phase>=0x800))
					v.triout = (v.triout ^ 0xFFF);
				
				if((v.control & 0x04) == 0x04) {
					if((voice[refosc].phase & 0x00800000) == 0x00800000)
						v.triout ^= 0xFFF;	
				}
				v.out &= v.triout & phase & (((phase) <= v.pw) ? (0xFFF) : (0)) & v.noiseout;
				break;
				
			default:
				break;
			}
			
			/*if((v.control & 0x10) == 0x10) {
				v.out &= v.triout;
			}
			if((v.control & 0x20) == 0x20) {
				v.out &= v.sawout;
			}
			if((v.control & 0x40) == 0x40) {
				v.out &= v.pulseout;
			}
			if((v.control & 0x80) == 0x80) {
				v.out &= v.noiseout;
			}*/
			
			v.out = v.out - 0x7FF;
			v.out = (int)v.out*v.env.output()>>3;// ((v.out * (v.envval>>16))>>8); 
		
			if(!v.enable) {
				v.out = 0;
			}
		}
		
		//if(filterEnabled) fout = filter(fout, cycles, (rVolFilt&0x10)==0x10, (rVolFilt&0x20)==0x20, (rVolFilt&0x40)==0x40);
		
		// DC Offset for DIGIs
		//return (int)(-(65536.0f*0.5f)/(4096.0f*3 + 4096.0f) * (out - fout + 4096.0f) * ((float)(rVolFilt&0xF))/15.0f);
		
		filter.clock(cycles, voice[0].out, voice[1].out, voice[2].out, 0);
		return filter.output();
	}
	
	void toggleVoice(int v) {
		if(v < 0 || v > 2) return;
		
		voice[v].enable = !voice[v].enable;
		
	}
	
	void toggleFilter() {
		filterEnabled = !filterEnabled;
		filter.enable_filter(filterEnabled);
	}
	

	@Override
	void reset(float clock) {
		// TODO Auto-generated method stub
		voice[0] = new SIDVoice();
		voice[1] = new SIDVoice();
		voice[2] = new SIDVoice();
		
	//	float clockRatio = 1000000.0f / clock;
		/*for(int i = 0; i < 16; ++i) {
			attackRates[i] = (long) (0x01000000L / (attackTimes[i]*clock));
			releaseRates[i] = (long) (0x01000000L / (releaseTimes[i]*clock));
		}*/
		
		rFC = rFilt = rVolFilt = 0;
		filterEnabled = true;
		
		localClock = clock;
	}

	@Override
	void clock(int cycles) {
		/*while(cycles > 0) {
			update();
			--cycles;
		}*/
		
		update(cycles);
	}

	@Override
	void write(int address, int data) {
		//if((address & 0xfc00) == 0xd400)
		{
			address &= 0x1f;
			if(address <= 0x14) {
				SIDVoice curVoice;
				if(address >= 0x0e) {
					address -= 0x0e;
					curVoice = voice[2];
				}
				else
				if(address >= 0x07) {
					address -= 0x07;
					curVoice = voice[1];
				}
				else
					curVoice = voice[0];
				
				switch(address) {
					case 0x00:
						curVoice.freq = (curVoice.freq&0xFF00) + (data&0xFF);
						break;
					case 0x01:
						curVoice.freq = (curVoice.freq&0x00FF) + ((data&0xFF)<<8);
						break;
					case 0x02:
						curVoice.pw = (curVoice.pw&0x0F00) | (data&0xFF);
						break;
					case 0x03:
						curVoice.pw = (curVoice.pw&0x00FF) | ((data&0x0F)<<8);
						break;
					case 0x04:
						curVoice.control = data&0xFF;
						curVoice.env.writeCONTROL_REG(data);
						break;
					case 0x05:
						curVoice.ad = data&0xFF;
						curVoice.env.writeATTACK_DECAY(data);
						break;
					case 0x06:
						curVoice.sr = data&0xFF;
						curVoice.env.writeSUSTAIN_RELEASE(data);
						break;
				}
					
			}
			else
			switch(address) {
				case 0x15:
					rFC = (rFC&0x07F8) | (data&0x07);
					filter.writeFC_LO(data);
					break;
				case 0x16:
					rFC = (rFC&0x07) | ((data&0xFF)<<3);
					filter.writeFC_HI(data);
					cutoff(rFC);
					break;
				case 0x17:
					rFilt = data&0xFF;
					//resonance((rFilt>>4) & 0xF);
					filter.writeRES_FILT(data);
					break;
				case 0x18:
					rVolFilt = data&0xFF;
					filter.writeMODE_VOL(data);
					break;
			}
		}
	}

	@Override
	int read(int address) {
		return 0;
	}

	@Override
	boolean requestsIRQ() {
		return false;
	}

}
