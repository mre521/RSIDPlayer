package rsidplayer.application;
import java.util.ArrayList;
import rsidplayer.application.SIDSong;

@SuppressWarnings("serial")
class PlayList extends ArrayList<SIDSong> {
	int currentPlayIndex;
	
	PlayList() {
		super();
		currentPlayIndex = -1;
	}
	
	@Override
	public
	boolean add(SIDSong e) {
		boolean result = super.add(e);
		setPlayIndex(0);
		return result;
	}
	
	void setPlayIndex(int index) {
		currentPlayIndex = index;
	}
	
	int getPlayIndex() {
		return currentPlayIndex;
	}
}
