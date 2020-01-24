package server;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import common.Message;

/**
 * User lagrar data som är associerad med en viss användare.
 */
public class User implements Serializable {
	private transient ClientConnection clientConnection;
	private final String userName;
	private final String protectedPassword;
	private List<User> contacts = Collections.synchronizedList(new LinkedList<>());
	private List<Message> bufferedMessages = Collections.synchronizedList(new LinkedList<>());
	private List<Group> groups = Collections.synchronizedList(new LinkedList<>());
	private List<User> contactReqestsFrom = Collections.synchronizedList(new LinkedList<>());
	private List<User> contactReqestsTo = Collections.synchronizedList(new LinkedList<>());
	
	/**
	 * Skapar en ny User med användarnamet <code>userName</code> och lösenordet <code>password</code>.
	 * Lösenord skyddas med SHA-256
	 * @param userName Användarens användarnamn.
	 * @param password Användarens lösenord.
	 */
	public User(String userName, String password) {
		this.userName = userName;
		this.protectedPassword = sha256(password);
	}

	/**
	 * Lägger till ett Message-objekt i bufferten.
	 * @param message Det Message-objekt som ska läggas till i bufferten.
	 */
	public void addMessageToBuffer(Message message) {
		bufferedMessages.add(message);
	}

	/**
	 * Returnerar true om det finns buffrade meddelanden.
	 * @return true om det finns buffrade meddelanden, annars false.
	 */
	public boolean hasBufferedMessages() {
		return bufferedMessages.isEmpty();
	}

//	/**
//	 * Returnerar det meddelande som lades till i bufferten först.
//	 * @return Returnerar det meddelande som lades till i bufferten först.
//	 */
//	public Message getNextBufferedMessage() {
//		return bufferedMessages.get(0);
//	}

	/**
	 * Raderar det meddelande som lades till i bufferten först.
	 */
	private void removeNextBufferedMessage() {
		bufferedMessages.remove(0);
	}
	
	public void removeNBufferedMessages(int nbrOfMsgsToRemove) {
		for (int i = 0; i < nbrOfMsgsToRemove; i++) {
			removeNextBufferedMessage();
		}
	}
	
	public Message[] getBufferedMessagesArray() {
		Message[] messageArray;
		synchronized (bufferedMessages) {
			int length = bufferedMessages.size();
			messageArray = new Message[length];
			for (int i = 0; i < length; i++) {
				messageArray[i] = bufferedMessages.get(i);
			}
		}
		return messageArray;
	}
	
	public void addContact(User user) {
		removeContactRequest(user);
		contacts.add(user);
		updateContactList(user);
	}
	
	public void updateContactList(User user) {
		if (clientConnection != null) {
			clientConnection.updateContactList(user);
		}
	}
	
	public LinkedList<User> getContacts() {
		LinkedList<User> contacts = new LinkedList<>();
		synchronized (contacts) {
			for (User u : this.contacts) {
				contacts.add(u);
			}
		}
		return contacts;
	}
	
	public boolean isContactWith(User user) {
		return contacts.contains(user);
	}
	
	public String[][] getContactsArray() {
		String[][] contactsArray;
		synchronized (contacts) {
			int length = contacts.size();
			contactsArray = new String[length][2];
			for (int i = 0; i < length; i++) {
				contactsArray[i][0] = contacts.get(i).getUserName();
				contactsArray[i][1] = (contacts.get(i).isOnline()) ? "true" : "false";
			}
		}
		return contactsArray;
	}
	
	public void addGroup(Group group) {
		groups.add(group);
		updateGroupsList(group);
	}
	
	public boolean removeGroup(Group group) {
		boolean removed = groups.remove(group);
		if (removed) {
			updateGroupsList(group);
		}
		return removed;
	}
	
	private void updateGroupsList(Group group) {
		if (clientConnection != null) {
			clientConnection.updateGroupList(group);
		}
	}

	public String[][] getGroupsArray() {
		String[][] groupsArray;
		synchronized (groups) {
			int length = groups.size();
			groupsArray = new String[length][2];
			Group group;
			for (int i = 0; i < length; i++) {
				group = groups.get(i);
				groupsArray[i][0] = group.getGroupName();
				groupsArray[i][1] = group.getGroupId();
			}
		}
		return groupsArray;
	}

	/**
	 * Returnerar User-objektets användarnamn.
	 * @return User-objektets användarnamn.
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * Returnerar true om angivet lösenord är identiskt med User-objektets lösenord.
	 * @return true om angivet lösenord är identiskt med User-objektets lösenord, annars false.
	 */
	public boolean checkPassword(String password) {
		String protectedPassword = sha256(password);
		return this.protectedPassword.equals(protectedPassword);
	}

	public ClientConnection getClientConnection() {
		return this.clientConnection;
	}
	
	public void setClientConnection(ClientConnection clientConnection) {
		this.clientConnection = clientConnection;
	}
	
	public boolean isOnline() {
		boolean isOnline = false;
		if(this.clientConnection != null) {
			isOnline = clientConnection.isConnected();
		}
		return isOnline;
	}

	public void addContactRequestFrom(User user) {
		if (!contactReqestsFrom.contains(user)) {
			contactReqestsFrom.add(user);
		}
	}
	
	public void addContactRequestTo(User user) {
		if (!contactReqestsTo.contains(user)) {
			contactReqestsTo.add(user);
		}
	}
	
	public void removeContactRequest(User user) {
		contactReqestsFrom.remove(user);
		contactReqestsTo.remove(user);
	}

	public boolean hasRequestedContactWith(User user) {
		return contactReqestsTo.contains(user);
	}
	
	public boolean hasContactRequests() {
		return !contactReqestsFrom.isEmpty();
	}

	public String[] getContactRequestsArray() {
		String[] contactRequests;
		synchronized (contactReqestsFrom) {
			contactRequests = new String[contactReqestsFrom.size()];
			for (int i = 0; i < contactRequests.length; i++) {
				contactRequests[i] = contactReqestsFrom.get(i).getUserName();
			}
		}
		return contactRequests;
	}

	/**
	 * Returnerar User-objektets användarnamn.
	 * @return User-objektets användarnamn.
	 */
	@Override
	public String toString() {
		return getUserName();
	}
	
	private static String sha256(String password) {
	    try{
	        MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
	        StringBuffer hexString = new StringBuffer();

	        for (int i = 0; i < hash.length; i++) {
	            String hex = Integer.toHexString(0xff & hash[i]);
	            if(hex.length() == 1) hexString.append('0');
	            hexString.append(hex);
	        }

	        return hexString.toString();
	    } catch(Exception ex){
	       throw new RuntimeException(ex);
	    }
	}
}
