package rsidplayer;

import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import javax.swing.JMenuItem;
import java.io.IOException;

import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.awt.Font;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import rsidplayer.CPU6510.ExecutionStats;
import rsidplayer.CPU6510.Instruction;
import javax.swing.border.LineBorder;
import java.awt.Color;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

public class PlayerWindow {

	private JFrame frmSidPlayer;
	private RSIDPlayer player;
	private JLabel lblName;
	private JLabel lblComposer;
	private JLabel lblCopyright;
	private JLabel lblLoad;
	private JLabel lblInit;
	private JLabel lblPlay;
	private JButton btnPlay;
	private JButton btnStop;
	private JLabel lblStatus;
	private JSlider sliderSubtune;

	private static String[] appArgs;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		appArgs = args;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PlayerWindow window = new PlayerWindow();
					window.frmSidPlayer.setVisible(true);

					if (appArgs.length > 0) {
						window.performSongLoad(appArgs[0]);
						window.performSongSelection(0);
						window.performPlay();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	void setStatus(String status) {
		lblStatus.setText(status);
	}

	void performOpen() {
		JFileChooser jfc = new JFileChooser();
		ExampleFileFilter filt = new ExampleFileFilter();

		filt.addExtension("sid");
		filt.setDescription("SIDPlay music files");

		jfc.setFileFilter(filt);

		int returnVal = jfc.showOpenDialog(frmSidPlayer);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			performSongLoad(jfc.getSelectedFile().getAbsolutePath());
			performSongSelection(0);
		} else
			return;
	}

	int currentSubtune;
	boolean isPlaying;
	int playTimeSeconds;
	Thread playTimeThread;
	
	
	private JList<String> playListDisplay;
	private SIDFile[] playList;
	
	private JTable statsTable;
	private StatsTableModel statsTableModel;
	private JLabel labelTime;

	void clearPlaylist(int size) {
		playList = new SIDFile[size];
	}

	void setPlaylist(int index, SIDFile song) {
		playList[index] = song;
		playListDisplay.setModel(new ListModel<String>() {

			@Override
			public void addListDataListener(ListDataListener arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public String getElementAt(int arg0) {
				if (playList.length > arg0)
					return (playList[arg0].fileName==null)?(""):(playList[arg0].fileName);
				else
					return "";
			}

			@Override
			public int getSize() {
				// TODO Auto-generated method stub
				return playList.length;
			}

			@Override
			public void removeListDataListener(ListDataListener arg0) {
				// TODO Auto-generated method stub
			}

		});
	}

	SIDFile getPlaylist(int index) {
		return playList[index];
	}

	void performSongLoad(String fileName) {
		SIDFile sidFile = null;
		try {
			sidFile = SIDFile.loadFromFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			setStatus(e.getMessage());
		}
		
		if(sidFile == null)
			return;
		
		clearPlaylist(1);
		setPlaylist(0, sidFile);
		
		dimensionSubtuneSlider(sidFile.songs, sidFile.startSong);

		setStatus(sidFile.fileName + " loaded.");
		sliderSubtune.setEnabled(true);
		
	}

	void performSongSelection(int playIndex) {
		SIDFile sidFile = getPlaylist(playIndex);
		if(sidFile == null)
			return;
		
		//player.sidFile = sidFile;
		try {
			player.setSID(sidFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			setStatus("Error: " + e.getMessage());
		}
		
		currentSubtune = sidFile.startSong;
		
		lblName.setText(sidFile.title);
		lblComposer.setText(sidFile.author);
		lblCopyright.setText(sidFile.released);
		
		lblLoad.setText("$"+Integer.toHexString(sidFile.loadAddress));
		lblInit.setText("$"+Integer.toHexString(sidFile.initAddress));
		lblPlay.setText("$"+Integer.toHexString(sidFile.playAddress));
		
		if(isPlaying) {
			performStop();
			performPlay();
		}
		
		playListDisplay.setSelectedIndex(playIndex);
	}
	
	synchronized boolean songIsPlaying() {
		return isPlaying;
	}
	
	synchronized void updatePlayTime() {
		int hours;
		int minutes;
		int seconds;
		
		hours = playTimeSeconds/3600;
		minutes = playTimeSeconds/60;
		seconds = playTimeSeconds%60;
		
		labelTime.setText(((hours>0)?(hours + ":" + ((minutes<10)?("0"):("")) + minutes):(minutes)) + ":" + ((seconds<10)?("0"):("")) + seconds);
	}
	
	void performPlay() {
		try {
			if(isPlaying)
				performStop();
			
			player.initTune(currentSubtune - 1);
			player.play();
			
			isPlaying = true;
			
			playTimeSeconds = 0;
			playTimeThread = new Thread() {
				public void run() {
					long lastTimeMillis = System.currentTimeMillis();
					while(songIsPlaying()) {
						updatePlayTime();
						
						
						while((System.currentTimeMillis()-lastTimeMillis) < 1000)
							try {
								if(!songIsPlaying())
									return;
								
								Thread.sleep(10);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						long curTimeMillis = System.currentTimeMillis();
						
						
						lastTimeMillis = curTimeMillis + (curTimeMillis-lastTimeMillis-1000);
						playTimeSeconds++;
						statsTable.repaint();
					}
				}
 			}; 
 		
 			playTimeThread.start();
 			
 			btnStop.setEnabled(true);
			
			setStatus("Playing " + player.sidFile.fileName + ", Subtune " + currentSubtune + ".");
			statsTableModel.updateStats(player.cpu);
		} catch (Exception e1) {
			setStatus(e1.getMessage());
			isPlaying = false;
		}
	}

	void performStop() {
		player.stop();
		
		isPlaying = false;
		try {
			playTimeThread.join();
		} catch (InterruptedException e) {
			setStatus(e.getMessage());
		}
		
		btnStop.setEnabled(false);
		setStatus("Stopped.");
		
		playTimeSeconds = 0;
		labelTime.setText("");
	}

	void dimensionSubtuneSlider(int numberOfTunes, int startTune) {
		sliderSubtune.setMaximum(numberOfTunes);
		sliderSubtune.setMinimum(1);

		sliderSubtune.setValue(startTune);
	}
	
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

	/**
	 * Create the application.
	 */
	public PlayerWindow() {
		initialize();
		player = new RSIDPlayer();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSidPlayer = new JFrame();
		frmSidPlayer.setResizable(false);
		frmSidPlayer.setTitle("SID Player");
		frmSidPlayer.setBounds(100, 100, 310, 419);
		frmSidPlayer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		frmSidPlayer.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performOpen();
			}
		});
		mnFile.add(mntmOpen);

		JMenu mnOptions = new JMenu("Options");
		menuBar.add(mnOptions);

		JMenuItem mntmShowVisualization = new JMenuItem("Show Visualization");
		mntmShowVisualization.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.showVisuals();
			}
		});
		mnOptions.add(mntmShowVisualization);
		frmSidPlayer.getContentPane().setLayout(null);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBorder(null);
		tabbedPane.setBounds(10, 11, 283, 230);
		frmSidPlayer.getContentPane().add(tabbedPane);

		JPanel panel = new JPanel();
		tabbedPane.addTab("Song InFoZ", null, panel, null);
		panel.setLayout(null);

		JLabel lblNewLabel = new JLabel("Name: ");
		lblNewLabel.setBounds(6, 6, 64, 14);
		panel.add(lblNewLabel);

		lblName = new JLabel("<?>");
		lblName.setBounds(101, 6, 167, 14);
		panel.add(lblName);

		JLabel lblNewLabel_1 = new JLabel("Composer:");
		lblNewLabel_1.setBounds(6, 26, 64, 14);
		panel.add(lblNewLabel_1);

		lblComposer = new JLabel("<?>");
		lblComposer.setBounds(101, 26, 167, 14);
		panel.add(lblComposer);

		JLabel lblNewLabel_2 = new JLabel("Copyright(?):");
		lblNewLabel_2.setBounds(6, 46, 64, 14);
		panel.add(lblNewLabel_2);

		lblCopyright = new JLabel("<?>");
		lblCopyright.setBounds(101, 46, 167, 14);
		panel.add(lblCopyright);

		JLabel lblNewLabel_3 = new JLabel("Load:");
		lblNewLabel_3.setBounds(6, 91, 64, 14);
		panel.add(lblNewLabel_3);

		lblLoad = new JLabel("");
		lblLoad.setBounds(101, 91, 167, 14);
		panel.add(lblLoad);

		JLabel lblNew4 = new JLabel("Init:");
		lblNew4.setBounds(6, 111, 64, 14);
		panel.add(lblNew4);

		lblInit = new JLabel("");
		lblInit.setBounds(101, 111, 167, 14);
		panel.add(lblInit);

		JLabel lblNew6 = new JLabel("Play:");
		lblNew6.setBounds(6, 131, 64, 14);
		panel.add(lblNew6);

		lblPlay = new JLabel("");
		lblPlay.setBounds(101, 131, 167, 14);
		panel.add(lblPlay);

		playListDisplay = new JList<String>();
		playListDisplay.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				if(!arg0.getValueIsAdjusting()) {
					JList<?> list = (JList<?>)arg0.getSource();
					//if((playList==null) || (playList.length == 0))
					int selected = list.getSelectedIndex();
					
					if(selected != -1)
						performSongSelection(list.getSelectedIndex());
				}
			}
		});
		playListDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tabbedPane.addTab("Playlist", null, playListDisplay, null);
		
		JPanel panel_3 = new JPanel();
		tabbedPane.addTab("Exec. Stats", null, panel_3, null);
		panel_3.setLayout(null);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 258, 151);
		panel_3.add(scrollPane);
		
		statsTableModel = new StatsTableModel();
		
		statsTable = new JTable();
		statsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(statsTable);
		statsTable.setModel(statsTableModel);
		
		JButton btnUpdate = new JButton("Update");
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				statsTable.repaint();
			}
		});
		btnUpdate.setBounds(93, 173, 89, 23);
		panel_3.add(btnUpdate);
		
		statsTable.repaint();
		JPanel panel_2 = new JPanel();
		panel_2.setLayout(null);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EtchedBorder(EtchedBorder.RAISED, null, null));
		panel_1.setBounds(10, 255, 283, 104);
		frmSidPlayer.getContentPane().add(panel_1);

		btnPlay = new JButton("Play");
		btnPlay.setBounds(10, 11, 64, 23);
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performPlay();
			}
		});
		panel_1.setLayout(null);
		panel_1.add(btnPlay);

		btnStop = new JButton("Stop");
		btnStop.setEnabled(false);
		btnStop.setBounds(207, 11, 64, 23);
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performStop();
			}
		});
		panel_1.add(btnStop);

		sliderSubtune = new JSlider();
		sliderSubtune.setBounds(10, 56, 261, 37);
		sliderSubtune.setEnabled(false);
		sliderSubtune.setFont(new Font("Tahoma", Font.PLAIN, 8));
		sliderSubtune.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				JSlider source = (JSlider) arg0.getSource();
				if (!source.getValueIsAdjusting())
					if (lblStatus != null) {
						try {
							currentSubtune = source.getValue();
							if (isPlaying) {
								//performStop();
								performPlay();
							}
						} catch (Exception e) {
							setStatus(e.getMessage());
						}
					}
			}
		});
		sliderSubtune.setValue(5);
		sliderSubtune.setMaximum(1);
		sliderSubtune.setMajorTickSpacing(1);
		sliderSubtune.setMinorTickSpacing(1);
		sliderSubtune.setPaintLabels(true);
		sliderSubtune.setPaintTicks(true);
		sliderSubtune.setSnapToTicks(true);
		panel_1.add(sliderSubtune);

		JLabel lblSubtuneSlider = new JLabel("Subtune Selector");
		lblSubtuneSlider.setBounds(10, 33, 261, 14);
		lblSubtuneSlider.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblSubtuneSlider.setHorizontalAlignment(SwingConstants.CENTER);
		panel_1.add(lblSubtuneSlider);
		
		labelTime = new JLabel("");
		labelTime.setHorizontalAlignment(SwingConstants.CENTER);
		labelTime.setBounds(84, 15, 113, 14);
		panel_1.add(labelTime);

		lblStatus = new JLabel("Load a song.");
		lblStatus.setBounds(6, 241, 287, 14);
		lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
		frmSidPlayer.getContentPane().add(lblStatus);

	}

	public JLabel getLblName() {
		return lblName;
	}

	public JLabel getLblComposer() {
		return lblComposer;
	}

	public JLabel getLblCopyright() {
		return lblCopyright;
	}

	public JLabel getLblLoad() {
		return lblLoad;
	}

	public JLabel getLblInit() {
		return lblInit;
	}

	public JLabel getLblPlay() {
		return lblPlay;
	}

	public JButton getBtnPlay() {
		return btnPlay;
	}

	public JButton getBtnStop() {
		return btnStop;
	}

	public JSlider getSliderSubtune() {
		return sliderSubtune;
	}
}
