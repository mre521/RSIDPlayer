package rsidplayer.c64;

public abstract class MemAccess {
	public abstract void WriteMemory(int address, int value);
	public abstract int ReadMemory(int address);
}
