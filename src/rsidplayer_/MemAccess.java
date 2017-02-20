package rsidplayer;

abstract class MemAccess {
	abstract void WriteMemory(int address, int value);
	abstract int ReadMemory(int address);
}
