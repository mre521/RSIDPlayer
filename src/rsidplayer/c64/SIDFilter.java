package rsidplayer.c64;
//---------------------------------------------------------------------------
// reSID license for its use:
//This file is part of reSID, a MOS6581 SID emulator engine.
//Copyright (C) 2004  Dag Lem <resid@nimrod.no>
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//---------------------------------------------------------------------------

public class SIDFilter {
	public static final int MOS6581 = 0;
	public static final int MOS8580 = 1;
	
	SIDFilter() {
		  fc = 0;

		  res = 0;

		  filt = 0;

		  voice3off = 0;

		  hp_bp_lp = 0;

		  vol = 0;

		  // State of filter.
		  Vhp = 0;
		  Vbp = 0;
		  Vlp = 0;
		  Vnf = 0;

		  enable_filter(true);

		  // Create mappings from FC to cutoff frequency.
		/*  interpolate(f0_points_6581, f0_points_6581
			      + sizeof(f0_points_6581)/sizeof(*f0_points_6581) - 1,
			      PointPlotter<sound_sample>(f0_6581), 1.0);
		  interpolate(f0_points_8580, f0_points_8580
			      + sizeof(f0_points_8580)/sizeof(*f0_points_8580) - 1,
			      PointPlotter<sound_sample>(f0_8580), 1.0);*/
		  
		  for(int i = 0; i < 0x800; ++i) {
				f0_8580[i] = (int) ((((12000.0-30.0)/2048.0)*i)+30.0);
		  }
		  
		  for(int i = 0; i < 0x800; ++i) {
				f0_6581[i] = (int)(((Math.tanh((6.0*(i&0x7FF)/2048.0)-3.0)+1.0)*0.5*(10000.0-30.0)) + 30);
		  }

		  set_chip_model(MOS6581);
	}
	void enable_filter(boolean enable) {
		enabled = enable;
	}

	void set_chip_model(int model) {
		if (model == MOS6581) {
		    // The mixer has a small input DC offset. This is found as follows:
		    //
		    // The "zero" output level of the mixer measured on the SID audio
		    // output pin is 5.50V at zero volume, and 5.44 at full
		    // volume. This yields a DC offset of (5.44V - 5.50V) = -0.06V.
		    //
		    // The DC offset is thus -0.06V/1.05V ~ -1/18 of the dynamic range
		    // of one voice. See voice.cc for measurement of the dynamic
		    // range.

		    mixer_DC = -0xfff*0xff/18 >> 7;

		    f0 = f0_6581;
		  }
		  else {
		    // No DC offsets in the MOS8580.
		    mixer_DC = 0;

		    f0 = f0_8580;
		  }

		  set_w0();
		  set_Q();
	}

	void clock(int voice1, int voice2, int voice3,
			int ext_in) {
		// Scale each voice down from 20 to 13 bits.
		voice1 >>= 7;
		voice2 >>= 7;

		// NB! Voice 3 is not silenced by voice3off if it is routed through
		// the filter.
		if ((voice3off!=0) && ((filt & 0x04)==0)) {
			voice3 = 0;
		}
		else {
			voice3 >>= 7;
		}

		ext_in >>= 7;

		// This is handy for testing.
		if (!enabled) {
			Vnf = voice1 + voice2 + voice3 + ext_in;
			Vhp = Vbp = Vlp = 0;
			return;
		}

		// Route voices into or around filter.
		// The code below is expanded to a switch for faster execution.
		// (filt1 ? Vi : Vnf) += voice1;
		// (filt2 ? Vi : Vnf) += voice2;
		// (filt3 ? Vi : Vnf) += voice3;

		int Vi;

		switch (filt) {
		default:
		case 0x0:
			Vi = 0;
			Vnf = voice1 + voice2 + voice3 + ext_in;
			break;
		case 0x1:
			Vi = voice1;
			Vnf = voice2 + voice3 + ext_in;
			break;
		case 0x2:
			Vi = voice2;
			Vnf = voice1 + voice3 + ext_in;
			break;
		case 0x3:
			Vi = voice1 + voice2;
			Vnf = voice3 + ext_in;
			break;
		case 0x4:
			Vi = voice3;
			Vnf = voice1 + voice2 + ext_in;
			break;
		case 0x5:
			Vi = voice1 + voice3;
			Vnf = voice2 + ext_in;
			break;
		case 0x6:
			Vi = voice2 + voice3;
			Vnf = voice1 + ext_in;
			break;
		case 0x7:
			Vi = voice1 + voice2 + voice3;
			Vnf = ext_in;
			break;
		case 0x8:
			Vi = ext_in;
			Vnf = voice1 + voice2 + voice3;
			break;
		case 0x9:
			Vi = voice1 + ext_in;
			Vnf = voice2 + voice3;
			break;
		case 0xa:
			Vi = voice2 + ext_in;
			Vnf = voice1 + voice3;
			break;
		case 0xb:
			Vi = voice1 + voice2 + ext_in;
			Vnf = voice3;
			break;
		case 0xc:
			Vi = voice3 + ext_in;
			Vnf = voice1 + voice2;
			break;
		case 0xd:
			Vi = voice1 + voice3 + ext_in;
			Vnf = voice2;
			break;
		case 0xe:
			Vi = voice2 + voice3 + ext_in;
			Vnf = voice1;
			break;
		case 0xf:
			Vi = voice1 + voice2 + voice3 + ext_in;
			Vnf = 0;
			break;
		}

		// delta_t = 1 is converted to seconds given a 1MHz clock by dividing
				// with 1 000 000.

				// Calculate filter outputs.
				// Vhp = Vbp/Q - Vlp - Vi;
		// dVbp = -w0*Vhp*dt;
		// dVlp = -w0*Vbp*dt;

		int dVbp = (w0_ceil_1*Vhp >> 20);
		int dVlp = (w0_ceil_1*Vbp >> 20);
		Vbp -= dVbp;
		Vlp -= dVlp;
		Vhp = (Vbp*_1024_div_Q >> 10) - Vlp - Vi;
	}

	void clock(int delta_t,
			int voice1, int voice2, int voice3,
			int ext_in) {
		// Scale each voice down from 20 to 13 bits.
		voice1 >>= 7;
		voice2 >>= 7;

		// NB! Voice 3 is not silenced by voice3off if it is routed through
		// the filter.
		if ((voice3off!=0) && ((filt & 0x04)==0)) {
			voice3 = 0;
		}
		else {
			voice3 >>= 7;
		}

		ext_in >>= 7;

		// Enable filter on/off.
		// This is not really part of SID, but is useful for testing.
		// On slow CPUs it may be necessary to bypass the filter to lower the CPU
		// load.
		if (!enabled) {
			Vnf = voice1 + voice2 + voice3 + ext_in;
			Vhp = Vbp = Vlp = 0;
			return;
		}

		// Route voices into or around filter.
		// The code below is expanded to a switch for faster execution.
		// (filt1 ? Vi : Vnf) += voice1;
		// (filt2 ? Vi : Vnf) += voice2;
		// (filt3 ? Vi : Vnf) += voice3;

		int Vi;

		switch (filt) {
		default:
		case 0x0:
			Vi = 0;
			Vnf = voice1 + voice2 + voice3 + ext_in;
			break;
		case 0x1:
			Vi = voice1;
			Vnf = voice2 + voice3 + ext_in;
			break;
		case 0x2:
			Vi = voice2;
			Vnf = voice1 + voice3 + ext_in;
			break;
		case 0x3:
			Vi = voice1 + voice2;
			Vnf = voice3 + ext_in;
			break;
		case 0x4:
			Vi = voice3;
			Vnf = voice1 + voice2 + ext_in;
			break;
		case 0x5:
			Vi = voice1 + voice3;
			Vnf = voice2 + ext_in;
			break;
		case 0x6:
			Vi = voice2 + voice3;
			Vnf = voice1 + ext_in;
			break;
		case 0x7:
			Vi = voice1 + voice2 + voice3;
			Vnf = ext_in;
			break;
		case 0x8:
			Vi = ext_in;
			Vnf = voice1 + voice2 + voice3;
			break;
		case 0x9:
			Vi = voice1 + ext_in;
			Vnf = voice2 + voice3;
			break;
		case 0xa:
			Vi = voice2 + ext_in;
			Vnf = voice1 + voice3;
			break;
		case 0xb:
			Vi = voice1 + voice2 + ext_in;
			Vnf = voice3;
			break;
		case 0xc:
			Vi = voice3 + ext_in;
			Vnf = voice1 + voice2;
			break;
		case 0xd:
			Vi = voice1 + voice3 + ext_in;
			Vnf = voice2;
			break;
		case 0xe:
			Vi = voice2 + voice3 + ext_in;
			Vnf = voice1;
			break;
		case 0xf:
			Vi = voice1 + voice2 + voice3 + ext_in;
			Vnf = 0;
			break;
		}

		// Maximum delta cycles for the filter to work satisfactorily under current
		// cutoff frequency and resonance constraints is approximately 8.
		int delta_t_flt = 8;

		while (delta_t > 0) {
			if (delta_t < delta_t_flt) {
				delta_t_flt = delta_t;
			}

			// delta_t is converted to seconds given a 1MHz clock by dividing
			// with 1 000 000. This is done in two operations to avoid integer
			// multiplication overflow.

			// Calculate filter outputs.
			// Vhp = Vbp/Q - Vlp - Vi;
			// dVbp = -w0*Vhp*dt;
			// dVlp = -w0*Vbp*dt;
			int w0_delta_t = w0_ceil_dt*delta_t_flt >> 6;

			int dVbp = (w0_delta_t*Vhp >> 14);
			int dVlp = (w0_delta_t*Vbp >> 14);
			Vbp -= dVbp;
			Vlp -= dVlp;
			Vhp = (Vbp*_1024_div_Q >> 10) - Vlp - Vi;

			delta_t -= delta_t_flt;
		}
	}
	
	void reset() {
		  fc = 0;

		  res = 0;

		  filt = 0;

		  voice3off = 0;

		  hp_bp_lp = 0;

		  vol = 0;

		  // State of filter.
		  Vhp = 0;
		  Vbp = 0;
		  Vlp = 0;
		  Vnf = 0;

		  set_w0();
		  set_Q();
	}

	// Write registers.
	void writeFC_LO(int fc_lo) {
		fc = fc & 0x7f8 | fc_lo & 0x007;
		set_w0();
	}
	void writeFC_HI(int fc_hi) {
		fc = (fc_hi << 3) & 0x7f8 | fc & 0x007;
		set_w0();
	}
	void writeRES_FILT(int res_filt) {
		res = (res_filt >> 4) & 0x0f;
		set_Q();

		filt = res_filt & 0x0f;
	}
	void writeMODE_VOL(int mode_vol) {
		voice3off = mode_vol & 0x80;

		hp_bp_lp = (mode_vol >> 4) & 0x07;

		vol = mode_vol & 0x0f;
	}

	// SID audio output (16 bits).
	int output() {
		  // This is handy for testing.
		  if (!enabled) {
		    return (Vnf + mixer_DC)*(int)(vol);
		  }

		  // Mix highpass, bandpass, and lowpass outputs. The sum is not
		  // weighted, this can be confirmed by sampling sound output for
		  // e.g. bandpass, lowpass, and bandpass+lowpass from a SID chip.

		  // The code below is expanded to a switch for faster execution.
		  // if (hp) Vf += Vhp;
		  // if (bp) Vf += Vbp;
		  // if (lp) Vf += Vlp;

		  int Vf;

		  switch (hp_bp_lp) {
		  default:
		  case 0x0:
		    Vf = 0;
		    break;
		  case 0x1:
		    Vf = Vlp;
		    break;
		  case 0x2:
		    Vf = Vbp;
		    break;
		  case 0x3:
		    Vf = Vlp + Vbp;
		    break;
		  case 0x4:
		    Vf = Vhp;
		    break;
		  case 0x5:
		    Vf = Vlp + Vhp;
		    break;
		  case 0x6:
		    Vf = Vbp + Vhp;
		    break;
		  case 0x7:
		    Vf = Vlp + Vbp + Vhp;
		    break;
		  }

		  // Sum non-filtered and filtered output.
		  // Multiply the sum with volume.
		  return (Vnf + Vf + mixer_DC)*(int)(vol);
	}

	void set_w0() {
		//const double pi = 3.1415926535897932385;

		// Multiply with 1.048576 to facilitate division by 1 000 000 by right-
		// shifting 20 times (2 ^ 20 = 1048576).
		w0 = (int)(2*Math.PI*f0[fc]*1.048576);

		// Limit f0 to 16kHz to keep 1 cycle filter stable.
		final int w0_max_1 = (int)(2*Math.PI*16000*1.048576);
		w0_ceil_1 = w0 <= w0_max_1 ? w0 : w0_max_1;

		// Limit f0 to 4kHz to keep delta_t cycle filter stable.
		final int w0_max_dt = (int)(2*Math.PI*4000*1.048576);
		w0_ceil_dt = w0 <= w0_max_dt ? w0 : w0_max_dt;
	}
	void set_Q() {
		// Q is controlled linearly by res. Q has approximate range [0.707, 1.7].
		// As resonance is increased, the filter must be clocked more often to keep
		// stable.

		// The coefficient 1024 is dispensed of later by right-shifting 10 times
		// (2 ^ 10 = 1024).
		_1024_div_Q = (int)(1024.0/(0.707 + 1.0*res/0x0f));
	}

	// Filter enabled.
	boolean enabled;

	// Filter cutoff frequency.
	int fc;

	// Filter resonance.
	int res;

	// Selects which inputs to route through filter.
	int filt;

	// Switch voice 3 off.
	int voice3off;

	// Highpass, bandpass, and lowpass filter modes.
	int hp_bp_lp;

	// Output master volume.
	int vol;

	// Mixer DC offset.
	int mixer_DC;

	// State of filter.
	int Vhp; // highpass
	int Vbp; // bandpass
	int Vlp; // lowpass
	int Vnf; // not filtered

	// Cutoff frequency, resonance.
	int w0, w0_ceil_1, w0_ceil_dt;
	int _1024_div_Q;

	// Cutoff frequency tables.
	// FC is an 11 bit register.
	int[] f0_6581 = new int[2048];
	int[] f0_8580 = new int[2048];
	int[] f0;
}
