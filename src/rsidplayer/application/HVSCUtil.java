package rsidplayer.application;

import java.io.File;

class HVSCUtil {
	private static String hvscDir;	// directory of C64Music
	private static File hvscDirFile;
	
	private static File hvscFile() {
		return new File(hvscDir);
	}
	
	public static String getBaseDirectory() {
		return hvscDir;
	}
	
	public static void setBaseDirectory(String directory) {
		hvscDir = directory;
		hvscDirFile = hvscFile();
	}
	
	public static File getBaseDirectoryFile() {
		return hvscDirFile;
	}
	
	public static File getSonglengthFile() {
		File[] directoryContents;
		
		if(hvscDirFile == null) {
			return null;
		}
		
		File docDir = new File(hvscDirFile.getAbsolutePath() + "\\DOCUMENTS");
		
		directoryContents = docDir.listFiles();
		for(File content : directoryContents) {
			if(content.getName().equals("Songlengths.txt") && content.isFile()) {
				return content;
			}
		}
		return null;
	}
	
	
}
