package rsidplayer.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class IniFileReader {
	File iniFile;
	FileInputStream inputStream;
	public IniFileReader(File iniFile) throws IOException, FileNotFoundException {
		if(null == iniFile)
			throw new IOException("Null ini File specified!");
		
		if(!iniFile.isFile())
			throw new IOException("Ini File given is not a file: " + iniFile.getAbsolutePath());
		
		if(!iniFile.canRead())
			throw new IOException("Insufficient permissions to read from given ini File: " + iniFile.getAbsolutePath());
	
		this.iniFile = iniFile;
		this.inputStream = new FileInputStream(iniFile);
	}
	
	public int findKey(String key) {
		return 0;
	}
}
