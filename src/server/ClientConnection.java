package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.LinkedList;

import common.Message;
import common.ResultCode;

public class ClientConnection implements Runnable, UserListener {
	private User user;
	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private ClientsManager clientsManager;
	private ThreadPool threadPool;
	private boolean loggedIn;
	private LogListener logListener;

	public ClientConnection(Socket socket, ClientsManager clientsManager, ThreadPool threadPool, LogListener listener) {
		this.socket = socket;
		this.clientsManager = clientsManager;
		this.threadPool = threadPool;
		this.logListener = listener;
		this.loggedIn = false;
		try {
			socket.setSoTimeout(1000);
			ois = new ObjectInputStream(socket.getInputStream());
			oos = new ObjectOutputStream(socket.getOutputStream());
			threadPool.execute(this);
			logListener.logCommunication("Client established connection from: " + socket.getInetAddress().getHostAddress());
		} catch (IOException e) {
			disconnectClient();
		}
	}
	
	/**
	 * Returnerar den User som klienten loggat in som.
	 * @return Den User som klienten loggat in som.
	 */
	public User getUser() {
		return user;
	}
	
	public boolean isConnected() {
		boolean connected = false;
		if(socket != null) {
			connected = !socket.isClosed();
		}
		return connected;
	}

	private void disconnectClient() {
		if(isConnected()) {
			this.loggedIn = false;
			try {
				oos.writeObject("Disconnect");
			} catch (Exception e1) {}
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {}
			}
			if(user != null) {
				user.setClientConnection(null);
				notifyContacts();
			}
			logListener.logInfo("Client disconnected: " + ((getUser() != null) ? getUser().getUserName() : "null"));
		}
	}

	private void notifyContacts() {
		LinkedList<User> contacts = user.getContacts();
		if (!contacts.isEmpty()) {
			for (User contact : contacts) {
				contact.updateContactList(contact);
			}
		}
	}

	private void receiveMessage(Object obj) throws ClassNotFoundException, IOException {
		Message message;
		if (obj instanceof Message && (message=(Message)obj).getRecipient() != null) {
			message.setServerReceivedTime(Calendar.getInstance().toInstant());
			message.setSender(getUser().getUserName());
			transferMessageToRecipients(message);
		} else {
			logListener.logError("receiveMessage() Received invalid message-obj from " + getUser().getUserName());
		}
	}
	
	private void receiveContactRequest(Object requestObj) {
		String[] newContactRequest;
		if (requestObj instanceof String[]) {
			newContactRequest = (String[])requestObj;
			boolean declineRequest = Boolean.parseBoolean(newContactRequest[1]);
			User requestedUser = clientsManager.getUser(newContactRequest[0]);
			if (requestedUser != null && !user.isContactWith(requestedUser)) {
				if (declineRequest) {
					this.user.removeContactRequest(requestedUser);
					requestedUser.removeContactRequest(this.user);
				} else {
					if (requestedUser.hasRequestedContactWith(getUser())) {
						this.user.addContact(requestedUser);
						requestedUser.addContact(getUser());
					} else {
						this.user.addContactRequestTo(requestedUser);
						requestedUser.addContactRequestFrom(this.user);
						logListener.logInfo("Added contact request from " + this.user.getUserName()  + " to " + requestedUser.getUserName());
						if (requestedUser.isOnline()) {
							requestedUser.getClientConnection().newContactRequest(this.user.getUserName());
						}
					}
				}
			}
		} else {
			logListener.logError("receiveContactRequest() Received invalid contactRequest-obj from " + getUser().getUserName());
		}
	}

	private void receiveNewGroup(Object obj) throws ClassNotFoundException, IOException {
		String[] newGroup;
		if (obj instanceof String[] && (newGroup = (String[])obj).length >= 4) {
			String groupName = newGroup[0];
			String[] memberNames = new String[newGroup.length - 1];
			for (int i = 1; i < newGroup.length; i++) {
				memberNames[i - 1] = newGroup[i];
			}
			String newGroupId = clientsManager.newGroup(user, groupName, memberNames);
			if (newGroupId != null) {
				logListener.logInfo("newGroup() new group by " + user.getUserName() + " created: " + groupName + ", ID: " + newGroupId);
			}
		} else {
			logListener.logError("newGroup() Received invalid newGroup-obj from " + getUser().getUserName());
		}
	}

	private void receiveLeaveGroup(Object groupIdObj) throws IOException {
		
		if(groupIdObj instanceof String){
		 Group group = clientsManager.getGroup((String)groupIdObj);
		
		    if (group != null) {

		        if (group.removeMember(getUser()) && getUser().removeGroup(group)) {

		            logListener.logInfo("receiveLeaveGroup() " + getUser().getUserName() + " removed from group: " + group.getGroupName());

		        }
		    }
		    } else {

		        logListener.logInfo("receiveLeaveGroup() group not found: " + (String)groupIdObj);

		    }

		}

	private LinkedList<User> getRecipients(Message message) {
		LinkedList<User> recipients = new LinkedList<User>();
		String recipient = message.getRecipient();
		if (message.isGroupMessage()) {
			Group group = clientsManager.getGroup(recipient);
			if (group != null && group.isMember(getUser())) {
				recipients = group.getMembers();
				recipients.remove(getUser());
			} else {
				logListener.logError("getRecipients() group not found: " + message.getRecipient());
			}
		} else {
			User user = clientsManager.getUser(recipient);
			if (user != null) {
				recipients.add(user);
			} else {
				logListener.logError("getRecipients() user not found: " + message.getRecipient());
			}
		}
		return recipients;
	}
	
	private void transferMessageToRecipients(Message message) {
		LinkedList<User> recipients = getRecipients(message);
		if (recipients != null && !recipients.isEmpty()) {
			for (User user : recipients) {
				ClientConnection clientConnection = user.getClientConnection();
				if (clientConnection != null) {
					clientConnection.transferMessage(message);
				} else {
					user.addMessageToBuffer(message);
					logListener.logInfo("Buffered message for: " + user.getUserName());
				}
			}
		}
	}

	private void transferMessage(Message message) {
		try {
			oos.writeObject("NewMessage");
			oos.writeObject(message);
			oos.flush();
			logListener.logCommunication("Sent message to: " + message.getRecipient());
		} catch (IOException e) {
			getUser().addMessageToBuffer(message);
			disconnectClient();
		}
	}
	
	/**
	 * Send contactList & groupChats
	 * @throws IOException 
	 */
	private void transferContactList() throws IOException {
		String[][] contacts = getUser().getContactsArray();
		int nbrOfContacts = contacts.length;
		if(nbrOfContacts > 0) {
			oos.writeObject("ContactList");
			oos.writeObject(contacts);
			oos.flush();
			logListener.logInfo("Transfered " + nbrOfContacts + " contacts to: " + getUser().getUserName());
		}
	}
	
	private void transferGroupChats() throws IOException {
		String[][] groups = getUser().getGroupsArray();
		oos.writeObject("GroupChats");
		oos.writeObject(groups);
		oos.flush();
		logListener.logInfo("Transfered groups list to: " + getUser().getUserName());
	}

	private void transferBufferedMessages() throws IOException {
		Message[] bufferedMessages = user.getBufferedMessagesArray();
		int nbrOfMessages = bufferedMessages.length;
		if (nbrOfMessages > 0) {
			oos.writeObject("BufferedMessages");
			oos.writeObject(bufferedMessages);
			oos.flush();
			user.removeNBufferedMessages(nbrOfMessages);
			logListener.logInfo(nbrOfMessages + " buffered messages transferred to: " + user.getUserName());
		}
	}
	
	private void transferContactRequests() throws IOException {
		if (user.hasContactRequests()) {
			String[] contactRequests = user.getContactRequestsArray();
			System.out.println("contactRequests.length == " + contactRequests.length);
			for (String userName : contactRequests) {
				transferContactRequest(userName);
			}
		}
	}

	private void transferContactRequest(String userName) throws IOException {
		oos.writeObject("NewContactRequest");
		oos.writeObject(userName);
	}

	private void transferSearchResults(Object searchObj) throws IOException {
		String searchString;
		if (searchObj instanceof String && (searchString=(String)searchObj).length() >= 2) {
			String[] results = clientsManager.searchUser(searchString, getUser());
			oos.writeObject("SearchResult");
			oos.writeObject(results);
			logListener.logInfo("searchUser() transferred search results to: " + user.getUserName());
		} else {
			logListener.logError("searchUser() Received invalid string-obj from " + getUser().getUserName());
		}
	}

	private boolean hasSpecialCharacters(String string) {
		for (Character c : string.toCharArray()) {
			if (!Character.isLetterOrDigit(c)) {
				return true;
			}
		}
		return false;
	}

	private boolean login(Object credentialsObj) {
		boolean success = false;
		try {
			if (credentialsObj instanceof String[]) {
				String[] credentials;
				credentials = (String[])credentialsObj;
				String userName, password;
				userName = credentials[0];
				password = credentials[1];
				User user = clientsManager.getUser(userName);
				if (user != null && user.checkPassword(password) == true) {
					oos.writeBoolean(true);
					oos.flush();
					this.user = user;
					ClientConnection clientConnection = user.getClientConnection();
					if (clientConnection != null) {
						clientConnection.disconnectClient();
					}
					user.setClientConnection(this);
					success = true;
				} else {
					logListener.logError("Client login failed: wrong userName or password.");
					oos.writeBoolean(false);
					oos.flush();
				}
			} else {
				logListener.logError("login() Received non-string[] credentials from client.");
			}
		} catch (IOException e) {
			logListener.logError("Client login timeout");
		}
		return success;
	}
	
	private void register(Object credentialsObj) {
		try {
			if (credentialsObj instanceof String[]) {
				int result = ResultCode.ok;
				String[] credentials = (String[])credentialsObj;
				String userName = credentials[0];
				String password = credentials[1];
				boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && 
						!hasSpecialCharacters(userName);
				boolean passwordOk = password.length() > 0 && password.length() <= 20;
				if (userNameOk && passwordOk) {
					User newUser = new User(userName, password);
					result = clientsManager.addUser(newUser);
					if (result == ResultCode.ok) {
						logListener.logInfo("User registered: " + userName);
					} else if (result == ResultCode.userNameAlreadyTaken) {
						logListener.logInfo("User name already taken: " + userName);
					}
				} else {
					if (!userNameOk && !passwordOk) {
						logListener.logError("Registration failed: Username and password have wrong format.");
						result = ResultCode.wrongCredentials;
					} else if (!userNameOk) {
						logListener.logError("Registration failed: Username has wrong format.");
						result = ResultCode.wrongUsernameFormat;
					} else if (!passwordOk) {
						logListener.logError("Registration failed: Password has wrong format.");
						result = ResultCode.wrongPasswordFormat;
					}
				}
				
				oos.writeInt(result);
				oos.flush();
			} else {
				logListener.logError("register() Received non-string[] credentials from client.");
			}
		} catch (IOException e) {
			logListener.logError("Client registration timeout");
		}
	}
	
	@Override
	public void updateContactList(User contact) {
		if(isConnected()) {
			try {
				transferContactList();
			} catch (IOException e) {
				disconnectClient();
			}
		}
	}

	@Override
	public void updateGroupList(Group group) {
		if(isConnected()) {
			try {
				transferGroupChats();
			} catch (IOException e) {
				disconnectClient();
			}
		}
	}

	@Override
	public void newContactRequest(String userName) {
		try {
			transferContactRequest(userName);
		} catch (IOException e) {
			disconnectClient();
		}
	}

	@Override
	public void run() {
		try {
			if (loggedIn == false) {
				String request = "";
				Object obj;
				socket.setSoTimeout(50);
				obj = ois.readObject();
				if (obj instanceof String) {
					request = (String)obj;
					logListener.logInfo("Received request(" + request + ") from: " + socket.getInetAddress().getHostAddress());
					socket.setSoTimeout(1500);
					switch (request) {
					case "Login":
						loggedIn = login(ois.readObject());
						if (loggedIn == true) {
							logListener.logInfo("User logged in: " + getUser().getUserName());
							transferContactList();
							transferGroupChats();
							transferBufferedMessages();
							transferContactRequests();
							notifyContacts();
						} else {
							logListener.logError("Failed login from " + socket.getInetAddress().getHostAddress());
							disconnectClient();
						}
						break;
					case "Register":
						register(ois.readObject());
						break;
					case "Disconnect":
						disconnectClient();
						break;
					default:
						logListener.logError("Unknown request");
					}
				} else {
					logListener.logError("Received non-string request from " + socket.getInetAddress().getHostAddress());
				}
			}
			if (loggedIn == true) {
				String request = "";
				Object obj;
				socket.setSoTimeout(50);
				while (!Thread.interrupted()) {
					obj = ois.readObject();
					if (obj instanceof String) {
						request = (String)obj;
						logListener.logInfo("Received request(" + request + ") from " + getUser().getUserName());
						socket.setSoTimeout(1500);
						switch (request) {
						case "NewMessage":
							receiveMessage(ois.readObject());
							break;
						case "NewContactRequest":
							receiveContactRequest(ois.readObject());
							break;
						case "SearchUser":
							transferSearchResults(ois.readObject());
							break;
						case "NewGroup":
							receiveNewGroup(ois.readObject());
							break;
						case "LeaveGroup":
							receiveLeaveGroup(ois.readObject());
							break;
						case "Disconnect":
							disconnectClient();
							break;
						default:
							logListener.logError("Unknown request");
						}
					} else {
						logListener.logError("Received non-string request from " + getUser().getUserName());
					}
				}
			}
		} catch (SocketTimeoutException e) {
			threadPool.execute(this);
		} catch (ClassNotFoundException e) {
			logListener.logError("Server: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			disconnectClient();
		}
	}
}
