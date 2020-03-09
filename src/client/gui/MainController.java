package client.gui;

import client.*;
import common.Encryption;
import common.Message;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.Timer;

public class MainController {
	private DefaultListModel<JLabel> groupsModel = new DefaultListModel<>(); //Måste synkroniseras
	private DefaultListModel<JLabel> contactsModel = new DefaultListModel<>();//Måste synkroniseras
	private DefaultListModel<String> resultsModel = new DefaultListModel<>();
	private String[] searchResults;
	private String lastSearchString, downloadPath;
	private JFrame frameMain, frameAddGroup, frameAddContact;
	private MainPanel mainPanel;
	private AddGroupChatPanel addGroupChatPanel;
	private AddContactPanel addContactPanel;
	private ClientCommunications clientCommunications;
	private Data data;
	private Font plainContactFont = new Font("PlainFont12", Font.PLAIN, 12);
	private Font boldContactFont = new Font("BoldFont12", Font.BOLD, 12);
	private Font plainMessageFont = new Font("PlainFont13", Font.PLAIN, 13);
	private Font boldMessageFont = new Font("BoldFont13", Font.BOLD, 13);
	private JLabel contactInFocus;
	private LinkedList <String> undeliveredMessageQueue = new LinkedList<>();
	private Timer notificationTimer;
    public static Timer timer;
    private final static int NOTIFICATION_MILLISEC = 2*1000;
    private final static int DISCONNECT_MILLISEC = 50*60*1000;
    private static String ENCRYPTION_KEY;


	public MainController(ClientCommunications clientCommunications, Data data) {
		this.clientCommunications = clientCommunications;
		this.data = data;
		ENCRYPTION_KEY = "key/"+ getUserName()+".pvt";
		setDownloadPath();
		showMainPanel();
		startDisconnectTimer();
		startNotificationTimer();
	}
	
	private void showMainPanel() {
		mainPanel = new MainPanel(this, contactsModel, groupsModel);
		frameMain = new JFrame("Xhat: " + getUserName());
		frameMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //handle operation in windowClosing() method of a registered WindowListener object. 
		//frameMain.addWindowListener(null); //TODO Create WindowListener
		frameMain.setResizable(false);
		frameMain.add(mainPanel);
		frameMain.pack();
		frameMain.setLocationRelativeTo(null);
		frameMain.setVisible(true);
	}
	
	private void disposeFrameMain() {
		if(frameMain != null) {
			frameMain.dispose();
		}
	}

	public void setDownloadPath() {
		String home = System.getProperty("user.home");
		String separator = System.getProperty("file.separator");
		downloadPath = home + separator + "Downloads" + separator;
	}
	
	public void showAddGroupChatPanel() {
		disposeFrameAddContact();
		resultsModel.clear();
		Set<String> keys = data.getContactKeys();
		for (String contactName : keys) {
			resultsModel.addElement(contactName);
		}
		addGroupChatPanel = new AddGroupChatPanel(this, resultsModel);
		frameAddGroup = new JFrame("Add group chat");
		frameAddGroup.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameAddGroup.setResizable(false);
		frameAddGroup.add(addGroupChatPanel);
		frameAddGroup.pack();
		frameAddGroup.setLocationRelativeTo(null); 
		frameAddGroup.setVisible(true);
	}
	
	public void updateGroupList() {
		groupsModel.clear();
		Collection<GroupChat> groups = data.getGroups();
		for (GroupChat group : groups) {
			JLabel jLabelGroup = new JLabel();
			jLabelGroup.setName(group.getGroupId());
			if (group.hasUnreadMessages()) {
				jLabelGroup.setText("*" + group.getName());
				jLabelGroup.setFont(boldContactFont);
				groupsModel.add(0, jLabelGroup);
			} else {
				jLabelGroup.setText(group.getName());
				jLabelGroup.setFont(plainContactFont);
				groupsModel.addElement(jLabelGroup);
			}
		}
		if (mainPanel != null && contactInFocus != null) {
			mainPanel.selectContact(getPosInListModel(groupsModel, contactInFocus.getName()), true);
		}
	}
	
	public void disposeFrameAddGroup() {
		if(frameAddGroup != null) {
			frameAddGroup.dispose();
		}
	}
	
	public void showAddContactPanel() {
		disposeFrameAddGroup();
		resultsModel.clear();
		searchResults = null;
		lastSearchString = null;
		addContactPanel = new AddContactPanel(this, resultsModel);
		frameAddContact = new JFrame("Add contact");
		frameAddContact.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameAddContact.setResizable(false);
		frameAddContact.add(addContactPanel);
		frameAddContact.pack();
		frameAddContact.setLocationRelativeTo(null);
		frameAddContact.setVisible(true);
	}
	
	public void updateContactsList() {
		contactsModel.clear();
		Collection<Contact> contacts = data.getContacts();
		for (Contact contact : contacts) {
			JLabel jLabelContact = new JLabel();
			boolean isOnline = contact.isOnline();
			String contactName = contact.getName();
			jLabelContact.setName(contactName);
			if (isOnline) jLabelContact.setForeground(new Color(0, 128, 0));
			if (contactInFocus != null && contactName.equals(contactInFocus.getName())) mainPanel.setOtherUserStatus(isOnline);
			if (contact.hasUnreadMessages()) {
				jLabelContact.setText("*" + contactName);
				jLabelContact.setFont(boldContactFont);
				contactsModel.add(0, jLabelContact);
			} else {
				jLabelContact.setText(contactName);
				jLabelContact.setFont(plainContactFont);
				contactsModel.addElement(jLabelContact);
			}
		}
		if (mainPanel != null && contactInFocus != null) {
			mainPanel.selectContact(getPosInListModel(contactsModel, contactInFocus.getName()), false);
		}
	}
	
	public void disposeFrameAddContact() {
		if(frameAddContact != null) {
			frameAddContact.dispose();
		}
	}

	public String getUserName() {
		return clientCommunications.getUserName();
	}

	public void disconnect() {
		clientCommunications.disconnect();
	}

	public boolean sendMessage(String recipient, byte[] bytes, String filename, boolean isGroupMsg, int type, String s) {
		boolean success = false;
		try {
			if(type == Message.TYPE_TEXT) {
				byte[] data = Encryption.encryptText(mainPanel.getMessageTxt(), mainPanel.getEncryptionKey(recipient)).getBytes("UTF-8");
				success = clientCommunications.sendMessage(new Message(recipient, isGroupMsg, filename, type, data,s));
			}
			else success = clientCommunications.sendMessage(new Message(recipient, isGroupMsg, filename, type, bytes,s));
			if(success) {
				Message message = new Message(recipient, isGroupMsg, filename, type, bytes,s);
				message.setSender("You");
				Contact contact = (isGroupMsg) ? data.getGroup(recipient) : data.getContact(recipient);
				addMessageToConversation(contact, message, message.getType()==0);
				mainPanel.clearMessageField();
			}
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(null, "File is too big: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}

	public void getUserKey(String username){
		clientCommunications.getUserKey(username);
	}
	
	public void createNewGroup(String groupName, DefaultListModel<String> membersModel) {
		//[groupname, ourself, member2, member3...]
		String[] newGroup;
		if (groupName != null && groupName.length() > 0) {
			if (membersModel != null){// && membersModel.size() >= 2) {
				newGroup = new String[membersModel.size() + 2];
				newGroup[0] = groupName;
				newGroup[1] = getUserName();
				for (int i = 2; i < newGroup.length; i++)
					newGroup[i] = membersModel.getElementAt(i-2);
				clientCommunications.createNewGroup(newGroup);
				disposeFrameAddGroup();
				JOptionPane.showMessageDialog(null, "New group successfully created: " + groupName, "Info", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null, "A group needs at least 2 other members.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			JOptionPane.showMessageDialog(null, "Please choose a group name.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void leaveGroupChat(String groupId) {
		clientCommunications.leaveGroup(groupId);
	}

	public void removeContact(String username) {
		clientCommunications.removeContact(username);
	}

	/**
	 * Add asterisk and set bold username in contact- or group-list and move contactName to top.
	 * @param userNameToNotify
	 * @param isGroupMsg
	 */
	public void notifyNewMessage(String userNameToNotify, boolean isGroupMsg) {
		DefaultListModel<JLabel> listModel = (isGroupMsg) ? groupsModel : contactsModel;
		synchronized (listModel) {
			int contactPos = getPosInListModel(listModel, userNameToNotify);
			if (contactPos != -1) {
				JLabel jLabel = listModel.get(contactPos);
				if (!jLabel.getText().startsWith("*")) {
					jLabel.setText("*" + jLabel.getText());
					jLabel.setFont(boldContactFont);
				}
				listModel.add(0, listModel.remove(contactPos));
			}
		}
		undeliveredMessageQueue.add(userNameToNotify);
		restartNotificationTimer();

	//	JOptionPane.showMessageDialog(frameMain, "You got a new message from " + userNameToNotify);
	}

	private void showNotificationQueue() {
		if(undeliveredMessageQueue.size() == 1) {
			JOptionPane.showMessageDialog(frameMain, "You got a new message from " + undeliveredMessageQueue.get(1));
			undeliveredMessageQueue.clear();
			restartNotificationTimer();


		} else if(undeliveredMessageQueue.size() > 1) {
			JOptionPane.showMessageDialog(frameMain, "You got " + undeliveredMessageQueue.size() + " new messages");
			undeliveredMessageQueue.clear();
			restartNotificationTimer();
		}


	}
	
	private int getPosInListModel(DefaultListModel<JLabel> listModel, String contactName) {
		int pos = -1;
		synchronized (listModel) {
			JLabel jLabel;
			int size = listModel.size();
			for (int i = 0; i < size; i++) {
				jLabel = listModel.get(i);
				if (jLabel != null && jLabel.getName().equals(contactName)) {
					pos = i;
				}
			}
		}
		return pos;
	}
	
	public void showConversation(JLabel selectedContact, boolean isGroup) {
		String contactIdentifier = selectedContact.getName();
		Contact contact = (isGroup) ? data.getGroup(contactIdentifier) : data.getContact(contactIdentifier) ;
		String contactName = contact.getName();
		if (contact != null) {
			if (selectedContact.getText().startsWith("*")) {
				selectedContact.setText(contactName);
				selectedContact.setFont(plainContactFont);
			}
			this.contactInFocus = selectedContact;
			while (contact.hasUnreadMessages()) {
				Message message = contact.getNextUnreadMessage();
				addMessageToConversation(contact, message, message.getType()==0);
				contact.removeNextUnreadMessage();
			}
			mainPanel.setConversationModel(contact.getConversation());
			mainPanel.setChattingWith(contactName);
			mainPanel.setOtherUserStatus(contact.isOnline());
			mainPanel.scrollDownConversation();
		}
	}

	public boolean isContactSelected(String contactName) {
		boolean isSelected = false;
		if(contactInFocus != null) isSelected = contactInFocus.getName().equals(contactName);
		return isSelected;
	}

	public void addMessageToConversation(Contact contact, Message message, boolean isText) {
		JLabel jLabelMessage = null;
		if(isText)
			try {
				String text = "";
				if(message.getSender() != "You")
					if(!message.isGroupMessage()) text = Encryption.decryptText(new String(message.getFileData()), ENCRYPTION_KEY);
					else text = Encryption.decryptText(new String(message.getFileData()), "key/"+message.getRecipient()+".pvt");
				else text = new String(message.getFileData());
				jLabelMessage = new JLabel(text);
			} catch (Exception e) {
				e.printStackTrace();
			}
		else
			try {
				if(message.getSender() != "You") {
					File file = new File(downloadPath + message.getFileName() + ".enc");
					FileUtils.writeByteArrayToFile(file, message.getFileData());
					if(!message.isGroupMessage()) Encryption.decryptFile(file, ENCRYPTION_KEY);
					else Encryption.decryptFile(file, "key/"+message.getRecipient()+".pvt");
					file.delete();
				}
				ImageIcon iconLogo = new ImageIcon(message.getFilePath());

				jLabelMessage = new JLabel();
				jLabelMessage.setIcon(iconLogo);
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (jLabelMessage != null) {
			jLabelMessage.setFont(plainMessageFont);
			contact.addMessageToConversation(jLabelMessage);
			mainPanel.scrollDownConversation();
		}
	}

	public void disconnected(String message) {
		SwingUtilities.invokeLater(() -> {
			disposeFrameAddContact();
			disposeFrameAddGroup();
			disposeFrameMain();
			new LogInController(clientCommunications, getUserName(), "");
			JOptionPane.showMessageDialog(null, message, "Info", JOptionPane.INFORMATION_MESSAGE);
		});
	}
	
	public void searchContact(String searchString) {
		resultsModel.clear();
		for (String contactName : data.getContactKeys())
			if (contactName.toLowerCase().startsWith(searchString.toLowerCase()))
				resultsModel.addElement(contactName);
	}

	public void searchUser(String searchString) {
		boolean sameString = searchString.length() >= 2 && searchString.substring(0, 2).equals(lastSearchString);
		boolean filterLastResults = this.searchResults != null && sameString;
		boolean contactServer = searchString.length() == 2 && !sameString;
		if (contactServer) {
			this.lastSearchString = searchString.substring(0, 2);
			clientCommunications.searchUser(searchString);
		}
		else if (filterLastResults) {
			resultsModel.clear();
			for (String contactName : searchResults)
				if (contactName.toLowerCase().startsWith(searchString.toLowerCase()))
					resultsModel.addElement(contactName);
		}
		else resultsModel.clear();
	}

	public void updateSearchResults(String[] searchResults) {
		this.searchResults = searchResults;
		resultsModel.clear();
		for (String contactName : searchResults) resultsModel.addElement(contactName);
	}

	public void sendNewContactRequest(String userName, boolean decline) {
		clientCommunications.sendNewContactRequest(new String[]{userName, Boolean.toString(decline)});
	}

	public void notifyNewContactRequest(String requestFromUserName) {
		Object[] options = { "Yes", "No" };
		int choice = JOptionPane.showOptionDialog(null, requestFromUserName + 
				" wants to add you as a contact. Do you accept?", "Contact Request",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if (choice == 0) sendNewContactRequest(requestFromUserName, false);
		else sendNewContactRequest(requestFromUserName, true);
	}
	

    private void startDisconnectTimer() {
        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
            	clientCommunications.disconnect("Disconnected from server due to inactivity");
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, DISCONNECT_MILLISEC);
    }
    
    public void restartDisconnectTimer() {
        timer.cancel();
        startDisconnectTimer();
    }

	private void startNotificationTimer() {
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				showNotificationQueue();

			}
		};
		notificationTimer = new Timer();
		notificationTimer.schedule(timerTask, NOTIFICATION_MILLISEC);
	}
	private void restartNotificationTimer() {
		notificationTimer.cancel();
		startNotificationTimer();
	}


}
