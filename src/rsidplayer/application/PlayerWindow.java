package rsidplayer.application;

import java.awt.EventQueue;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.ListModel;
import javax.swing.JMenuItem;

import java.io.File;
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

import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.awt.Font;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;

import rsidplayer.player.RSIDPlayer;
import rsidplayer.player.SIDFile;

import javax.swing.border.EtchedBorder;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;

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
	private MemoryViewer memoryViewer;

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
						window.performSongLoad(appArgs[0], true);
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
	
	File previousOpenDirectory = null;

	void performOpen() {
		JFileChooser jfc = new JFileChooser(previousOpenDirectory);
		FileFilter filt = new SIDFileFilter();

		jfc.setFileFilter(filt);
		jfc.setMultiSelectionEnabled(true);
		int returnVal = jfc.showOpenDialog(frmSidPlayer);
		previousOpenDirectory = jfc.getCurrentDirectory();
		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] files = jfc.getSelectedFiles();
			if(files.length == 1) {
				performSongLoad(files[0].getAbsolutePath(), true);
			}
			else
			{
				performSongLoad(files);
			}
			performSongSelection(0);
		} else
			return;
	}
	
	void performOpenDirectory() {
		JFileChooser jfc = new JFileChooser(previousOpenDirectory);

		jfc.setAcceptAllFileFilterUsed(true);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		int returnVal = jfc.showOpenDialog(frmSidPlayer);
		previousOpenDirectory = jfc.getCurrentDirectory();
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File directory = jfc.getSelectedFile();
			System.out.println(directory.getAbsolutePath());
			performSongLoad(directory.listFiles(new SIDFileFilter()));
			performSongSelection(0);
		} else
			return;
	}

	int currentSubtune;
	boolean isPlaying;
	int playTimeSeconds;
	Thread playTimeThread;
	
	private JList<String> playListDisplay;
	private PlayList playList;
	
	private JTable statsTable;
	private StatsTableModel statsTableModel;
	private JLabel labelTime;
	private int playTime;

	void setupPlaylist() {
		//playList[index] = song;

	}
	
	SIDFile getPlaylistSIDFile(int index) {
		return playList.get(index).getSIDFile();
	}

	void performSongLoad(String fileName, boolean clearPlaylist) {
		SIDFile sidFile = null;
		try {
			sidFile = SIDFile.loadFromFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			setStatus(e.getMessage());
		}
		
		if(sidFile == null)
			return;
		
		if(clearPlaylist) playList.clear();//clearPlaylist(1);
		playList.add(new SIDSong(sidFile));
		//setupPlaylist();

		setStatus(sidFile.fileName + " loaded.");
		sliderSubtune.setEnabled(true);
	}
	
	void performSongLoad(File[] files) {
		
		playList.clear();//clearPlaylist(1);
		if(files == null) {
			setStatus("No files loaded.");
			return;
		}
		
		for(File f : files) {
			SIDFile sidFile = null;
			if(f.isDirectory()) continue;
			try {
				sidFile = SIDFile.loadFromFile(f.getAbsolutePath());
			} catch (IOException e) {
				setStatus(e.getMessage());
			}
			
			if(sidFile == null)
				continue;
			
			playList.add(new SIDSong(sidFile));
		}
		
		setStatus("Loaded multiple tunes!");
		sliderSubtune.setEnabled(true);
	}

	void performSongSelection(int playIndex) {
		if(playList.size() < 1) {
			setStatus("No songs are loaded to select!");
			return;
		}
		playList.setPlayIndex(playIndex);
		SIDSong sidSong = playList.get(playIndex);
		SIDFile sidFile = sidSong.getSIDFile();//getPlaylist(playIndex);
		if(sidFile == null)
			return;
		
		currentSubtune = sidFile.startSong;
		playTime = sidSong.getPlayTime();
		lblName.setText(sidFile.title);
		lblComposer.setText(sidFile.author);
		lblCopyright.setText(sidFile.released);
		
		dimensionSubtuneSlider(sidFile.songs, sidFile.startSong);
		
		lblLoad.setText("$"+Integer.toHexString(sidFile.loadAddress));
		lblInit.setText("$"+Integer.toHexString(sidFile.initAddress));
		lblPlay.setText("$"+Integer.toHexString(sidFile.playAddress));
		
		boolean wasPlaying = false;
		if(isPlaying) {
			performStop();
			wasPlaying = true;
		}
		
		try {
			player.setSID(sidFile);
		} catch (IOException e) {
			setStatus("Error: " + e.getMessage());
		}
		
		if(wasPlaying)
			performPlay();
		
		playListDisplay.setSelectedIndex(playIndex);
	}
	
	void goNextSong()
	{
		int thisPlayIndex = playList.getPlayIndex();
		
		if((thisPlayIndex+1) < playList.size())
		{
			++thisPlayIndex;
			playList.setPlayIndex(thisPlayIndex);
			performSongSelection(thisPlayIndex);
		}	
		else
			performStop();
	}
	
	void goPrevSong()
	{
		int thisPlayIndex = playList.getPlayIndex();
		
		if((thisPlayIndex-1) >= 0)
		{
			--thisPlayIndex;
			playList.setPlayIndex(thisPlayIndex);
			performSongSelection(thisPlayIndex);
		}	
		else
			performStop();
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
						
						if(playTimeSeconds >= playTime) {
							goNextSong();
							break;
						}
						
						
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
			statsTableModel.updateStats(player.getCpu());
		} catch (Exception e1) {
			setStatus(e1.getMessage());
			isPlaying = false;
		}
	}

	void performStop() {
		player.stop();
		
		isPlaying = false;
		if(!Thread.currentThread().equals(playTimeThread))
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
	
	/**
	 * Create the application.
	 */
	public PlayerWindow() {
		initialize();
		player = new RSIDPlayer();
		memoryViewer = new MemoryViewer();
		setupPlaylist();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSidPlayer = new JFrame();
		frmSidPlayer.setResizable(false);
		frmSidPlayer.setTitle("SID Player");
		frmSidPlayer.setBounds(100, 100, 310, 440);
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
		
		JMenuItem mntmOpenDir = new JMenuItem("Open Directory");
		mntmOpenDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performOpenDirectory();
			}
		});
		mnFile.add(mntmOpenDir);

		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);

		JMenuItem mntmVisualization = new JMenuItem("Audio Visualization");
		mntmVisualization.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				player.showVisuals();
			}
		});
		mnView.add(mntmVisualization);
		
		JMenuItem mntmMemoryViewer = new JMenuItem("Memory Viewer");
		mntmMemoryViewer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				memoryViewer.setVisible(true);
			}
		});
		mnView.add(mntmMemoryViewer);
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
		playList = new PlayList();
		
		JPanel panel_4 = new JPanel();
		panel_4.setLayout(null);
		JScrollPane playListScroll = new JScrollPane();
		playListScroll.setBounds(10, 11, 258, 130);
		//tabbedPane.addTab("Playlist", null, playListScroll, null);
		panel_4.add(playListScroll);
		
		playListDisplay = new JList<String>();
		//playListScroll.setViewportView(playListDisplay);
		playListDisplay.setBackground(Color.WHITE);
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
		playListDisplay.setModel(new ListModel<String>() {

			@Override
			public void addListDataListener(ListDataListener arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public String getElementAt(int index) {
				if (playList.size() > index)
					return /*(*/playList.get(index).getSIDFile().fileName;//==null)?(""):(playList[arg0].fileName);
				else
					return "";
			}

			@Override
			public int getSize() {
				// TODO Auto-generated method stub
				return playList.size();
			}

			@Override
			public void removeListDataListener(ListDataListener arg0) {
				// TODO Auto-generated method stub
			}

		});
		playListDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		playListScroll.setVisible(true);
		playListScroll.setViewportView(playListDisplay);
		tabbedPane.addTab("Playlist", null, panel_4, null);
		
		
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
		panel_1.setBounds(10, 255, 283, 125);
		frmSidPlayer.getContentPane().add(panel_1);

		btnPlay = new JButton("Play");
		btnPlay.setBounds(10, 29, 64, 23);
		btnPlay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performPlay();
			}
		});
		panel_1.setLayout(null);
		panel_1.add(btnPlay);

		btnStop = new JButton("Stop");
		btnStop.setEnabled(false);
		btnStop.setBounds(84, 29, 64, 23);
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performStop();
			}
		});
		panel_1.add(btnStop);

		sliderSubtune = new JSlider();
		sliderSubtune.setBounds(10, 77, 261, 37);
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
		lblSubtuneSlider.setBounds(20, 63, 261, 14);
		lblSubtuneSlider.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblSubtuneSlider.setHorizontalAlignment(SwingConstants.CENTER);
		panel_1.add(lblSubtuneSlider);
		
		labelTime = new JLabel("");
		labelTime.setHorizontalAlignment(SwingConstants.CENTER);
		labelTime.setBounds(84, 11, 113, 14);
		panel_1.add(labelTime);
		
		JButton btnPrev = new JButton("<-");
		btnPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				goPrevSong();
			}
		});
		btnPrev.setBounds(158, 29, 51, 23);
		panel_1.add(btnPrev);
		
		JButton btnNext = new JButton("->");
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goNextSong();
			}
		});
		btnNext.setBounds(219, 29, 52, 23);
		panel_1.add(btnNext);

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
