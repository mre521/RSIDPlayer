package rsidplayer.c64;

//import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;

import rsidplayer.c64.C64System;

public class CPU6510 {
	// the 6510 registers
	private int A; // accumulator
	private int X; // index x
	private int Y; // index y
	private int PC; // program counter
	private int SP; // stack pointer
	private int P; // status register

	// status flag bits
	private final int CARRY_FLAG = 0;
	private final int ZERO_FLAG = 1;
	private final int INT_FLAG = 2;
	private final int BCD_FLAG = 3;
	private final int BRK_FLAG = 4;

	private final int OVERFLOW_FLAG = 6;
	private final int NEGATIVE_FLAG = 7;

	// internal operations variables
	private int operand; // one-byte operand of an instruction
	private int operandAddr; // two-byte (address) operand of an instruction
	private int[] instruction = new int[3]; // three-byte (max) instruction
											// array
	private int tmp, tmp2; // temporary storage for operations
	private int h, l; // high and low bytes of addresses
	private boolean pageCrossed = false;

	private C64System memory;
	private RandomAccessFile dump;

	public CPU6510(C64System access, String dumpFile) {
		memory = access;
		if(dumpFile != null)
			try {
				dump = new RandomAccessFile(dumpFile, "rw");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		
		for(int i = 0; i < 256; ++i) {
			instr_freq[i].opcode = i;
		}
		Reset();
	}

	public void Reset() {
		A = X = Y = 0;
		PC = memory.ReadMemory(0xFFFC) | (memory.ReadMemory(0xFFFD)<<8);

		SP = 0x1FF;

		P = 0;
		SEI();
		
		instr_count = 0;
		//instr_freq = new long[256];
	}

	private void ReadPC(int x) {
		instruction[x] = memory.ReadMemory(PC);
		PC++;
	}

	private void OneByte() {
		return;
	}

	private void TwoByte() {
		ReadPC(1);
	}

	private void ThreeByte() {
		ReadPC(1);
		ReadPC(2);
	}
	
	int PageFromAddress(int addr) {
		return addr>>8;
	}
	
	int byte1, byte2, tempAddr;

	// Operation Addressing mode setup methods
	// addressing is implied by the instruction
	private void Implied() {
	}

	// read-modify-write the accumulator
	private void Accumulator() {
		operand = A;
	}

	// uses an immediately given value for operations
	private void Immediate() {
		operand = instruction[1];
	}

	// like absolute, but for zero page (first 256 bytes of ram)
	private void ZeroPage() {
		operandAddr = instruction[1] & 0xFF;
		operand = memory.ReadMemory(operandAddr);
	}

	// like absolute x, but for zero page (first 256 bytes of ram)
	private void ZeroPageX() {
		operandAddr = (instruction[1] + X) & 0xFF;
		operand = memory.ReadMemory(operandAddr);
	}

	// like absolute y, but for zero page (first 256 bytes of ram)
	private void ZeroPageY() {
		operandAddr = (instruction[1] + Y) & 0xFF;
		operand = memory.ReadMemory(operandAddr);
	}

	// for branch instructions. address relative to PC
	private void Relative() {
		operandAddr = (byte) instruction[1];
	}

	// read from absolute 16 bit address
	private void Absolute() {
		operandAddr = (instruction[1] | (instruction[2] << 8)) & 0xFFFF;
		operand = memory.ReadMemory(operandAddr);
	}

	// read an absolute 16 bit address plus X
	private void AbsoluteX() {
		/*tmp = (instruction[1] | (instruction[2] << 8));
		operandAddr = (tmp+X) & 0xFFFF;
		
		if(PageFromAddress(tmp) != PageFromAddress(operandAddr)) {
			pageCrossed = true;
		}
		
		operand = memory.ReadMemory(operandAddr);*/
		operandAddr = ((instruction[1] | (instruction[2] << 8)) + X) & 0xFFFF;
		operand = memory.ReadMemory(operandAddr);
	}

	// read an absolute 16 bit address plus Y
	private void AbsoluteY() {
		tmp = (instruction[1] | (instruction[2] << 8));
		operandAddr = (tmp+Y) & 0xFFFF;
		
		if(PageFromAddress(tmp) != PageFromAddress(operandAddr)) {
			pageCrossed = true;
		}
		
		operand = memory.ReadMemory(operandAddr);
	}

	// reads a pointer for jump target
	private void Indirect() {
		operandAddr = instruction[1] | (instruction[2] << 8);
		operandAddr = memory.ReadMemory(operandAddr)
				| (memory.ReadMemory(operandAddr + 1) << 8);
	}

	// reads value from address retrieved from zero page plus X
	private void IndirectX() {
		operandAddr = instruction[1] & 0xFF;		// initial zero page address
		
		operandAddr = (operandAddr + X)&0xFF;		// add X to the zp addr with wraparound
		l = (char) memory.ReadMemory(operandAddr);  // read low byte of target address
		operandAddr = (operandAddr+1)&0xFF;			// add 1 to the zp addr with wraparound
		h = (char) memory.ReadMemory(operandAddr);	// read the high byte of target address
		
		operandAddr = (l | (h<<8));					// combine high and low byte into an address

		operand = memory.ReadMemory(operandAddr);	// read the memory location
	}

	// reads address retrieved from zero page, adds Y, then retrieves value
	private void IndirectY() {
		
		operandAddr = instruction[1] & 0xFF;	// initial address from zero page
		
		l = (char) memory.ReadMemory(operandAddr);
		operandAddr = (operandAddr+1)&0xFF;
		h = (char) memory.ReadMemory(operandAddr);
		
		tmp = (l | (h<<8));
		operandAddr = (tmp+Y) & 0xFFFF;
		
		if(PageFromAddress(tmp) != PageFromAddress(operandAddr)) {
			pageCrossed = true;
		}
		
		operand = memory.ReadMemory(operandAddr);
	}

	int GetStatusBit(int bit) {
		return (P >> bit) & 1;
	}

	void SetStatusBit(int bit, int value) {
		P = (P & ~(1 << bit)) | ((value & 1) << bit);
		return;
	}

	public int ToBCD(int a) {
		return (a - ((a / 10) * 10)) | ((a / 10) << 4);
	}

	// Operation base methods
	// ADC - Add Memory to A with Carry
	private void ADC() {
		int a = (A&0xFF) + (operand&0xFF);
		
		/*if(GetStatusBit(CARRY_FLAG) == 1)
			a++;
		
		if(A > 127) A -= 256;
		if(operand > 127) operand -= 256;
		
		int result = A + operand;
		if(GetStatusBit(CARRY_FLAG) == 1)
			result++;
		
		SetStatusBit(CARRY_FLAG, (a > 255) ? 1 : 0);		
		A = a & 0xFF;

		SetStatusBit(NEGATIVE_FLAG, (A >> 7) & 1);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
		
		SetStatusBit(OVERFLOW_FLAG, ((a < -128) | (a > 127)) ? 1 : 0);*/
		
		if(GetStatusBit(CARRY_FLAG) == 1) a++;
		SetStatusBit(CARRY_FLAG, (a > 255) ? 1 : 0);
		
		int sA = (A > 127) ? (A-256) : (A); // signed A
		int sOp = (operand > 127) ? (operand-256) : (operand); // signed A
		int sa = sA + sOp;
		SetStatusBit(OVERFLOW_FLAG, ( ((A&0x80) != (sa&0x80)) && ((sa < -128) | (sa > 127)) ) ? 1 : 0);
		
		A = a & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, (A >> 7) & 1);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
		
	}

	// AND - Bitwise-AND A with Memory
	private void AND() {
		A = A & operand & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// ASL - Arithmetic Shift Left
	private void ASL() {
		operand &= 0xFF;
		SetStatusBit(CARRY_FLAG, operand >> 7);
		operand = (operand << 1) & 0xFE;
		SetStatusBit(NEGATIVE_FLAG, operand >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// BCC - Branch if P.C is CLEAR
	private void BCC() {
		if (GetStatusBit(CARRY_FLAG) == 0) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;
		}
	}

	// BCS - Branch if P.C is SET
	private void BCS() {
		if (GetStatusBit(CARRY_FLAG) == 1) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;
		}
	}

	// BEQ - Branch if P.Z is SET
	private void BEQ() {
		if (GetStatusBit(ZERO_FLAG) == 1) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;
		}
	}

	// BIT - Test bits in A with M
	private void BIT() {
		tmp = A & operand & 0xFF;
		
		SetStatusBit(NEGATIVE_FLAG, (operand >> 7) & 1);
		SetStatusBit(OVERFLOW_FLAG, (operand >> 6) & 1);
		SetStatusBit(ZERO_FLAG, (tmp == 0) ? 1 : 0);
	}

	// BMI - Branch if P.N is SET
	private void BMI() {
		if (GetStatusBit(NEGATIVE_FLAG) == 1) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;	
		}
	}

	// BNE - Branch if P.Z is CLEAR
	private void BNE() {
		if (GetStatusBit(ZERO_FLAG) == 0) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;	
		}
	}

	// BPL - Branch if P.N is CLEAR
	private void BPL() {
		if (GetStatusBit(NEGATIVE_FLAG) == 0) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;	
		}
	}

	// BRK - Simulate Interrupt ReQuest (IRQ)
	private void BRK() {
		// push pc into stack
//		++PC;
		memory.WriteMemory(SP, PC>>8);
		SP = (SP - 1) & 0x1FF;
		memory.WriteMemory(SP, PC&0xFF);
		SP = (SP - 1) & 0x1FF;
		
		// push status to stack
		memory.WriteMemory(SP, P|0x10);
		SP = (SP - 1) & 0x1FF;
		
		SetStatusBit(INT_FLAG, 1);
		
		l = memory.ReadMemory(0xFFFE);
		h = memory.ReadMemory(0xFFFF);
		
		PC = (l | (h<<8));
	} 

	// BVC - Branch if P.V is CLEAR
	private void BVC() {
		if (GetStatusBit(OVERFLOW_FLAG) == 0) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;	
		}
	}

	// BVS - Branch if P.V is SET
	private void BVS() {
		if (GetStatusBit(OVERFLOW_FLAG) == 1) {
			instrCycles++;	// extra cycle if branch taken 
			
			if(PageFromAddress(PC) != PageFromAddress(operandAddr))
				instrCycles++;	// yet another cycle if page boundary is crossed
			
			PC += operandAddr;	
		}
	}

	// CLC - Clear Carry Flag (P.C)
	private void CLC() {
		SetStatusBit(CARRY_FLAG, 0);
	}

	// CLD = Clear Decimal Flag (P.D)
	private void CLD() {
		SetStatusBit(BCD_FLAG, 0);
	}

	// CLI - Clear Interrupt (disable) Flag (P.I)
	private void CLI() {
		SetStatusBit(INT_FLAG, 0);
	}

	// CLV - Clear oVerflow Flag (P.V)
	private void CLV() {
		SetStatusBit(OVERFLOW_FLAG, 0);
	}

	// CMP - Compare A with Memory
	private void CMP() {
		tmp2 = ((A >> 7) == 1) ? (0xFFFFFF00 | A) : (A);
		tmp = tmp2 - operand;
		tmp &= 0xFF;
		SetStatusBit(NEGATIVE_FLAG, tmp >> 7);
		SetStatusBit(CARRY_FLAG, (A >= operand) ? 1 : 0);
		SetStatusBit(ZERO_FLAG, (tmp == 0) ? 1 : 0);
	}

	// CPX - Compare X with Memory
	private void CPX() {
		tmp2 = ((X >> 7) == 1) ? (0xFFFFFF00 | X) : (X);
		tmp = X - operand;
		tmp &= 0xFF;
		SetStatusBit(NEGATIVE_FLAG, tmp >> 7);
		SetStatusBit(CARRY_FLAG, (X >= operand) ? 1 : 0);
		SetStatusBit(ZERO_FLAG, (tmp == 0) ? 1 : 0);
	}

	// CPY - Compare Y with Memory
	private void CPY() {
		tmp2 = ((Y >> 7) == 1) ? (0xFFFFFF00 | Y) : (Y);
		tmp = Y - operand;
		tmp &= 0xFF;
		SetStatusBit(NEGATIVE_FLAG, tmp >> 7);
		SetStatusBit(CARRY_FLAG, (Y >= operand) ? 1 : 0);
		SetStatusBit(ZERO_FLAG, (tmp == 0) ? 1 : 0);
	}

	// DEC - Decrement Memory by one
	private void DEC() {
		operand = (operand - 1) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, operand >> 7);
		SetStatusBit(ZERO_FLAG, (operand == 0) ? 1 : 0);
	}

	// DEX - Decrement X by one
	private void DEX() {
		X = (X - 1) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, X >> 7);
		SetStatusBit(ZERO_FLAG, (X == 0) ? 1 : 0);
	}

	// DEY - Decrement Y by one
	private void DEY() {
		Y = (Y - 1) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, Y >> 7);
		SetStatusBit(ZERO_FLAG, (Y == 0) ? 1 : 0);
	}

	// EOR - Bitwise-EXclusive-OR A with Memory
	private void EOR() {
		A ^= operand;
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// INC - Increment Memory by one
	private void INC() {
		operand = (operand + 1) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, operand >> 7);
		SetStatusBit(ZERO_FLAG, (operand == 0) ? 1 : 0);
	}

	// INX - Increment X by one
	private void INX() {
		X = (X + 1) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, X >> 7);
		SetStatusBit(ZERO_FLAG, (X == 0) ? 1 : 0);
	}

	// INX - Increment Y by one
	private void INY() {
		Y = (Y + 1) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, Y >> 7);
		SetStatusBit(ZERO_FLAG, (Y == 0) ? 1 : 0);
	}

	// JMP - GOTO Address
	private void JMP() {
		PC = operandAddr;
	}

	// JSR - Jump to SubRoutine
	private void JSR() {
		tmp = PC - 1;
		memory.WriteMemory(SP, tmp >> 8);
		SP = (SP - 1) & 0x1FF;
		memory.WriteMemory(SP, tmp & 0xFF);
		SP = (SP - 1) & 0x1FF;

		PC = operandAddr;
	}

	// LDA - Load A with Memory
	private void LDA() {
		A = operand;
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// LDX - Load X with Memory
	private void LDX() {
		X = operand;
		SetStatusBit(NEGATIVE_FLAG, X >> 7);
		SetStatusBit(ZERO_FLAG, (X == 0) ? 1 : 0);
	}

	// LDY - Load Y with Memory
	private void LDY() {
		Y = operand;
		SetStatusBit(NEGATIVE_FLAG, Y >> 7);
		SetStatusBit(ZERO_FLAG, (Y == 0) ? 1 : 0);
	}

	private void LSR() {
		SetStatusBit(NEGATIVE_FLAG, 0);
		SetStatusBit(CARRY_FLAG, operand & 1);
		operand = (operand >>> 1) & 0x7F;
		SetStatusBit(ZERO_FLAG, (operand == 0) ? 1 : 0);
	}

	// NOP - No OPeration
	private void NOP() {
		// that's it
	}

	// ORA - Bitwise-OR A with Memory
	private void ORA() {
		A = (A | operand) & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// PHA - PusH A onto Stack
	private void PHA() {
		memory.WriteMemory(SP, A);
		SP = (SP - 1) & 0x1FF;
	}

	// PHP - PusH P onto Stack
	private void PHP() {
		memory.WriteMemory(SP, P);
		SP = (SP - 1) & 0x1FF;
	}

	// PLA - PulL from Stack to A
	private void PLA() {
		SP = (SP + 1) & 0x1FF;
		A = memory.ReadMemory(SP);
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// PLP - PulL from Stack to P
	private void PLP() {
		SP = (SP + 1) & 0x1FF;
		P = memory.ReadMemory(SP);
	}

	// ROL - ROtate Left
	private void ROL() {
		tmp = (operand >> 7) & 1;
		operand = (operand << 1) & 0xFE;
		operand |= GetStatusBit(CARRY_FLAG);
		SetStatusBit(CARRY_FLAG, tmp);
		SetStatusBit(ZERO_FLAG, (operand == 0) ? 1 : 0);
		SetStatusBit(NEGATIVE_FLAG, operand >> 7);
	}

	// ROR - ROtate Right
	private void ROR() {
		tmp = operand & 1;
		operand = (operand >> 1) & 0x7F;
		operand |= (GetStatusBit(CARRY_FLAG) == 1) ? 0x80 : 0x00;
		SetStatusBit(CARRY_FLAG, tmp);
		SetStatusBit(ZERO_FLAG, (operand == 0) ? 1 : 0);
		SetStatusBit(NEGATIVE_FLAG, operand >> 7);
	}

	// RTI - ReTurn from Interrupt
	private void RTI() {
		SP = (SP + 1) & 0x1FF;
		P = memory.ReadMemory(SP);
		SP = (SP + 1) & 0x1FF;
		l = memory.ReadMemory(SP);
		SP = (SP + 1) & 0x1FF;
		h = memory.ReadMemory(SP) << 8;
		PC = h | l;
	}

	// RTS - ReTurn from Subroutine
	private void RTS() {
		SP = (SP + 1) & 0x1FF;
		l = memory.ReadMemory(SP);
		SP = (SP + 1) & 0x1FF;
		h = memory.ReadMemory(SP) << 8;
		PC = (h | l) + 1;
	}

	// SBC - Subtract Memory from A with Carry (Borrow)
	private void SBC() {
		//operand = (operand ^ -1);
		//ADC();
		int a = (A&0xFF) - (operand&0xFF);
		
		/*if(GetStatusBit(CARRY_FLAG) == 1)
			a++;
		
		if(A > 127) A -= 256;
		if(operand > 127) operand -= 256;
		
		int result = A + operand;
		if(GetStatusBit(CARRY_FLAG) == 1)
			result++;
		
		SetStatusBit(CARRY_FLAG, (a > 255) ? 1 : 0);		
		A = a & 0xFF;

		SetStatusBit(NEGATIVE_FLAG, (A >> 7) & 1);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
		
		SetStatusBit(OVERFLOW_FLAG, ((a < -128) | (a > 127)) ? 1 : 0);*/
		
		if(GetStatusBit(CARRY_FLAG) == 0) a--;
		SetStatusBit(CARRY_FLAG, (a < 0) ? 0 : 1);
		
		int sA = (A > 127) ? (A-256) : (A); // signed A
		int sOp = (operand > 127) ? (operand-256) : (operand); // signed A
		int sa = sA - sOp;
		SetStatusBit(OVERFLOW_FLAG, ( ((A&0x80) != (sa&0x80)) && ((sa < -128) | (sa > 127)) ) ? 1 : 0);
		
		A = a & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, (A >> 7) & 1);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// SEC - Set Carry flag (P.C)
	private void SEC() {
		SetStatusBit(CARRY_FLAG, 1);
	}

	// SED - Set Binary Coded Decimal Flag (P.D)
	private void SED() {
		SetStatusBit(BCD_FLAG, 1);
	}

	// SEI - Set Interrupt (disable) Flag (P.I)
	private void SEI() {
		SetStatusBit(INT_FLAG, 1);
	}

	// STA - Store A in Memory
	private void STA() {
		operand = A;
	}

	// STX - Store X in Memory
	private void STX() {
		operand = X;
	}

	// STY - Store Y in Memory
	private void STY() {
		operand = Y;
	}

	// TAX - Transfer A to X
	private void TAX() {
		X = A;
		SetStatusBit(NEGATIVE_FLAG, X >> 7);
		SetStatusBit(ZERO_FLAG, (X == 0) ? 1 : 0);
	}

	// TAY - Transfer A to Y
	private void TAY() {
		Y = A;
		SetStatusBit(NEGATIVE_FLAG, Y >> 7);
		SetStatusBit(ZERO_FLAG, (Y == 0) ? 1 : 0);
	}

	// TSX - Transfer SP to X
	private void TSX() {
		X = SP & 0xFF;
		SetStatusBit(NEGATIVE_FLAG, X >> 7);
		SetStatusBit(ZERO_FLAG, (X == 0) ? 1 : 0);
	}

	// TXA - Transfer X to A
	private void TXA() {
		A = X;
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// TXS - Transfer X to SP
	private void TXS() {
		SP = (SP & 0x100) | X;
	}

	// TYA - Transfer Y to A
	private void TYA() {
		A = Y;
		SetStatusBit(NEGATIVE_FLAG, A >> 7);
		SetStatusBit(ZERO_FLAG, (A == 0) ? 1 : 0);
	}

	// END of instructions
	
	public class CPUBreakException extends Exception
	{
		// the 6502 registers
		private int A; // accumulator
		private int X; // index x
		private int Y; // index y
		private int PC; // program counter
		private int SP; // stack pointer
		private int P; // status register

		// internal operations variables
		private int operand; // one-byte operand of an instruction
		private int operandAddr; // two-byte (address) operand of an instruction
		private int[] instruction = new int[3]; // three-byte (max) instruction
												// array
		private int tmp, tmp2; // temporary storage for operations
		private int h, l; // high and low bytes of addresses
		private boolean pageCrossed;
		
		private C64System memory;
		
		CPUBreakException(CPU6510 cpu) {
			this.A = cpu.A;
			this.X = cpu.X;
			this.Y = cpu.Y;
			this.PC = cpu.PC;
			this.SP = cpu.SP;
			this.P = cpu.P;
			this.operand = cpu.operand;
			this.operandAddr = cpu.operandAddr;
			for(int i = 0; i < 3; ++i) {
				this.instruction[i] = cpu.instruction[i];
			}
			this.tmp = cpu.tmp;
			this.tmp2 = cpu.tmp2;
			this.h = cpu.h;
			this.l = cpu.l;
			this.pageCrossed = cpu.pageCrossed;
			this.memory = cpu.memory;
		}
		
		@Override
		public
		void printStackTrace() {
			System.out.print("A: " + Integer.toHexString(A) + "  ");
			System.out.print("P: " + Integer.toHexString(P) + "  ");
			System.out.print("X: " + Integer.toHexString(X) + "  ");
			System.out.print("Y: " + Integer.toHexString(Y) + "  ");
			System.out.print("S: " + Integer.toHexString(SP) + "  ");
			System.out.print("PC: " + Integer.toHexString(PC) + "  ");
			
			System.out.print("Flags:[");
			
			if(GetStatusBit(NEGATIVE_FLAG)==1)
				System.out.print('N');
			else
				System.out.print('.');
			
			if(GetStatusBit(OVERFLOW_FLAG)==1)
				System.out.print('V');
			else
				System.out.print('.');
			
			System.out.print('.');
			
			if(GetStatusBit(BRK_FLAG)==1)
				System.out.print('B');
			else
				System.out.print('.');
			
			if(GetStatusBit(BCD_FLAG)==1)
				System.out.print('D');
			else
				System.out.print('.');
			
			if(GetStatusBit(INT_FLAG)==1)
				System.out.print('I');
			else
				System.out.print('.');
			
			if(GetStatusBit(ZERO_FLAG)==1)
				System.out.print('Z');
			else
				System.out.print('.');
			
			if(GetStatusBit(CARRY_FLAG)==1)
				System.out.print('C');
			else
				System.out.print('.');
			
			System.out.println(']');
			
			System.out.println("VECTORS:\n");
			System.out.println("NMI: $" + Integer.toHexString(memory.ReadMemory(0xFFFA)|memory.ReadMemory(0xFFFB)<<8));
			System.out.println("RESET: $" + Integer.toHexString(memory.ReadMemory(0xFFFC)|memory.ReadMemory(0xFFFD)<<8));
			System.out.println("IRQ: $" + Integer.toHexString(memory.ReadMemory(0xFFFE)|memory.ReadMemory(0xFFFF)<<8));
			
			System.out.println();
			
			System.out.println("OPCODE: [$" + Integer.toHexString(instruction[0]) + "]\n");
			System.out.println("PREV. INTRUCTION: [$" + Integer.toHexString(prevInstruction) + "]\n");
		}
		
	}
	
	
	private void DebugInterface() {
		System.out.print("A: " + Integer.toHexString(A) + "  ");
		System.out.print("P: " + Integer.toHexString(P) + "  ");
		System.out.print("X: " + Integer.toHexString(X) + "  ");
		System.out.print("Y: " + Integer.toHexString(Y) + "  ");
		System.out.print("S: " + Integer.toHexString(SP) + "  ");
		System.out.print("PC: " + Integer.toHexString(PC) + "  ");
		
		System.out.print("Flags:[");
		
		if(GetStatusBit(NEGATIVE_FLAG)==1)
			System.out.print('N');
		else
			System.out.print('.');
		
		if(GetStatusBit(OVERFLOW_FLAG)==1)
			System.out.print('V');
		else
			System.out.print('.');
		
		System.out.print('.');
		
		if(GetStatusBit(BRK_FLAG)==1)
			System.out.print('B');
		else
			System.out.print('.');
		
		if(GetStatusBit(BCD_FLAG)==1)
			System.out.print('D');
		else
			System.out.print('.');
		
		if(GetStatusBit(INT_FLAG)==1)
			System.out.print('I');
		else
			System.out.print('.');
		
		if(GetStatusBit(ZERO_FLAG)==1)
			System.out.print('Z');
		else
			System.out.print('.');
		
		if(GetStatusBit(CARRY_FLAG)==1)
			System.out.print('C');
		else
			System.out.print('.');
		
		System.out.println(']');
		
		System.out.println("VECTORS:\n");
		System.out.println("NMI: $" + Integer.toHexString(memory.ReadMemory(0xFFFA)|memory.ReadMemory(0xFFFB)<<8));
		System.out.println("RESET: $" + Integer.toHexString(memory.ReadMemory(0xFFFC)|memory.ReadMemory(0xFFFD)<<8));
		System.out.println("IRQ: $" + Integer.toHexString(memory.ReadMemory(0xFFFE)|memory.ReadMemory(0xFFFF)<<8));
		
		System.out.println();
		
		System.out.println("OPCODE: [$" + Integer.toHexString(instruction[0]) + "]\n");
		System.out.println("PREV. INTRUCTION: [$" + Integer.toHexString(prevInstruction) + "]\n");
		System.out.print("press Enter to step: ");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		in = null;
	}
	
	public static class Instruction {
		int type;
		int addr_mode;
		int exec_count;
		public int opcode;
		
		public static final int ADC = 0;
		public static final int AND = 1;
		public static final int ASL = 2;
		public static final int BCC = 3;
		public static final int BCS = 4;
		public static final int BEQ = 5;
		public static final int BIT = 6;
		public static final int BMI = 7;
		public static final int BNE = 8;
		public static final int BPL = 9;
		public static final int BRK = 10;
		public static final int BVC = 11;
		public static final int BVS = 12;
		public static final int CLC = 13;
		public static final int CLD = 14;
		public static final int CLI = 15;
		public static final int CLV = 16;
		public static final int CMP = 17;
		public static final int CPX = 18;
		public static final int CPY = 19;
		public static final int DEC = 20;
		public static final int DEX = 21;
		public static final int DEY = 22;
		public static final int EOR = 23;
		public static final int INC = 24;
		public static final int INX = 25;
		public static final int INY = 26;
		public static final int JMP = 27;
		public static final int JSR = 28;
		public static final int LDA = 29;
		public static final int LDX = 30;
		public static final int LDY = 31;
		public static final int LSR = 32;
		public static final int NOP = 33;
		public static final int ORA = 34;
		public static final int PHA = 35;
		public static final int PHP = 36;
		public static final int PLA = 37;
		public static final int PLP = 38;
		public static final int ROL = 39;
		public static final int ROR = 40;
		public static final int RTI = 41;
		public static final int RTS = 42;
		public static final int SBC = 43;
		public static final int SEC = 44;
		public static final int SED = 45;
		public static final int SEI = 46;
		public static final int STA = 47;
		public static final int STX = 48;
		public static final int STY = 49;
		public static final int TAX = 50;
		public static final int TAY = 51;
		public static final int TSX = 52;
		public static final int TXA = 53;
		public static final int TXS = 54;
		public static final int TYA = 55;
		public static final int ILL = 56;
		
		static final String[] name = { 
				"ADC", "AND", "ASL", "BCC", "BCS", "BEQ", "BIT",
				"BMI", "BNE", "BPL", "BRK", "BVC", "BVS", "CLC", "CLD", "CLI",
				"CLV", "CMP", "CPX", "CPY", "DEC", "DEX", "DEY", "EOR", "INC",
				"INX", "INY", "JMP", "JSR", "LDA", "LDX", "LDY", "LSR", "NOP",
				"ORA", "PHA", "PHP", "PLA", "PLP", "ROL", "ROR", "RTI", "RTS",
				"SBC", "SEC", "SED", "SEI", "STA", "STX", "STY", "TAX", "TAY",
				"TSX", "TXA", "TXS", "TYA", "???"
		};
		
		public static final int ADDR_IMP = 0;
		public static final int ADDR_ACC = 1;
		public static final int ADDR_IMM = 2;
		public static final int ADDR_ZP = 3;
		public static final int ADDR_ZPX = 4;
		public static final int ADDR_ZPY = 5;
		public static final int ADDR_REL = 6;
		public static final int ADDR_ABS = 7;
		public static final int ADDR_ABX = 8;
		public static final int ADDR_ABY = 9;
		public static final int ADDR_IND = 10;
		public static final int ADDR_INDX = 11;
		public static final int ADDR_INDY = 12;
		public static final int ADDR_ILL = 13;
		
		static final String[] addrName = { 
			"Implied", "Accumulator", "Immediate", "Zero Page",
			"Zero Page, X", "Zero Page, Y", "Relative", "Absolute",
			"Absolute, X", "Absolute, Y", "Indirect", "Indexed Indirect (X)",
			"Indirect Indexed (Y)", "???"
		};
		
		public Instruction(int iType, int iMode) {
			this.type = iType;
			this.addr_mode = iMode;
			this.exec_count = 0;
		}
		
		public String getFullName() {
			return name[this.type] + " " + addrName[this.addr_mode];
		}
		
		public void exec() {
			exec_count++;
		}
		
		public long getExecCount() {
			return exec_count;
		}
	}
	
	private class InstructionComparator implements Comparator<Instruction> {

		@Override
		public int compare(Instruction o1, Instruction o2) {
			
			return -(o1.exec_count-o2.exec_count);
		}
	}
	
	private long instr_count = 0;
	//public long[] instr_freq = new long[256];
	
	private final Instruction[] instr_freq = {
		new Instruction(Instruction.BRK, Instruction.ADDR_IMP), 
		new Instruction(Instruction.ORA, Instruction.ADDR_INDX), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ORA, Instruction.ADDR_ZP),
		new Instruction(Instruction.ASL, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.PHP, Instruction.ADDR_IMP),
		new Instruction(Instruction.ORA, Instruction.ADDR_IMM),
		new Instruction(Instruction.ASL, Instruction.ADDR_ACC),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ORA, Instruction.ADDR_ABS),
		new Instruction(Instruction.ASL, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BPL, Instruction.ADDR_REL),
		new Instruction(Instruction.ORA, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ORA, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ASL, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CLC, Instruction.ADDR_IMP),
		new Instruction(Instruction.ORA, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ORA, Instruction.ADDR_ABX),
		new Instruction(Instruction.ASL, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.JSR, Instruction.ADDR_ABS),
		new Instruction(Instruction.AND, Instruction.ADDR_INDX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BIT, Instruction.ADDR_ZP), 
		new Instruction(Instruction.AND, Instruction.ADDR_ZP),
		new Instruction(Instruction.ROL, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.PLP, Instruction.ADDR_IMP),
		new Instruction(Instruction.AND, Instruction.ADDR_IMM),
		new Instruction(Instruction.ROL, Instruction.ADDR_ACC),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BIT, Instruction.ADDR_ABS), 
		new Instruction(Instruction.AND, Instruction.ADDR_ABS),
		new Instruction(Instruction.ROL, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BMI, Instruction.ADDR_REL),
		new Instruction(Instruction.AND, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.AND, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ROL, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.SEC, Instruction.ADDR_IMP),
		new Instruction(Instruction.AND, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.AND, Instruction.ADDR_ABX),
		new Instruction(Instruction.ROL, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.RTI, Instruction.ADDR_IMP),
		new Instruction(Instruction.EOR, Instruction.ADDR_INDX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.EOR, Instruction.ADDR_ZP),
		new Instruction(Instruction.LSR, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.PHA, Instruction.ADDR_IMP),
		new Instruction(Instruction.EOR, Instruction.ADDR_IMM),
		new Instruction(Instruction.LSR, Instruction.ADDR_ACC),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.JMP, Instruction.ADDR_ABS),
		new Instruction(Instruction.EOR, Instruction.ADDR_ABS),
		new Instruction(Instruction.LSR, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BVC, Instruction.ADDR_REL),
		new Instruction(Instruction.EOR, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.EOR, Instruction.ADDR_ZPX),
		new Instruction(Instruction.LSR, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CLI, Instruction.ADDR_IMP),
		new Instruction(Instruction.EOR, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.EOR, Instruction.ADDR_ABX),
		new Instruction(Instruction.LSR, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.RTS, Instruction.ADDR_IMP),
		new Instruction(Instruction.ADC, Instruction.ADDR_INDX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ADC, Instruction.ADDR_ZP),
		new Instruction(Instruction.ROR, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.PLA, Instruction.ADDR_IMP),
		new Instruction(Instruction.ADC, Instruction.ADDR_IMM),
		new Instruction(Instruction.ROR, Instruction.ADDR_ACC),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.JMP, Instruction.ADDR_IND),
		new Instruction(Instruction.ADC, Instruction.ADDR_ABS),
		new Instruction(Instruction.ROR, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BVS, Instruction.ADDR_REL),
		new Instruction(Instruction.ADC, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ADC, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ROR, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.SEI, Instruction.ADDR_IMP),
		new Instruction(Instruction.ADC, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL), 
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ADC, Instruction.ADDR_ABX),
		new Instruction(Instruction.ROR, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.STA, Instruction.ADDR_INDX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.STY, Instruction.ADDR_ZP),
		new Instruction(Instruction.STA, Instruction.ADDR_ZP),
		new Instruction(Instruction.STX, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.DEY, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.TXA, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.STY, Instruction.ADDR_ABS),
		new Instruction(Instruction.STA, Instruction.ADDR_ABS),
		new Instruction(Instruction.STX, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BCC, Instruction.ADDR_REL),
		new Instruction(Instruction.STA, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.STY, Instruction.ADDR_ZPX),
		new Instruction(Instruction.STA, Instruction.ADDR_ZPX),
		new Instruction(Instruction.STX, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.TYA, Instruction.ADDR_IMP),
		new Instruction(Instruction.STA, Instruction.ADDR_ABY),
		new Instruction(Instruction.TXS, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.STA, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.LDY, Instruction.ADDR_IMM),
		new Instruction(Instruction.LDA, Instruction.ADDR_INDX),
		new Instruction(Instruction.LDX, Instruction.ADDR_IMM),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.LDY, Instruction.ADDR_ZP),
		new Instruction(Instruction.LDA, Instruction.ADDR_ZP),
		new Instruction(Instruction.LDX, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.TAY, Instruction.ADDR_IMP),
		new Instruction(Instruction.LDA, Instruction.ADDR_IMM),
		new Instruction(Instruction.TAX, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.LDY, Instruction.ADDR_ABS),
		new Instruction(Instruction.LDA, Instruction.ADDR_ABS),
		new Instruction(Instruction.LDX, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BCS, Instruction.ADDR_REL),
		new Instruction(Instruction.LDA, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.LDY, Instruction.ADDR_ZPX),
		new Instruction(Instruction.LDA, Instruction.ADDR_ZPX),
		new Instruction(Instruction.LDX, Instruction.ADDR_ZPY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CLV, Instruction.ADDR_IMP),
		new Instruction(Instruction.LDA, Instruction.ADDR_ABY),
		new Instruction(Instruction.TSX, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.LDY, Instruction.ADDR_ABX),
		new Instruction(Instruction.LDA, Instruction.ADDR_ABX),
		new Instruction(Instruction.LDX, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CPY, Instruction.ADDR_IMM),
		new Instruction(Instruction.CMP, Instruction.ADDR_INDX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CPY, Instruction.ADDR_ZP),
		new Instruction(Instruction.CMP, Instruction.ADDR_ZP),
		new Instruction(Instruction.DEC, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.INY, Instruction.ADDR_IMP),
		new Instruction(Instruction.CMP, Instruction.ADDR_IMM),
		new Instruction(Instruction.DEX, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CPY, Instruction.ADDR_ABS),
		new Instruction(Instruction.CMP, Instruction.ADDR_ABS),
		new Instruction(Instruction.DEC, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BNE, Instruction.ADDR_REL),
		new Instruction(Instruction.CMP, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CMP, Instruction.ADDR_ZPX),
		new Instruction(Instruction.DEC, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CLD, Instruction.ADDR_IMP),
		new Instruction(Instruction.CMP, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CMP, Instruction.ADDR_ABX),
		new Instruction(Instruction.DEC, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CPX, Instruction.ADDR_IMM),
		new Instruction(Instruction.SBC, Instruction.ADDR_INDX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CPX, Instruction.ADDR_ZP),
		new Instruction(Instruction.SBC, Instruction.ADDR_ZP),
		new Instruction(Instruction.INC, Instruction.ADDR_ZP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.INX, Instruction.ADDR_IMP),
		new Instruction(Instruction.SBC, Instruction.ADDR_IMM),
		new Instruction(Instruction.NOP, Instruction.ADDR_IMP),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.CPX, Instruction.ADDR_ABS),
		new Instruction(Instruction.SBC, Instruction.ADDR_ABS),
		new Instruction(Instruction.INC, Instruction.ADDR_ABS),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.BEQ, Instruction.ADDR_REL),
		new Instruction(Instruction.SBC, Instruction.ADDR_INDY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.SBC, Instruction.ADDR_ZPX),
		new Instruction(Instruction.INC, Instruction.ADDR_ZPX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.SED, Instruction.ADDR_IMP),
		new Instruction(Instruction.SBC, Instruction.ADDR_ABY),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL),
		new Instruction(Instruction.SBC, Instruction.ADDR_ABX),
		new Instruction(Instruction.INC, Instruction.ADDR_ABX),
		new Instruction(Instruction.ILL, Instruction.ADDR_ILL)
	};
	
	public class ExecutionStats {
		private Long executionCount;
		private Instruction[] executionFreq = new Instruction[256];
		
		ExecutionStats(Long execCount, Instruction[] execFreq) {
			executionCount = execCount;
			for(int i = 0; i < 256; ++i) {
				executionFreq[i] = execFreq[i];
			}
		}
		
		public void sortByFrequency() {
			InstructionComparator comparator = new InstructionComparator();
			
			Arrays.sort(executionFreq, comparator);
		}
		
		public Instruction getInstruction(int index) {
			return executionFreq[index];
		}
		
		long getExecutionCount() {
			return executionCount;
		}
	}
	
	public ExecutionStats retrieveCurrentExecutionStats() {
		return new ExecutionStats(instr_count, instr_freq);
	}
	
	private static final int[] InstructionCycles = {
  /*  	0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F */
  /*0*/	7, 6, 0, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
  /*1*/	2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
  /*2*/	6, 6, 0, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
  /*3*/	2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
  /*4*/	6, 6, 0, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
  /*5*/	2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
  /*6*/	6, 6, 0, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
  /*7*/	2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
  /*8*/	2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
  /*9*/	3, 6, 0, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
  /*A*/	2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
  /*B*/	2, 5, 0, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
  /*C*/	2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
  /*D*/	2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
  /*E*/	2, 6, 3, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
  /*F*/	2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7
	};
	
	public long instructionCount() {
		return instr_count;
	}

	private int instrCycles;
	//private int prevInstructionEnd;
	private int prevInstruction;
	public int InstructionParse() throws CPUBreakException {
		
		//DumpInstruction();
		instruction[0] = memory.ReadMemory(PC)&0xFF;
		instr_count++;
		instr_freq[instruction[0]].exec_count++;
		instrCycles = InstructionCycles[instruction[0]];
		//DebugInterface();
		PC++;
		
		pageCrossed = false;
		
		switch (instruction[0]) {
		// ADC
		case 0x69:
			TwoByte();
			Immediate();
			ADC();
			break;
		case 0x65:
			TwoByte();
			ZeroPage();
			ADC();
			break;
		case 0x75:
			TwoByte();
			ZeroPageX();
			ADC();
			break;
		case 0x6D:
			ThreeByte();
			Absolute();
			ADC();
			break;
		case 0x7D:
			ThreeByte();
			AbsoluteX();
			ADC();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0x79:
			ThreeByte();
			AbsoluteY();
			ADC();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0x61:
			TwoByte();
			IndirectX();
			ADC();
			break;
		case 0x71:
			TwoByte();
			IndirectY();
			ADC();
			if(pageCrossed == true) ++instrCycles;
			break;

		// AND
		case 0x29:
			TwoByte();
			Immediate();
			AND();
			break;
		case 0x25:
			TwoByte();
			ZeroPage();
			AND();
			break;
		case 0x35:
			TwoByte();
			ZeroPageX();
			AND();
			break;
		case 0x2D:
			ThreeByte();
			Absolute();
			AND();
			break;
		case 0x3D:
			ThreeByte();
			AbsoluteX();
			AND();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0x39:
			ThreeByte();
			AbsoluteY();
			AND();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0x21:
			TwoByte();
			IndirectX();
			AND();
			break;
		case 0x31:
			TwoByte();
			IndirectY();
			AND();
			if(pageCrossed == true) ++instrCycles;
			break;

		// ASL
		case 0x0A:
			OneByte();
			Accumulator();
			ASL();
			A = operand;
			break;
		case 0x06:
			TwoByte();
			ZeroPage();
			ASL();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x16:
			TwoByte();
			ZeroPageX();
			ASL();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x0E:
			ThreeByte();
			Absolute();
			ASL();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x1E:
			ThreeByte();
			AbsoluteX();
			ASL();
			memory.WriteMemory(operandAddr, operand);
			break;

		// BCC
		case 0x90:
			TwoByte();
			Relative();
			BCC();
			break;

		// BCS
		case 0xB0:
			TwoByte();
			Relative();
			BCS();
			break;

		// BEQ
		case 0xF0:
			TwoByte();
			Relative();
			BEQ();
			break;

		// BIT
		case 0x24:
			TwoByte();
			ZeroPage();
			BIT();
			break;
		case 0x2C:
			ThreeByte();
			Absolute();
			BIT();
			break;

		// BMI
		case 0x30:
			TwoByte();
			Relative();
			BMI();
			break;

		// BNE
		case 0xD0:
			TwoByte();
			Relative();
			BNE();
			break;

		// BPL
		case 0x10:
			TwoByte();
			Relative();
			BPL();
			break;

		// BRK
		case 0x00:
			OneByte();
			Implied();
			BRK();
			break;

		// BVC
		case 0x50:
			TwoByte();
			Relative();
			BVC();
			break;

		// BVS
		case 0x70:
			TwoByte();
			Relative();
			BVS();
			break;

		// CLC
		case 0x18:
			OneByte();
			Implied();
			CLC();
			break;

		// CLD
		case 0xD8:
			OneByte();
			Implied();
			CLD();
			break;

		// CLI
		case 0x58:
			OneByte();
			Implied();
			CLI();
			break;

		// CLV
		case 0xB8:
			OneByte();
			Implied();
			CLV();
			break;

		// CMP
		case 0xC9:
			TwoByte();
			Immediate();
			CMP();
			break;
		case 0xC5:
			TwoByte();
			ZeroPage();
			CMP();
			break;
		case 0xD5:
			TwoByte();
			ZeroPageX();
			CMP();
			break;
		case 0xCD:
			ThreeByte();
			Absolute();
			CMP();
			break;
		case 0xDD:
			ThreeByte();
			AbsoluteX();
			CMP();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xD9:
			ThreeByte();
			AbsoluteY();
			CMP();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xC1:
			TwoByte();
			IndirectX();
			CMP();
			break;
		case 0xD1:
			TwoByte();
			IndirectY();
			CMP();
			if(pageCrossed == true) ++instrCycles;
			break;

		// CPX
		case 0xE0:
			TwoByte();
			Immediate();
			CPX();
			break;
		case 0xE4:
			TwoByte();
			ZeroPage();
			CPX();
			break;
		case 0xEC:
			ThreeByte();
			Absolute();
			CPX();
			break;

		// CPY
		case 0xC0:
			TwoByte();
			Immediate();
			CPY();
			break;
		case 0xC4:
			TwoByte();
			ZeroPage();
			CPY();
			break;
		case 0xCC:
			ThreeByte();
			Absolute();
			CPY();
			break;

		// DEC
		case 0xC6:
			TwoByte();
			ZeroPage();
			DEC();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0xD6:
			TwoByte();
			ZeroPageX();
			DEC();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0xCE:
			ThreeByte();
			Absolute();
			DEC();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0xDE:
			ThreeByte();
			AbsoluteX();
			DEC();
			memory.WriteMemory(operandAddr, operand);
			break;

		// DEX
		case 0xCA:
			OneByte();
			Implied();
			DEX();
			break;

		// DEY
		case 0x88:
			OneByte();
			Implied();
			DEY();
			break;

		// EOR
		case 0x49:
			TwoByte();
			Immediate();
			EOR();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x45:
			TwoByte();
			ZeroPage();
			EOR();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x55:
			TwoByte();
			ZeroPageX();
			EOR();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x4D:
			ThreeByte();
			Absolute();
			EOR();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x5D:
			ThreeByte();
			AbsoluteX();
			EOR();
			if(pageCrossed == true) ++instrCycles;
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x59:
			ThreeByte();
			AbsoluteY();
			EOR();
			if(pageCrossed == true) ++instrCycles;
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x41:
			TwoByte();
			IndirectX();
			EOR();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x51:
			TwoByte();
			IndirectY();
			EOR();
			if(pageCrossed == true) ++instrCycles;
			//memory.WriteMemory(operandAddr, operand);
			break;

		// INC
		case 0xE6:
			TwoByte();
			ZeroPage();
			INC();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0xF6:
			TwoByte();
			ZeroPageX();
			INC();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0xEE:
			ThreeByte();
			Absolute();
			INC();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0xFE:
			ThreeByte();
			AbsoluteX();
			INC();
			memory.WriteMemory(operandAddr, operand);
			break;

		// INX
		case 0xE8:
			OneByte();
			Implied();
			INX();
			break;

		// INY
		case 0xC8:
			OneByte();
			Implied();
			INY();
			break;

		// JMP
		case 0x4C:
			ThreeByte();
			Absolute();
			JMP();
			break;
		case 0x6C:
			ThreeByte();
			Indirect();
			JMP();
			break;

		// JSR
		case 0x20:
			ThreeByte();
			Absolute();
			JSR();
			break;

		// LDA
		case 0xA9:
			TwoByte();
			Immediate();
			LDA();
			break;
		case 0xA5:
			TwoByte();
			ZeroPage();
			LDA();
			break;
		case 0xB5:
			TwoByte();
			ZeroPageX();
			LDA();
			break;
		case 0xAD:
			ThreeByte();
			Absolute();
			LDA();
			break;
		case 0xBD:
			ThreeByte();
			AbsoluteX();
			LDA();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xB9:
			ThreeByte();
			AbsoluteY();
			LDA();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xA1:
			TwoByte();
			IndirectX();
			LDA();
			break;
		case 0xB1:
			TwoByte();
			IndirectY();
			LDA();
			if(pageCrossed == true) ++instrCycles;
			break;

		// LDX
		case 0xA2:
			TwoByte();
			Immediate();
			LDX();
			break;
		case 0xA6:
			TwoByte();
			ZeroPage();
			LDX();
			break;
		case 0xB6:
			TwoByte();
			ZeroPageY();
			LDX();
			break;
		case 0xAE:
			ThreeByte();
			Absolute();
			LDX();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xBE:
			ThreeByte();
			AbsoluteY();
			LDX();
			break;

		// LDY
		case 0xA0:
			TwoByte();
			Immediate();
			LDY();
			break;
		case 0xA4:
			TwoByte();
			ZeroPage();
			LDY();
			break;
		case 0xB4:
			TwoByte();
			ZeroPageX();
			LDY();
			break;
		case 0xAC:
			ThreeByte();
			Absolute();
			LDY();
			break;
		case 0xBC:
			ThreeByte();
			AbsoluteX();
			LDY();
			break;

		// LSR
		case 0x4A:
			OneByte();
			Accumulator();
			LSR();
			A = operand;
			break;
		case 0x46:
			TwoByte();
			ZeroPage();
			LSR();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x56:
			TwoByte();
			ZeroPageX();
			LSR();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x4E:
			ThreeByte();
			Absolute();
			LSR();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x5E:
			ThreeByte();
			AbsoluteX();
			LSR();
			memory.WriteMemory(operandAddr, operand);
			break;

		// NOP
		case 0xEA:
			OneByte();
			Implied();
			NOP();
			break;

		// ORA
		case 0x09:
			TwoByte();
			Immediate();
			ORA();
		 	//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x05:
			TwoByte();
			ZeroPage();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x15:
			TwoByte();
			ZeroPageX();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x0D:
			ThreeByte();
			Absolute();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x1D:
			ThreeByte();
			AbsoluteX();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0x19:
			ThreeByte();
			AbsoluteY();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0x01:
			TwoByte();
			IndirectX();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			break;
		case 0x11:
			TwoByte();
			IndirectY();
			ORA();
			//memory.WriteMemory(operandAddr, operand);
			if(pageCrossed == true) ++instrCycles;
			break;

		// PHA
		case 0x48:
			OneByte();
			Implied();
			PHA();
			break;

		// PHP
		case 0x08:
			OneByte();
			Implied();
			PHP();
			break;

		// PLA
		case 0x68:
			OneByte();
			Implied();
			PLA();
			break;

		// PLP
		case 0x28:
			OneByte();
			Implied();
			PLP();
			break;

		// ROL
		case 0x2A:
			OneByte();
			Accumulator();
			ROL();
			A = operand;
			break;
		case 0x26:
			TwoByte();
			ZeroPage();
			ROL();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x36:
			TwoByte();
			ZeroPageX();
			ROL();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x2E:
			ThreeByte();
			Absolute();
			ROL();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x3E:
			ThreeByte();
			AbsoluteX();
			ROL();
			memory.WriteMemory(operandAddr, operand);
			break;

		// ROR
		case 0x6A:
			OneByte();
			Accumulator();
			ROR();
			A = operand;
			break;
		case 0x66:
			TwoByte();
			ZeroPage();
			ROR();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x76:
			TwoByte();
			ZeroPageX();
			ROR();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x6E:
			ThreeByte();
			Absolute();
			ROR();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x7E:
			ThreeByte();
			AbsoluteX();
			ROR();
			memory.WriteMemory(operandAddr, operand);
			break;

		// RTI
		case 0x40:
			OneByte();
			Implied();
			RTI();
			break;

		// RTS
		case 0x60:
			OneByte();
			Implied();
			RTS();
			break;

		// SBC
		case 0xE9:
			TwoByte();
			Immediate();
			SBC();
			break;
		case 0xE5:
			TwoByte();
			ZeroPage();
			SBC();
			break;
		case 0xF5:
			TwoByte();
			ZeroPageX();
			SBC();
			break;
		case 0xED:
			ThreeByte();
			Absolute();
			SBC();
			break;
		case 0xFD:
			ThreeByte();
			AbsoluteX();
			SBC();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xF9:
			ThreeByte();
			AbsoluteY();
			SBC();
			if(pageCrossed == true) ++instrCycles;
			break;
		case 0xE1:
			TwoByte();
			IndirectX();
			SBC();
			break;
		case 0xF1:
			TwoByte();
			IndirectY();
			SBC();
			if(pageCrossed == true) ++instrCycles;
			break;

		// SEC
		case 0x38:
			OneByte();
			Implied();
			SEC();
			break;

		// SED
		case 0xF8:
			OneByte();
			Implied();
			SED();
			break;

		// SEI
		case 0x78:
			OneByte();
			Implied();
			SEI();
			break;

		// STA
		case 0x85:
			TwoByte();
			ZeroPage();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x95:
			TwoByte();
			ZeroPageX();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x8D:
			ThreeByte();
			Absolute();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x9D:
			ThreeByte();
			AbsoluteX();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x99:
			ThreeByte();
			AbsoluteY();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x81:
			TwoByte();
			IndirectX();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x91:
			TwoByte();
			IndirectY();
			STA();
			memory.WriteMemory(operandAddr, operand);
			break;

		// STX
		case 0x86:
			TwoByte();
			ZeroPage();
			STX();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x96:
			TwoByte();
			ZeroPageY();
			STX();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x8E:
			ThreeByte();
			Absolute();
			STX();
			memory.WriteMemory(operandAddr, operand);
			break;

		// STY
		case 0x84:
			TwoByte();
			ZeroPage();
			STY();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x94:
			TwoByte();
			ZeroPageX();
			STY();
			memory.WriteMemory(operandAddr, operand);
			break;
		case 0x8C:
			ThreeByte();
			Absolute();
			STY();
			memory.WriteMemory(operandAddr, operand);
			break;

		// TAX
		case 0xAA:
			OneByte();
			Implied();
			TAX();
			break;

		// TAY
		case 0xA8:
			OneByte();
			Implied();
			TAY();
			break;

		// TSX
		case 0xBA:
			OneByte();
			Implied();
			TSX();
			break;

		// TXA
		case 0x8A:
			OneByte();
			Implied();
			TXA();
			break;

		// TXS
		case 0x9A:
			OneByte();
			Implied();
			TXS();
			break;

		// TYA
		case 0x98:
			OneByte();
			Implied();
			TYA();
			break;

			default:
				System.out.println("!Unknown opcode!");
				throw new CPUBreakException(this);
				//DebugInterface();
				//break;
		}
		
		prevInstruction = instruction[0];
		//prevInstructionEnd = PC;
		return instrCycles;
	}
	
	public void DumpInstruction()
	{
		if(dump != null)
		{
			try {
				dump.write(PC&0xFF);
				dump.write(PC>>8);
				dump.write(memory.ReadMemory(PC));
				dump.write(A);
				dump.write(X);
				dump.write(Y);
				dump.write(P);
				dump.write(SP&0xFF);
				dump.write(0xFF);
				dump.write(0xFF);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	private final int NUM_CPU_IDLE_CYCLES = 2;
	int remainingInstrCycles = 0;
	public int InstructionStep() throws CPUBreakException {
		int cycles;
		if(memory.haltCPU())
		{
			cycles = NUM_CPU_IDLE_CYCLES;
		}
		else
		{
			cycles = InstructionParse();
		}
		
		
		/*if(remainingInstrCycles == 0)
		{
			remainingInstrCycles = InstructionParse();
		}
		else
			remainingInstrCycles--;
		
		memory.clock(1);*/
		
		memory.clock(cycles);
		
	/*	switch(memory.intFlags&0x7) {
		case (C64System.NMI_CIA2):
		case (C64System.NMI_CIA2|C64System.IRQ_CIA1):
		case (C64System.NMI_CIA2|C64System.IRQ_CIA1|C64System.IRQ_VICII):
			
		}*/
		if(memory.IRQ() && (GetStatusBit(INT_FLAG)==0)) {
			memory.WriteMemory(SP, PC>>8);
			SP = (SP - 1) & 0x1FF;
			memory.WriteMemory(SP, PC&0xFF);
			SP = (SP - 1) & 0x1FF;
			
			// push status to stack
			memory.WriteMemory(SP, P);
			SP = (SP - 1) & 0x1FF;
			
			SetStatusBit(INT_FLAG, 1);
			SetStatusBit(BRK_FLAG, 0);
			
			l = memory.ReadMemory(0xFFFE);
			h = memory.ReadMemory(0xFFFF);
			
			PC = (l | (h<<8));
		}
		
		if(memory.NMI()) {
			memory.WriteMemory(SP, PC>>8);
			SP = (SP - 1) & 0x1FF;
			memory.WriteMemory(SP, PC&0xFF);
			SP = (SP - 1) & 0x1FF;
			
			// push status to stack
			memory.WriteMemory(SP, P);
			SP = (SP - 1) & 0x1FF;
			
			l = memory.ReadMemory(0xFFFA);
			h = memory.ReadMemory(0xFFFB);
			
			PC = (l | (h<<8));
		}
		return PC;
	}
	
	public void SetPC(int address) {
		PC = address;
	}
 
	public void setA(int regA) {
		this.A = regA;
	}
	public long CallSub(int address, int regA) throws CPUBreakException {
		A = regA;
		X = 0;
		Y = 0;
		P = 0;
		SP = 0x1FF;
		
		operandAddr = address;
		PC = 0x0001;
		JSR();
		
		long nInstructions = 0;

		while (InstructionStep() != 0x0001) ++nInstructions;
		
		return nInstructions;
	}
}
