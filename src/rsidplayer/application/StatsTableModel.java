package rsidplayer.application;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import rsidplayer.c64.CPU6510;
import rsidplayer.c64.CPU6510.ExecutionStats;

class StatsTableModel implements TableModel {
	CPU6510 cpu;
	ExecutionStats stats;
	
	StatsTableModel() {
		stats = null;
	}
	
	public void updateStats(CPU6510 cpu) {
		this.cpu = cpu;
		stats = cpu.retrieveCurrentExecutionStats();
	}

	@Override
	public void addTableModelListener(TableModelListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class<?> getColumnClass(int index) {
		switch(index) {
		case 0:
			return String.class;
		case 1:
			return String.class;
		case 2:
			return Long.class;
		case 3:
			return String.class;
		default:
			return String.class;
		}
	}

	@Override
	public int getColumnCount() {
	/*	if(stats == null)
			return 0;*/
		
		return 4;
	}

	@Override
	public String getColumnName(int index) {
		switch(index) {
		case 0:
			return "Name";
		case 1:
			return "Opcode";
		case 2:
			return "Frequency";
		case 3:
			return "Rel. Freq.";
		default:
			return "";
		}
	}

	@Override
	public int getRowCount() {
	/*	if(stats == null)
			return 0;*/
		
		return 256;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if(stats == null)
			return " ";
		
		stats.sortByFrequency();
		
		CPU6510.Instruction instr = stats.getInstruction(row);
		
		switch(col) {
		case 0:
			return instr.getFullName();
		case 1:
			return "$" + Integer.toHexString(instr.opcode);
		case 2:
			return instr.getExecCount();
		case 3:
			return Float.toString(((float)instr.getExecCount()/(float)cpu.instructionCount())*100.0f) + "%";
		}
		return " ";
	}

	@Override
	public boolean isCellEditable(int arg0, int arg1) {
		return false;
	}

	@Override
	public void removeTableModelListener(TableModelListener arg0) {
	}

	@Override
	public void setValueAt(Object arg0, int arg1, int arg2) {
	}
}