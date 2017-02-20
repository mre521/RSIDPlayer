//  Note: code below was adapted from reSID (envelope.h/.cc). License appended to make it legal
//  ---------------------------------------------------------------------------
//  This file is part of reSID, a MOS6581 SID emulator engine.
//  Copyright (C) 2004  Dag Lem <resid@nimrod.no>
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//  ---------------------------------------------------------------------------

package rsidplayer.c64;

public class SIDEnvelope {
	enum State {
		ATTACK, DECAY_SUSTAIN, RELEASE;
	};
	
	SIDEnvelope() {
		reset();
	}
	
	void clock() {
		if((++rate_counter & 0x8000) != 0) {
			++rate_counter;
			rate_counter &= 0x7fff;
		}
		
		if(rate_counter != rate_period) {
			return;
		}
		
		rate_counter = 0;
		
		if(state == State.ATTACK || ++exponential_counter == exponential_counter_period) {
			exponential_counter = 0;
			
			if(hold_zero)
				return;
			
			switch(state) {
			case ATTACK:
				++envelope_counter;
				envelope_counter &= 0xff;
				if(envelope_counter == 0xff) {
					state = State.DECAY_SUSTAIN;
					rate_period = rate_counter_period[decay];
				}
				break;
			case DECAY_SUSTAIN:
				if(envelope_counter != sustain_level[sustain]) {
					--envelope_counter;
				}
				break;
			case RELEASE:
				--envelope_counter;
				envelope_counter &= 0xff;
				break;
			}
		}
		
		switch(envelope_counter) {
		case 0xff:
			exponential_counter_period = 1;
			break;
		case 0x5d:
			exponential_counter_period = 2;
			break;
	    case 0x36:
	    	exponential_counter_period = 4;
	    	break;
	    case 0x1a:
	    	exponential_counter_period = 8;
	    	break;
	    case 0x0e:
	    	exponential_counter_period = 16;
	    	break;
	    case 0x06:
	    	exponential_counter_period = 30;
	    	break;
	    case 0x00:
	    	exponential_counter_period = 1;
	    	
	    	hold_zero = true;
	    	break;
		}
	}
	
	void clock(int delta_t) {
		
		int rate_step = rate_period - rate_counter;
		if(rate_step <= 0) {
			rate_step += 0x7fff;
		}
		
		while(delta_t > 0) {
			if(delta_t < rate_step){
				rate_counter += delta_t;
				if((rate_counter & 0x8000) != 0) {
					++rate_counter;
					rate_counter &= 0x7fff;
				}
				return;
			}
		
			rate_counter = 0;
			delta_t -= rate_step;
			
			if(state == State.ATTACK || ++exponential_counter == exponential_counter_period) {
				exponential_counter = 0;
				
				if(hold_zero) {
					rate_step = rate_period;
					continue;
				}
				
				switch(state) {
				case ATTACK:
					++envelope_counter;
					envelope_counter &= 0xff;
					if(envelope_counter == 0xff) {
						state = State.DECAY_SUSTAIN;
						rate_period = rate_counter_period[decay];
					}
					break;
				case DECAY_SUSTAIN:
					if(envelope_counter != sustain_level[sustain]) {
						--envelope_counter;
					}
					break;
				case RELEASE:
					--envelope_counter;
					envelope_counter &= 0xff;
					break;
				}
			}
			
			switch(envelope_counter) {
			case 0xff:
				exponential_counter_period = 1;
				break;
			case 0x5d:
				exponential_counter_period = 2;
				break;
		    case 0x36:
		    	exponential_counter_period = 4;
		    	break;
		    case 0x1a:
		    	exponential_counter_period = 8;
		    	break;
		    case 0x0e:
		    	exponential_counter_period = 16;
		    	break;
		    case 0x06:
		    	exponential_counter_period = 30;
		    	break;
		    case 0x00:
		    	exponential_counter_period = 1;
		    	
		    	hold_zero = true;
		    	break;
			}
			
			rate_step = rate_period;
		}
	}
	
	void reset() {
		envelope_counter = 0;
		
		attack = 0;
		decay = 0;
		sustain = 0;
		release = 0;
		
		gate = 0;
		
		rate_counter = 0;
		exponential_counter = 0;
		exponential_counter_period = 1;
		
		state = State.RELEASE;
		rate_period = rate_counter_period[release];
		hold_zero = true;
	}
	
	void writeCONTROL_REG(int control) {
		int gate_next = control & 0x01;
		
		if((gate==0) && (gate_next!=0)) {
			state = State.ATTACK;
			rate_period = rate_counter_period[attack];
		
			hold_zero = false;
		}
		
		else if((gate!=0) && (gate_next==0)) {
			state = State.RELEASE;
			rate_period = rate_counter_period[release];
		}
		
		gate = gate_next;
	}
	
	void writeATTACK_DECAY(int attack_decay) {
		attack = (attack_decay>>4) & 0x0f;
		decay = attack_decay & 0x0f;
		if(state == State.ATTACK) {
			rate_period = rate_counter_period[attack];
		}
		else if(state == State.DECAY_SUSTAIN) {
			rate_period = rate_counter_period[decay];
		}
	}

	void writeSUSTAIN_RELEASE(int sustain_release) {
		sustain = (sustain_release>>4) & 0x0f;
		release = sustain_release & 0x0f;
		if(state == State.RELEASE) {
			rate_period = rate_counter_period[release];
		}
	}
	
	int readENV() {
		return output();
	}
	
	int output() {
		return envelope_counter;
	}
	
	protected int rate_counter;
	protected int rate_period;
	protected int exponential_counter;
	protected int exponential_counter_period;
	protected int envelope_counter;
	protected boolean hold_zero;
	
	int attack;
	int decay;
	int sustain;
	int release;
	
	int gate;
	
	State state;
	
	static int[] rate_counter_period = {
	      9,  //   2ms*1.0MHz/256 =     7.81
	      32,  //   8ms*1.0MHz/256 =    31.25
	      63,  //  16ms*1.0MHz/256 =    62.50
	      95,  //  24ms*1.0MHz/256 =    93.75
	     149,  //  38ms*1.0MHz/256 =   148.44
	     220,  //  56ms*1.0MHz/256 =   218.75
	     267,  //  68ms*1.0MHz/256 =   265.63
	     313,  //  80ms*1.0MHz/256 =   312.50
	     392,  // 100ms*1.0MHz/256 =   390.63
	     977,  // 250ms*1.0MHz/256 =   976.56
	    1954,  // 500ms*1.0MHz/256 =  1953.13
	    3126,  // 800ms*1.0MHz/256 =  3125.00
	    3907,  //   1 s*1.0MHz/256 =  3906.25
	   11720,  //   3 s*1.0MHz/256 = 11718.75
	   19532,  //   5 s*1.0MHz/256 = 19531.25
	   31251   //   8 s*1.0MHz/256 = 31250.00
	};
	
	int sustain_level[] = {
	  0x00,
	  0x11,
	  0x22,
	  0x33,
	  0x44,
	  0x55,
	  0x66,
	  0x77,
	  0x88,
	  0x99,
	  0xaa,
	  0xbb,
	  0xcc,
	  0xdd,
	  0xee,
	  0xff,
	};
	
}
