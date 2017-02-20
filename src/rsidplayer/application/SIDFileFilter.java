package rsidplayer.application;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class SIDFileFilter extends FileFilter implements FilenameFilter {

	@Override
	public boolean accept(File file) {
		if(file != null) {
			if(file.isDirectory())
				return true;
			
			String extension = getExtension(file);
			if( (extension != null) && extension.equals("sid")) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean accept(File arg0, String arg1) {
		return accept(arg0);
	}
	
	String getExtension(File file) {
		if(file != null) {
			String fileName = file.getName();
			int i = fileName.lastIndexOf('.');
			if(i>0 && i<fileName.length()-1) {
				return fileName.substring(i+1).toLowerCase();
			}
		}
		return null;
	}

	@Override
	public String getDescription() {
		return "SIDPlay music files";
	}

}
