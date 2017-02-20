package rsidplayer;

class SID8580 extends SID6581 {
	SID8580(String identity, C64System memory) {
		super(identity, memory);
		filter.set_chip_model(SIDFilter.MOS8580);
		calcCutoffTable();
	}
	
	private void calcCutoffTable() {
		for(int i = 0; i < 0x800; ++i) {
			cutoff[i] = (float) ((((12000.0-30.0)/2048.0)*i)+30.0);
		}
	}
}
