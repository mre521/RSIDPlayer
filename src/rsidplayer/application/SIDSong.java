package rsidplayer.application;
import rsidplayer.player.SIDFile;
import java.io.File;

public class SIDSong {
	private SIDFile sidFile;
	private File file;
	
	private int playTimeSeconds;
	static int defaultPlayTimeSeconds = 60*4;	// five minutes
	
	public static void setDefaultPlayTime(int seconds) {
		defaultPlayTimeSeconds = seconds;
	}
	public static int getDefaultPlayTime() {
		return defaultPlayTimeSeconds;
	}
	
	public SIDSong(SIDFile sidFile) {
		this.sidFile = sidFile;
		
		if(this.sidFile.fileName != null) {
			this.file = new File(this.sidFile.fileName);
		}
		playTimeSeconds = defaultPlayTimeSeconds;
	}
	
	public SIDSong(SIDFile sidFile, int playTimeSeconds) {
		this(sidFile);
		this.playTimeSeconds = playTimeSeconds;
	}
	
	SIDFile getSIDFile() {
		return sidFile;
	}
	
	File getFile() {
		return file;
	}
	
	int getPlayTime() {
		return playTimeSeconds;
	}
}
