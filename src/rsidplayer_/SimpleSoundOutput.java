package rsidplayer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

class SimpleSoundOutput{
	AudioFormat format = null;
	SourceDataLine line = null;

	public SimpleSoundOutput() {
	}

	public boolean open(int sampleRate, int bits, int channels, boolean signed,
			boolean bigEndian, int bufferSize) throws LineUnavailableException {
		if (this.isOpen()) {
			return false;
		}

		format = new AudioFormat(sampleRate, bits, channels, signed, bigEndian);

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		line = (SourceDataLine) AudioSystem.getLine(info);

		line.open(format, bufferSize);
		line.start();
		
		//System.out.println("Buffer size: " + line.getBufferSize());
		return true;
	}

	public void close() {
		if (!this.isOpen()) {
			return;
		}

		//line.drain();
		line.close();
		line = null;
	}

	public boolean isOpen() {
		if (line == null) {
			return false;
		}

		return line.isOpen();
	}

	public int bytesAvailable() {
		if (line == null) {
			return 0;
		}

		if (!this.isOpen()) {
			return 0;
		}

		return line.available();
	}

	public boolean writeSamples(byte[] buffer, int len) {
		if (this.isOpen()) {
			line.write(buffer, 0, len);
			return true;
		} else {
			return false;
		}
	}

	public SimpleSoundOutput(int sampleRate, int bits, int channels,
			boolean signed, boolean bigEndian, int bufferSize) throws LineUnavailableException {
		this.open(sampleRate, bits, channels, signed, bigEndian, bufferSize);
	}
}
