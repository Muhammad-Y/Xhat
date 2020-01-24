package server;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * Grafiskt användargränssnitt för servern.
 */
public class ServerController {
	private ServerConnection server;
	private JFrame guiFrame;
	private JButton btnClose;
	private DefaultListModel<String> logModel = new DefaultListModel<>();
	private JList<String> jlLogList = new JList<>();
	private JScrollPane spLogList = new JScrollPane(jlLogList);


	/**
	 * Skapar ett grafiskt användargränssnitt för servern.
	 * @param server En referens till ett Server-objekt.
	 */
	public ServerController (ServerConnection server) {
		this.server = server;
		server.addListener(new Logger());
		server.start();

		guiFrame = new JFrame();
		
		jlLogList.setModel(logModel);
		jlLogList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		
		//Norra panelen
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setPreferredSize(new Dimension(380, 400));
		spLogList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		northPanel.add(spLogList);
		northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		//Södra panelen
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setPreferredSize(new Dimension(100, 80));
		southPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		btnClose = new JButton( "Close Server");
		southPanel.add(btnClose);

		// make sure the program exits when the frame closes
		guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		guiFrame.setTitle("Server");
		guiFrame.setSize(400, 500);

		// This will center the JFrame in the middle of the screen
		guiFrame.setLocationRelativeTo(null);

		guiFrame.add(northPanel, BorderLayout.NORTH);
		guiFrame.add(southPanel, BorderLayout.SOUTH);

		Action action = new Action();
		btnClose.addActionListener(action);

		// make sure the JFrame is visible
		guiFrame.setVisible(true);
	}
	
	private class Logger implements LogListener {
		public void logInfo(String info) {
			addLogRow("[INFO] " + info);
			ServerLogger.logInfo(info);
		}

		public void logCommunication(String com) {
			addLogRow("[COM] " + com);
			ServerLogger.logCommunication(com);
		}

		public void logError(String error) {
			addLogRow("[ERROR] " + error);
			ServerLogger.logError(error);
		}
		
		private void addLogRow(String row) {
			logModel.addElement(row);
			
			int lastIndex = jlLogList.getModel().getSize() - 1;
			if (lastIndex >= 0) {
				jlLogList.ensureIndexIsVisible(lastIndex);
			}
		}
	}

	private class Action implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == btnClose) {
				server.shutdownServer();
			}
		}
	}

}
