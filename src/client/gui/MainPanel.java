package client.gui;

import common.Encryption;
import common.Message;
import org.apache.commons.io.FileUtils;
import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * En klass som möjliggör för en användare att interagera med systemet 
 * i syftet att kunna chatta med andra användare. Gränssnittet ska erhålla
 * möjligheter för att bl.a. skriva och skicka text, visa kontaktlista och
 * logga ut användaren. 
 * @author grupp09 Xhat
 */
public class MainPanel extends JPanel {
	private MainController mainController;
	private AddContactPanel addContactPanel;
	private AddGroupChatPanel addGroupChatPanel;
	private JLabel lblUsername = new JLabel();
	private JLabel lblContactList = new JLabel("Contact list");
	private JLabel lblGroupChats = new JLabel("Group chats");
	private JLabel lblChattingWith = new JLabel("You are chatting with: ");
	private JLabel lblOtherUserStatus = new JLabel("User status: ");
	private JButton btnLogout = new JButton("Log out");
	private JButton btnSend = new JButton("Send");
	private JButton btnRemoveCon = new JButton("Remove contact");
	private JButton btnSendFile = new JButton("Send file");
	private JButton btnAddContact = new JButton("Add contact");
	private JButton btnAddGroupChat = new JButton("Add group chat");
	private JButton btnLeaveGroupChat = new JButton("Leave group");
	private Font font = new Font("SansSerif", Font.BOLD, 18);
	private boolean isGroupInFocus;
	private String encryptionKey;
	
	//behövs för fönster med scrollfunktion
	private DefaultListModel<JLabel> conversationModel = new DefaultListModel<>();
	private JList<JLabel> jlistConversation = new JList<>();
	private JList<JLabel> jlistGroupChats = new JList<>();
	private JList<JLabel> jlistContactList = new JList<>();
	private JTextArea txtMessageField = new JTextArea();	
	private JScrollPane spMessageField = new JScrollPane(txtMessageField);
	private JScrollPane spGroupChats = new JScrollPane(jlistGroupChats);
	private JScrollPane spContactList = new JScrollPane(jlistContactList);
	private JScrollPane spConversationWindow = new JScrollPane(jlistConversation);

	public MainPanel(MainController mainController, DefaultListModel<JLabel> contactsModel, DefaultListModel<JLabel> groupsModel) {
		this.mainController = mainController;
		jlistContactList.setModel(contactsModel);
		jlistGroupChats.setModel(groupsModel);
		ListListener listListener = new ListListener();
		jlistContactList.addListSelectionListener(listListener);
		jlistGroupChats.addListSelectionListener(listListener);
		this.lblUsername.setText("Logged in as: " + mainController.getUserName());
		generateChatPanel();
	}

	/**
	 * genererar utseende för det fönster där en användare
	 * kan chatta med andra användare.
	 */
	private void generateChatPanel() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setPreferredSize(new Dimension(550, 600));
		
		//Norra panelen
		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setPreferredSize(new Dimension(540, 60));
		northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //top, left, bottom, right
		lblUsername.setFont(font);
		lblUsername.setPreferredSize(new Dimension(300, 60)); 
		btnLogout.setPreferredSize(new Dimension(100, 60)); 
		northPanel.add(lblUsername, BorderLayout.WEST);
		northPanel.add(btnLogout, BorderLayout.EAST);
		
		
		//Västra panelen
		JPanel westPanel = new JPanel(new BorderLayout());
		westPanel.setPreferredSize(new Dimension(380, 400));
		JPanel otherUserInfoPanel = new JPanel(new GridLayout(2, 1));
		otherUserInfoPanel.add(lblChattingWith);
		otherUserInfoPanel.add(lblOtherUserStatus);
		otherUserInfoPanel.setPreferredSize(new Dimension(50, 30));
		
		//Gör att JList visar JLabels som text/bild
		jlistConversation.setCellRenderer(new JListConversationRenderer());
		jlistConversation.addMouseListener(new ConversationML());
		JListContactsRenderer listRenderer = new JListContactsRenderer(); 
		jlistContactList.setCellRenderer(listRenderer);
		jlistGroupChats.setCellRenderer(listRenderer);
		jlistConversation.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlistContactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlistGroupChats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		spConversationWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		westPanel.add(otherUserInfoPanel, BorderLayout.NORTH);
		westPanel.add(spConversationWindow);
		westPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		//Östra panelen
		JPanel eastPanel = new JPanel(new BorderLayout());
		eastPanel.setPreferredSize(new Dimension(150, 400)); 
		eastPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		//Östra panelen - knappen "add user"
		JPanel addRemovePanel = new JPanel(new BorderLayout());
		addRemovePanel.setPreferredSize(new Dimension(100, 40));
		btnAddContact.setPreferredSize(new Dimension(140, 17));
		btnRemoveCon.setPreferredSize(new Dimension(140, 17));
		addRemovePanel.add(btnAddContact, BorderLayout.NORTH);
		addRemovePanel.add(btnRemoveCon, BorderLayout.SOUTH);
		
		
		
		//Östra panelen - kontaktlista
		JPanel contactListPanel = new JPanel(new BorderLayout());
		contactListPanel.setPreferredSize(new Dimension(100, 160));
		lblContactList.setPreferredSize(new Dimension(140, 15));
		spContactList.setPreferredSize(new Dimension(140, 140));
		spContactList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		contactListPanel.add(lblContactList, BorderLayout.NORTH);
		contactListPanel.add(spContactList, BorderLayout.SOUTH);
		
		//Östra panelen - gruppchatt
		JPanel groupChatsPanel = new JPanel(new BorderLayout());
		groupChatsPanel.setPreferredSize(new Dimension(140, 160));
		lblGroupChats.setPreferredSize(new Dimension(140, 15));
		spGroupChats.setPreferredSize(new Dimension(140, 140));
		spGroupChats.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		groupChatsPanel.add(lblGroupChats, BorderLayout.NORTH);
		groupChatsPanel.add(spGroupChats, BorderLayout.SOUTH);

		
		//Östra panelen - panel för kontaktlista + gruppchatt
		JPanel centerEastPanel = new JPanel(new GridLayout(2,1)); //contactListPanel + groupChatsPanel
		centerEastPanel.setPreferredSize(new Dimension(140, 320));
		centerEastPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		centerEastPanel.add(contactListPanel);
		centerEastPanel.add(groupChatsPanel);
		
		//Östra panelen - knappen "add group chat"
		JPanel addLeavePanel = new JPanel(new BorderLayout());
		addLeavePanel.setPreferredSize(new Dimension(140, 40));
		btnAddGroupChat.setPreferredSize(new Dimension(140, 17));
		btnLeaveGroupChat.setPreferredSize(new Dimension(140, 17));
		addLeavePanel.add(btnAddGroupChat, BorderLayout.NORTH);
		addLeavePanel.add(btnLeaveGroupChat, BorderLayout.SOUTH);
		
		//Östra panelen - lägga ihop alla tillhörande komponenter
		eastPanel.add(addRemovePanel, BorderLayout.NORTH);
		eastPanel.add(centerEastPanel, BorderLayout.CENTER);
		eastPanel.add(addLeavePanel, BorderLayout.SOUTH);
		
		//Södra panelen
		txtMessageField.setLineWrap(true);
		spMessageField.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		spMessageField.setPreferredSize(new Dimension(380, 90));
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setPreferredSize(new Dimension(540, 100));
		southPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JPanel btnSendPanel = new JPanel(new GridLayout(2, 1));
		btnSendPanel.setPreferredSize(new Dimension(140, 90)); 
		btnSendPanel.add(btnSend);
		btnSendPanel.add(btnSendFile);
		southPanel.add(spMessageField, BorderLayout.WEST);
		southPanel.add(btnSendPanel, BorderLayout.EAST);

		//Huvudpanelen - allt läggs samman
		mainPanel.add(northPanel, BorderLayout.NORTH);
		mainPanel.add(westPanel, BorderLayout.CENTER); 
		mainPanel.add(eastPanel, BorderLayout.EAST);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		add(mainPanel);
		
		//Koppla knappar till actionListener
		ButtonListener listener = new ButtonListener();
		btnLogout.addActionListener(listener);
		btnSend.addActionListener(listener);
		btnSendFile.addActionListener(listener);
		btnAddGroupChat.addActionListener(listener);
		btnAddContact.addActionListener(listener);
		btnRemoveCon.addActionListener(listener);
		btnLeaveGroupChat.addActionListener(listener);
	}

	/**
	 * genererar fönstret för när en användare väljer att bifoga en fil
	 * i chatten. Metoden returnerar sedan den valda filen. 
	 * @return returnerar den valda filen.
	 */
	private File generateSelectFile() {
		setSystemLookAndFeel();
		File file = null; //Objekttyp ska ändras efter krav, t.ex. pdf osv. även koden nedan
		JFileChooser chooser = new JFileChooser();
 	    
 	    int returnChoice = chooser.showOpenDialog(null);
		if (returnChoice == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();
			
		}
		setMetalLookAndFeel();
		return file;
	}
	
	/**
	 * gör att gränssnittets utseende anpassar sig utefter det operativsystem
	 * det befinner sig på.
	 */
	private void setSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	/**
	 * gör att gränssnittets utseende erhåller den designen som kallas
	 * "MetalLookAndFeel" som kan liknas vid det utseende som JComponents har.
	 */
	private void setMetalLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	
	public void setChattingWith(String userName) {
		this.lblChattingWith.setText("You are chatting with: " + userName);
	}
	
	public void setOtherUserStatus(boolean isOnline) {
		if(isOnline) {
			this.lblOtherUserStatus.setText("<html>User status: <font color='green'>Online</font></html>");
		} else {
			this.lblOtherUserStatus.setText("<html>User status: <font color='red'>Offline</font></html>");
		}
	}
	
	/**
	 * hämtar innehåller för skrivfönstret i chatten.
	 * @return returnerar innehållet i skrivfönstret. 
	 */
	public String getMessageTxt() {
		return txtMessageField.getText();
	}

	/**
	 * tömmer chatfönstrets innehåll
	 */
	public void clearMessageField() {
		txtMessageField.setText("");
	}
	
	/**
	 * Byter konversationen i konversationsfönstret.
	 * @param conversationModel Den konversation som ska visas i konversationsfönstret.
	 */
	public void setConversationModel(ListModel<JLabel> conversationModel) {
		jlistConversation.setModel(conversationModel);
	}
	
	public void scrollDownConversation() {
		int lastIndex = jlistConversation.getModel().getSize() - 1;
		if (lastIndex >= 0) {
			jlistConversation.ensureIndexIsVisible(lastIndex);
		}
	}
	
	public void selectContact(int contactIndex, boolean isGroup) {
		if(isGroup == true) {
			jlistGroupChats.setSelectedIndex(contactIndex);
			jlistGroupChats.ensureIndexIsVisible(contactIndex);
		} else {
			jlistContactList.setSelectedIndex(contactIndex);
			jlistContactList.ensureIndexIsVisible(contactIndex);
		}
	}
	
	public boolean isGroupInFocus() {
		return isGroupInFocus;
	}

	/**
	 * Converts a File-object to a byte array
	 *
	 * @param file the file to convert
	 * @return the file as a byte array
	 */
	private byte[] readFileToByteArray(File file){
		FileInputStream fis = null;
		byte[] byteArray = new byte[(int) file.length()];
		try{
			fis = new FileInputStream(file);
			fis.read(byteArray);
			fis.close();
		}catch(IOException ioExp){
			ioExp.printStackTrace();
		}
		return byteArray;
	}

	private class ListListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if(e.getSource() == jlistContactList) {
				JLabel selectedContact = jlistContactList.getSelectedValue();
				if(selectedContact != null) {
					isGroupInFocus = false;
					jlistGroupChats.clearSelection();
					mainController.showConversation(selectedContact, isGroupInFocus);
				}
			} else if (e.getSource() == jlistGroupChats) {
				JLabel selectedGroup = jlistGroupChats.getSelectedValue();
				if(selectedGroup != null) {
					isGroupInFocus = true;
					jlistContactList.clearSelection();
					mainController.showConversation(selectedGroup, isGroupInFocus);
				}
			}
		}
	}
	
	/**
	 * inre klass som hanterar händelser för
	 * knapptryck i chattfönstret. 
	 * @author grupp09 Xhat
	 *
	 */
	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == btnLogout)
				mainController.disconnect();
			else if (event.getSource() == btnSend)
				sendTextMessage();
			else if (event.getSource() == btnSendFile)
				sendFileMessage();
			else if (event.getSource() == btnAddGroupChat)
				mainController.showAddGroupChatPanel();
			else if (event.getSource() == btnAddContact)
				mainController.showAddContactPanel();
			else if (event.getSource() == btnRemoveCon)
				removeContact();
			else if (event.getSource() == btnLeaveGroupChat)
				leaveGroupChat();
		}

		private void sendTextMessage() {
			mainController.restartDisconnectTimer();
			JLabel selectedContact = (isGroupInFocus) ? jlistGroupChats.getSelectedValue() : jlistContactList.getSelectedValue();
			byte[] bytesOfMessage = null;
			try {
				bytesOfMessage = getMessageTxt().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (bytesOfMessage.length > 3 * Math.pow(10, 6))
				JOptionPane.showMessageDialog(null, "Please write a message consisting of less than 3 MB");
			else if (selectedContact != null) {
				File file = new File("temp/file.txt");
				try {
					FileUtils.writeByteArrayToFile(file, getMessageTxt().getBytes());
					File encryptedFile = Encryption.encryptFile(file, getEncryptionKey(selectedContact.getName()), "pub");
					byte[] data = readFileToByteArray(encryptedFile);
					mainController.sendMessage(selectedContact.getName(), data, "", isGroupInFocus, Message.TYPE_TEXT);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else
				JOptionPane.showMessageDialog(null, "Please select a contact or a group.", "Info", JOptionPane.INFORMATION_MESSAGE);
		}

		private void sendFileMessage() {
			mainController.restartDisconnectTimer();
			JLabel selectedContact = (isGroupInFocus) ? jlistGroupChats.getSelectedValue() : jlistContactList.getSelectedValue();
			if (selectedContact != null) {
				File file = generateSelectFile();
				if (file != null) {
					try {
						File encryptedFile = Encryption.encryptFile(file, getEncryptionKey(selectedContact.getName()), "pub");
						byte[] fileData = readFileToByteArray(encryptedFile);
						String filename = file.getName();
						mainController.sendMessage(selectedContact.getName(), fileData, filename, isGroupInFocus, Message.TYPE_FILE);
						new File(file.getPath()+".enc").delete();
						removeEncryptionKey(selectedContact.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				JOptionPane.showMessageDialog(null, "Please select a contact or a group.", "Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}

		private String getEncryptionKey(String username){
			String filename = "key/"+username+".pub";
			mainController.getUserKey(username);
			return filename;
		}

		private void removeEncryptionKey(String username){
			new File("key/"+username+".pub").delete();
		}

		private void removeContact() {
			JLabel selectedContact = jlistContactList.getSelectedValue();
			if (selectedContact != null) {
				Object[] options = {"Yes", "No"};
				int choice = JOptionPane.showOptionDialog(null,
						"Do you want to remove " + selectedContact.getName() + " from contact list?",
						"Remove Contact", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (choice == 0) {
					mainController.removeContact(selectedContact.getName());
				}
			} else {
				JOptionPane.showMessageDialog(null, "Please select a contact.", "Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}

		private void leaveGroupChat() {
			JLabel selectedGroup = jlistGroupChats.getSelectedValue();
			if (selectedGroup != null) {
				Object[] options = { "Yes", "No" };
				int choice = JOptionPane.showOptionDialog(null,
						"Do you want to leave " + selectedGroup.getText() + "?", "Leave Group",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (choice == 0)
					mainController.leaveGroupChat(selectedGroup.getName());
			} else {
				JOptionPane.showMessageDialog(null, "Please select a group.", "Info", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * inre klass som gör att en JList visar JLabels som text eller bild utan markering.
	 */
	private class JListConversationRenderer extends DefaultListCellRenderer {
		@Override
	    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel elem = (JLabel) value;
			return elem;
	    }
	}
	
	/**
	 * inre klass som gör att en JList visar JLabels som text eller bild med markering.
	 */
	private class JListContactsRenderer extends DefaultListCellRenderer {
		private final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
		private final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setComponentOrientation(list.getComponentOrientation());

			Color bg = null;
			Color fg = null;

			JList.DropLocation dropLocation = list.getDropLocation();
			if (dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index) {
				bg = DefaultLookup.getColor(this, ui, "List.dropCellBackground");
				fg = DefaultLookup.getColor(this, ui, "List.dropCellForeground");
				isSelected = true;
			}

			if (isSelected) {
				setBackground(bg == null ? list.getSelectionBackground() : bg);
				setForeground(fg == null ? list.getSelectionForeground() : fg);
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}

			setEnabled(list.isEnabled());
			setFont(list.getFont());

			if (value instanceof Icon) {
				setIcon((Icon) value);
				setText("");
			} else if (value instanceof JLabel) {
				setIcon(((JLabel) value).getIcon());
				setText(((JLabel) value).getText());
				setFont(((JLabel) value).getFont());
				setForeground(((JLabel) value).getForeground());
			} else {
				setIcon(null);
				setText((value == null) ? "" : value.toString());
			}

			Border border = null;
			if (cellHasFocus) {
				if (isSelected) {
					border = DefaultLookup.getBorder(this, ui, "List.focusSelectedCellHighlightBorder");
				}
				if (border == null) {
					border = DefaultLookup.getBorder(this, ui, "List.focusCellHighlightBorder");
				}
			} else {
				border = getNoFocusBorder();
			}
			setBorder(border);
			return this;
		}
		
		private Border getNoFocusBorder() {
	        Border border = DefaultLookup.getBorder(this, ui, "List.cellNoFocusBorder");
	        if (System.getSecurityManager() != null) {
	            if (border != null) return border;
	            return SAFE_NO_FOCUS_BORDER;
	        } else {
	            if (border != null &&
	                    (noFocusBorder == null ||
	                    noFocusBorder == DEFAULT_NO_FOCUS_BORDER)) {
	                return border;
	            }
	            return noFocusBorder;
	        }
	    }
	}
	
	private class ConversationML implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent mouseEvent) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent mouseEvent) {
			int selectedIndex = jlistConversation.getSelectedIndex();
			if(selectedIndex != -1) {
				mainController.decodeAndReplaceMessageInConversation(selectedIndex, isGroupInFocus);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}
		
	}

	// test --> kontrollera utseende för GUI
	public static void main(String[] args) {
		client.Data data = new client.Data();
		MainPanel panel = new MainPanel(new MainController(new client.ClientCommunications("127.0.0.1", 5555, data), data), new DefaultListModel<>(), new DefaultListModel<>());
//		JFrame frameMainPanel = new JFrame("Xhat: " + "dummy");
//		frameMainPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frameMainPanel.setResizable(false);
//		frameMainPanel.add(panel);
//		frameMainPanel.pack();
//		frameMainPanel.setLocationRelativeTo(null);
//		frameMainPanel.setVisible(true);
	}
}
