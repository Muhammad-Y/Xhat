package client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import client.ClientCommunications;
import client.Contact;
import client.Data;
import client.GroupChat;
import client.Steganography;
import common.Message;

public class MainController {
	private Steganography stego = new Steganography();
	private DefaultListModel<JLabel> groupsModel = new DefaultListModel<>(); //Måste synkroniseras
	private DefaultListModel<JLabel> contactsModel = new DefaultListModel<>();//Måste synkroniseras
	private DefaultListModel<String> resultsModel = new DefaultListModel<>();
	private String[] searchResults;
	private String lastSearchString;
	private JFrame frameMain;
	private JFrame frameAddGroup;
	private JFrame frameAddContact;
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
    public static Timer timer;
    private final static int DISCONNECT_MILLISEC = 50*60*1000;


	public MainController(ClientCommunications clientCommunications, Data data) {
		this.clientCommunications = clientCommunications;
		this.data = data;
		showMainPanel();
		startDisconnectTimer();
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
			if (isOnline == true) {
				jLabelContact.setForeground(new Color(0, 128, 0));
			}
			if (contactInFocus != null && contactName.equals(contactInFocus.getName())) {
				mainPanel.setOtherUserStatus(isOnline);
			}
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

	public boolean sendMessage(String recipient, byte[] payload, boolean isGroupMsg, int type) {
		boolean success = false;
		try {
			byte[] stegoData = Steganography.imageToByteArray(stego.encode(payload));
			Message message = new Message(recipient, isGroupMsg, type, stegoData);
			success = clientCommunications.sendMessage(message);
			if(success) {
				message.setSender("You");
				Contact contact = (isGroupMsg) ? data.getGroup(recipient) : data.getContact(recipient);
				addMessageToConversation(contact, message, true);
				mainPanel.clearMessageField();
			}
		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(null, "File is too big: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return success;
	}
	
	public void createNewGroup(String groupName, DefaultListModel<String> membersModel) {
		//[groupname, ourself, member2, member3...]
		String[] newGroup;
		if (groupName != null && groupName.length() > 0) {
			if (membersModel != null && membersModel.size() >= 2) {
				newGroup = new String[membersModel.size() + 2];
				newGroup[0] = groupName;
				newGroup[1] = getUserName();
				for (int i = 2; i < newGroup.length; i++) {
					newGroup[i] = membersModel.getElementAt(i-2);
				}
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
		JOptionPane.showMessageDialog(frameMain, "You got a new message from " + userNameToNotify);	
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
				addMessageToConversation(contact, message, false);
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
		if(contactInFocus != null) {
			isSelected = contactInFocus.getName().equals(contactName);
		}
		return isSelected;
	}
	
	private JLabel decodeMessage(Message message) {
		JLabel jLabelMessage = null;
		BufferedImage stegoImage = Steganography.byteArrayToImage(message.getStegoData());
		if (stegoImage != null) {
			byte[] payloadData = stego.decode(stegoImage);
			int messageType = message.getType();
			if(messageType == Message.TYPE_TEXT) {
				String payloadText = new String(payloadData);
				jLabelMessage = new JLabel(message.getSender() + ": " + payloadText);
			} else if (messageType == Message.TYPE_IMAGE) {
				BufferedImage image = Steganography.byteArrayToImage(payloadData);
				ImageIcon payloadImage = new ImageIcon(image);
				//TODO: return 2 jlabels, one for sender-string and one for image.
//				JLabel jLabelSender = new JLabel(message.getSender() + ": ");
				jLabelMessage = new JLabel(payloadImage, SwingConstants.LEFT);
			} else {
				//TODO: Handle files
				jLabelMessage = new JLabel(message.getSender() + ": Itsa filee");
			}
		} else {
			System.out.println("Message decode error");
		}
		return jLabelMessage;
	}

	public void addMessageToConversation(Contact contact, Message message, boolean decode) {
		JLabel jLabelMessage;
		if(decode == true) {
			jLabelMessage = decodeMessage(message);
		} else {
			ImageIcon stegoImage = new ImageIcon(message.getStegoData());
			jLabelMessage = new JLabel(stegoImage, SwingConstants.LEFT);
			String key = contact.addUndecodedMessage(message);
			jLabelMessage.setName(key);
		}
		if (jLabelMessage != null) {
			jLabelMessage.setFont(plainMessageFont);
//			jLabelMessage.setHorizontalAlignment(SwingConstants.RIGHT);
			contact.addMessageToConversation(jLabelMessage);
			mainPanel.scrollDownConversation();
		}
	}
	
	private JLabel[] splitMessage(String messageText, String sender) {
		int spaceIndex = 0;
		String split = null;
		JLabel jLabelMessage;
		if(messageText.length() > 1) {
			
			for(int i = 0; i < messageText.length(); i++) {
				if(i != 0 && i % 10 == 0) {
					System.out.println("Nu splittar vi!");
					spaceIndex = messageText.lastIndexOf(' ', i);
					split = messageText.substring(i, spaceIndex);
					if (i <= 100) {
//						jLabelMessage = new JLabel(message.getSender() + ": " + split);
					} else {
						jLabelMessage = new JLabel(split);
					}
//					contact.addMessageToConversation(jLabelMessage);
				}
			}
			split = messageText.substring(spaceIndex, messageText.length());
			jLabelMessage = new JLabel(split);
//			contact.addMessageToConversation(jLabelMessage);
			
		}
		return null;
	}
	
	public void decodeAndReplaceMessageInConversation(int messageIndex, boolean isGroup) {
		String contactInFocusIdentifier = contactInFocus.getName();
		Contact contact = (isGroup) ? data.getGroup(contactInFocusIdentifier) : data.getContact(contactInFocusIdentifier);
		if (contact != null) {
			DefaultListModel<JLabel> conversationModel = contact.getConversation();
			JLabel selectedMessage = conversationModel.getElementAt(messageIndex);
			if (selectedMessage != null) {
				String selectedMessageKey = selectedMessage.getName();
				Message undecodedMessage = contact.popUndecodedMessage(selectedMessageKey);
				if (undecodedMessage != null) {
					JLabel decodedMessage = decodeMessage(undecodedMessage);
					if (decodedMessage != null) {
						decodedMessage.setFont(plainMessageFont);
						conversationModel.set(messageIndex, decodedMessage);
					}
				}
			}
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
		for (String contactName : data.getContactKeys()) {
			if (contactName.toLowerCase().startsWith(searchString.toLowerCase())) {
				resultsModel.addElement(contactName);
			}
		}
	}

	public void searchUser(String searchString) {
		boolean sameString = searchString.length() >= 2 && searchString.substring(0, 2).equals(lastSearchString);
		boolean filterLastResults = this.searchResults != null && sameString;
		boolean contactServer = searchString.length() == 2 && !sameString;
		if (contactServer) {
			this.lastSearchString = searchString.substring(0, 2);
			clientCommunications.searchUser(searchString);
		} else if (filterLastResults) {
			resultsModel.clear();
			for (String contactName : searchResults) {
				if (contactName.toLowerCase().startsWith(searchString.toLowerCase())) {
					resultsModel.addElement(contactName);
				}
			}
		} else {
			resultsModel.clear();
		}
	}

	public void updateSearchResults(String[] searchResults) {
		this.searchResults = searchResults;
		resultsModel.clear();
		for (String contactName : searchResults) {
			resultsModel.addElement(contactName);
		}
	}

	public void sendNewContactRequest(String userName, boolean decline) {
		String[] requestObj = new String[]{userName, Boolean.toString(decline)};
		clientCommunications.sendNewContactRequest(requestObj);
	}

	public void notifyNewContactRequest(String requestFromUserName) {
		Object[] options = { "Yes", "No" };
		int choice = JOptionPane.showOptionDialog(null, requestFromUserName + 
				" wants to add you as a contact. Do you accept?", "Contact Request",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if (choice == 0) {
			sendNewContactRequest(requestFromUserName, false);
		} else {
			sendNewContactRequest(requestFromUserName, true);
		}
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

}
