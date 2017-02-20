package rsidplayer.c64;

public class SID8580 extends SID6581 {
	public SID8580(int idSid, C64System memory) {
		super(idSid, memory);
		filter.set_chip_model(SIDFilter.MOS8580);
		calcCutoffTable();
	}
	
	private void calcCutoffTable() {
		for(int i = 0; i < 0x800; ++i) {
			cutoff[i] = (float) ((((20000.0-30.0)/2048.0)*i)+30.0);
		}
	}
}
