package client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;

import common.Message;

public class Contact {
	private final String name;
	private boolean isOnline;
	private LinkedList<Message> unreadMessages = new LinkedList<>();
	private HashMap<String,Message> undecodedMessages = new HashMap<>();
	private DefaultListModel<JLabel> conversation;
	
	public Contact(String name) {
		this.name = name;
		this.conversation = new DefaultListModel<JLabel>();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isOnline() {
		return isOnline;
	}
	
	public void setIsOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}
	
	public DefaultListModel<JLabel> getConversation() {
		return conversation;
	}
	
	public void addUnreadMessage(Message message) {
		unreadMessages.add(message);
	}
	
	public boolean hasUnreadMessages() {
		return !unreadMessages.isEmpty();
	}
	
	public Message getNextUnreadMessage() {
		return unreadMessages.get(0);
	}
	
	public void removeNextUnreadMessage() {
		unreadMessages.remove(0);
	}

	public void addMessageToConversation(JLabel message) {
		conversation.addElement(message);
	}
	
	public String addUndecodedMessage(Message message) {
		String key = Integer.toString(message.hashCode());
		synchronized (undecodedMessages) {
			undecodedMessages.put(key, message);
		}
		return key;
	}

	public Message popUndecodedMessage(String key) {
		synchronized (undecodedMessages) {
			return undecodedMessages.remove(key);
		}
	}
}
