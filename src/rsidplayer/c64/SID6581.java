package rsidplayer.c64;

public class SID6581 extends SIDAbstract {

	public SID6581(int idSid, C64System memory) {
		super(idSid, memory);
		
		cutoff = new float[0x800];
		calcCutoffTable();
	}
	
	float actualCutoff, localFC;
	float w0_precalc = (int)(2.0 * Math.PI * 1.048576);
	float Q,w0;
	float Vbp = 0, Vlp = 0, Vhp = 0;
	protected float[] cutoff;
	
	private void calcCutoffTable() {
		for(int i = 0; i < 0x800; ++i) {
			cutoff[i] = (float)(((Math.tanh((6.0*(i&0x7FF)/2048.0)-3.0)+1.0)*0.5*(10000.0-30.0)) + 30);
		}
	}
	
	private void precalc(float clock) {
		w0 = 0;
		w0_precalc = (float)(2.0 * Math.PI * 1.048576/*localClock/1000000f*/);
		resonance(0);
	}
	
	@Override
	void reset(float clock) {
		super.reset(clock);
		precalc(clock);
	}
	
	@Override
	void resonance(int r) {
		Q = (float)((0.707f + (4*(r&0x0F)) / 15.0f));
	}

	@Override
	void cutoff(int fc) {
		localFC = fc&0x7FF;
		actualCutoff = cutoff[fc&0x7FF];
		
		//actualCutoff = (14000-30)/2047.0 * (fc&0x7FF) + 30;		
		w0 = (actualCutoff * w0_precalc);
	}

	@Override
	public
	float getActualCutoff() {
		return actualCutoff;
	}

	@Override
	int filter(int in, int cycles, boolean lp, boolean hp, boolean bp) {
		/*double*/float A = 0;
		int delta_t = cycles, delta_t_flt = 1;
		
		if(in == 0)
			return 0;
		
		while(delta_t > 0) {
			if(delta_t < delta_t_flt) {
				delta_t_flt = delta_t;
			}
			float w_delta_t = (w0 * delta_t_flt);
			float dVbp = (int) /*(float)*/ ((w_delta_t * (Vhp)) / (localClock));
			float dVlp = (int) /*(float)*/ ((w_delta_t * (Vbp)) / (localClock));
			Vbp -= dVbp;
			Vlp -= dVlp;
			Vhp = (Vbp/Q + in - Vlp);
			
			delta_t -= delta_t_flt;
		}
		
		
		if(lp == true) {
			A += Vlp;
		}
		
		if(bp == true) {
			A += Vbp;
		}
		
		if(hp == true) {
			A += Vhp;
		}
		
		return (int) A;
	}

}
