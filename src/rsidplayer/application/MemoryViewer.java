package rsidplayer.application;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTabbedPane;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public class MemoryViewer extends JFrame {

	private JPanel contentPane;

	/**
	 * Create the frame.
	 */
	public MemoryViewer() {
		setTitle("Memory Viewer");
		setBounds(100, 100, 450, 343);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
		JPanel memoryPanel = new JPanel();
		tabbedPane.addTab("Memory", null, memoryPanel, null);
		memoryPanel.setLayout(new BorderLayout(0, 0));
		
		JTextArea txtrDfgdfsgsGfsDfgsd = new JTextArea();
		txtrDfgdfsgsGfsDfgsd.setEditable(false);
		txtrDfgdfsgsGfsDfgsd.setColumns(52);
		txtrDfgdfsgsGfsDfgsd.setRows(16);
		memoryPanel.add(txtrDfgdfsgsGfsDfgsd, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		memoryPanel.add(panel, BorderLayout.SOUTH);
		
		JLabel lblPage = new JLabel("Page: ");
		panel.add(lblPage);
		
		JComboBox pageComboBox = new JComboBox();
		panel.add(pageComboBox);
		
		JButton btnUpdatePage = new JButton("Update");
		panel.add(btnUpdatePage);
		
		JPanel sidPanel = new JPanel();
		tabbedPane.addTab("SID", null, sidPanel, null);
		
		JPanel ciaOnePanel = new JPanel();
		tabbedPane.addTab("CIA 1", null, ciaOnePanel, null);
		
		JPanel ciaTwoPanel = new JPanel();
		tabbedPane.addTab("CIA 2", null, ciaTwoPanel, null);
	}
}
