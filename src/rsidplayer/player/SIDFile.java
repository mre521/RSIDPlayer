package rsidplayer.player;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.*;

import rsidplayer.c64.MemAccess;

public class SIDFile {
	private static final char[] MAGIC1 = "PSID".toCharArray();
	private static final char[] MAGIC2 = "RSID".toCharArray();
	public char[] magicID = new char[4];
	
	public String fileName;

	public char version;
	public char dataOffset;
	public char loadAddress;
	public char initAddress;
	public char playAddress;
	public char songs;
	public char startSong;
	public int speed;

	public String title;
	public String author;
	public String released;

	public char flags;
	public char startPage;
	public char pageLength;
	public char reserved;

	public byte[] payload = null;
	public int dataSize;
	
	public String md5;

	private static class RandomAccessFileSID extends RandomAccessFile {
		RandomAccessFileSID(String file, String mode)
				throws java.io.FileNotFoundException {
			super(file, mode);
		}

		public char readUnsignedShortLE() throws java.io.IOException {
			char byte1 = (char) readUnsignedByte();
			char byte2 = (char) readUnsignedByte();

			return (char) ((byte1 & 0xFF) | ((byte2 & 0xFF) << 8));
		}

		public char readUnsignedShortBE() throws java.io.IOException {
			char byte1 = (char) readUnsignedByte();
			char byte2 = (char) readUnsignedByte();

			return (char) ((byte2 & 0xFF) | ((byte1 & 0xFF) << 8));
		}
	}
	
	private static String toHex(byte[] bytes) {
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "x", bi);
	}

	public static SIDFile loadFromFile(String file) throws IOException {
		RandomAccessFileSID raf = new RandomAccessFileSID(file, "r");
		SIDFile sid = new SIDFile();

		sid.magicID[0] = (char) raf.readUnsignedByte();
		sid.magicID[1] = (char) raf.readUnsignedByte();
		sid.magicID[2] = (char) raf.readUnsignedByte();
		sid.magicID[3] = (char) raf.readUnsignedByte();
		
		boolean mismatch1 = false, mismatch2 = false;
		for(int i = 0; i<sid.magicID.length; ++i) {
			if(sid.magicID[i] != MAGIC1[i])
			{
				mismatch1 = true;
			}
			
			if(sid.magicID[i] != MAGIC2[i])
			{
				mismatch2 = true;
			}
		}
		if (mismatch1 && mismatch2) {
			raf.close();
			throw new IOException("Invalid SID file");
		}
		
		sid.version = raf.readUnsignedShortBE();

		sid.dataOffset = raf.readUnsignedShortBE();
		sid.loadAddress = raf.readUnsignedShortBE();
		sid.initAddress = raf.readUnsignedShortBE();
		sid.playAddress = raf.readUnsignedShortBE();

		sid.songs = raf.readUnsignedShortBE();
		sid.startSong = raf.readUnsignedShortBE();

		sid.speed = raf.readInt();

		char[] textarray = new char[32];

		for (int c = 0; c < 32; ++c) {
			textarray[c] = (char) raf.readUnsignedByte();
		}
		sid.title = new String(textarray).replaceAll("\\0000+", "");

		for (int c = 0; c < 32; ++c) {
			textarray[c] = (char) raf.readUnsignedByte();
		}
		sid.author = new String(textarray).replaceAll("\\0000+", "");

		for (int c = 0; c < 32; ++c) {
			textarray[c] = (char) raf.readUnsignedByte();
		}
		sid.released = new String(textarray).replaceAll("\\0000+", "");

		textarray = null;

		if (sid.version == 2) {
			sid.flags = raf.readUnsignedShortBE();
			sid.startPage = (char) raf.readUnsignedByte();
			sid.pageLength = (char) raf.readUnsignedByte();
			sid.reserved = raf.readUnsignedShortBE();
		}

		if (sid.loadAddress == 0) {
				sid.loadAddress = raf.readUnsignedShortLE();
		}

		long position = raf.getFilePointer();
		int payloadsize = 0;

		while (raf.read() != -1) {
			payloadsize++;
		}
		
		sid.dataSize = payloadsize;

		raf.seek(position);

		sid.payload = new byte[payloadsize];
		for (int c = 0; c < payloadsize; ++c) {
			sid.payload[c] = (byte) raf.readUnsignedByte();
		}

		raf.close();
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			md.update((byte)(sid.loadAddress&0xFF));
			md.update( (byte)((sid.loadAddress>>8)&0xFF) );
			
			md.update(sid.payload);

			// add little-endian initAddress
			md.update((byte)(sid.initAddress&0xFF));
			md.update( (byte)((sid.initAddress>>8)&0xFF) );
			
			// ''		''	 	 playAddress
			md.update((byte) (sid.playAddress&0xFF));
			md.update((byte)((sid.playAddress>>8)&0xFF));
			
			// ''		''	 	 number of songs
			md.update((byte) (sid.songs&0xFF));
			md.update((byte)((sid.songs>>8)&0xFF));
			
			for(int s = 1; s < sid.songs; s++) {
				int ciaSpeed = 60;
				int vbiSpeed = 0;
				
				boolean bVbiSpeed = false;
				
				for(int i = 0; i < sid.songs; ++i) {
					if(sid.isPSID()) {
						if(i < 31)
							bVbiSpeed = (sid.speed & (1 << i)) == 0;
						else
							bVbiSpeed = (sid.speed & (1 << 31)) == 0;
					} else {
						bVbiSpeed = (sid.speed & (1 << (i % 32))) == 0;
					}
					
					if(bVbiSpeed)
						md.update((byte) vbiSpeed);
					else
						md.update((byte) ciaSpeed);
					
				}
			}
			
			if(sid.version == 2) {
				if((sid.flags & 0x0c) == 0x08)
					md.update((byte) 2);
			}
			sid.md5 = toHex(md.digest());
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(file);
		sid.fileName = file2.getName();
		
		return sid;
	}
	
	void loadIntoMemory(MemAccess memory) {
		int address = this.loadAddress;
		int index = 0;
		if(this.payload != null) {
			for(index = 0; index < this.dataSize; ++index) {
				memory.WriteMemory(address+index, this.payload[index]);
			}
		}
	}
	
	boolean isRSID() {
		if(this.magicID[0] == MAGIC2[0]) {
			return true;
		}
		
		return false;
	}
	
	boolean isPSID() {
		if(this.magicID[0] == MAGIC1[0]) {
			return true;
		}
		
		return false;
	}
}
