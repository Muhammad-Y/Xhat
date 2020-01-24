package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import client.gui.MainController;
import common.Message;
import common.ResultCode;

public class ClientCommunications implements Runnable {
	private Thread thread;
	private String ip;
	private int port;
	private String userName;
	private Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private boolean loggedIn;
	private MainController mainController;
	private Data data;

	public ClientCommunications(String ip, int port, Data data) {
		this.ip = ip;
		this.port = port;
		this.data = data;
	}
	
	public boolean sendMessage(Message message) {
		boolean success = false;
		try {
			oos.writeObject("NewMessage");
			oos.writeObject(message);
			oos.flush();
			success = true;
		} catch (IOException e) {
			success = false;
			disconnect();
		}
		return success;
	}
	
	public void sendNewContactRequest(String[] requestObj) {
		try {
			oos.writeObject("NewContactRequest");
			oos.writeObject(requestObj);
			oos.flush();
		} catch (IOException e) {
			disconnect();
		}
	}
	
	public void searchUser(String searchString) {
		try {
			oos.writeObject("SearchUser");
			oos.writeObject(searchString);
			oos.flush();
		} catch (IOException e) {
			disconnect();
		}
	}

	public void createNewGroup(String[] newGroup) {
		try {
			oos.writeObject("NewGroup");
			oos.writeObject(newGroup);
			oos.flush();
		} catch (IOException e) {
			disconnect();
		}
	}
	
	public void leaveGroup(String groupId) {
		try {

	        oos.writeObject("LeaveGroup");

	        oos.writeObject(groupId);

	        oos.flush();

	    } catch (IOException e) {

	        disconnect();

	    }
	}
	
	public String getUserName() {
		return this.userName;
	}

	public boolean isConnected() {
		boolean connected = false;
		if(socket != null) {
			connected = !socket.isClosed();
		}
		return connected;
	}
	
	public boolean login(String userName, String password) {
		//TODO: Dont let UI-thread from LoginController execute this code
		try {
			if (!this.loggedIn) {
				if(!isConnected()) {
					establishConnection();
				}
				oos.writeObject("Login");
				oos.writeObject(new String[] { userName, password });
				oos.flush();
				this.loggedIn = ois.readBoolean();
				if (this.loggedIn == true) {
					this.userName = userName;
					this.mainController = new MainController(this, data);
					ClientLogger.logInfo("login() Succesfully logged in as: " + userName);
					startListener();
				} else {
					ClientLogger.logError("Wrong username or password.");
					disconnect();
				}
			} else {
				ClientLogger.logError("login() This client is already logged in as: " + getUserName());
			}
		} catch (IOException e) {
			ClientLogger.logError("Login failed(IOException): " + e.getMessage());
			disconnect();
		}
		return loggedIn;
	}

	public int register(String userName, String password) {
		int result = ResultCode.ok;
		if (!this.loggedIn) {
			try {
				if (!isConnected()) {
					establishConnection();
				}
				oos.writeObject("Register");
				oos.writeObject(new String[] { userName, password });
				oos.flush();
				result = ois.readInt();
			} catch (IOException e) {
				ClientLogger.logError("Registration failed(IOException): " + e.getMessage());
			}
		}
		disconnect();
		
		return result;
	}
	
	public void disconnect() {
		disconnect("Disconnected from server");
	}

	public void disconnect(String message) {
		if (isConnected()) {
			stopListener();
			try {
				oos.writeObject("Disconnect");
			} catch (IOException e1) {}
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {}
			}
			if (loggedIn == true) {
				mainController.disconnected(message);
			}
			this.loggedIn = false;
			this.data = new Data();
			ClientLogger.logInfo(message);
		}
	}
	
	private void establishConnection() throws SocketException, IOException {
		socket = new Socket(ip, port);
		socket.setSoTimeout(1000);
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
	}

	private void receiveContactList(Object contactsObj) throws ClassNotFoundException, IOException {
		if (contactsObj instanceof String[][]) {
			String[][] contacts = (String[][])contactsObj;
			for (int i = 0; i < contacts.length; i++) {
				String contactName = contacts[i][0];
				boolean isOnline = Boolean.parseBoolean(contacts[i][1]);
				Contact contact = data.getContact(contactName);
				if (contact == null) {
					contact = new Contact(contactName);
					data.addContact(contact);
				}
				contact.setIsOnline(isOnline);
			}
			mainController.updateContactsList();
			ClientLogger.logInfo("Received and processed new contactList");
		} else {
			ClientLogger.logError("receiveContactList(): Received non-String[] object.");
		}
	}

	private void receiveGroupChats(Object groupsObj) {
		if (groupsObj instanceof String[][]) {
			String[][] groups = (String[][])groupsObj;
			data.clearGroups();
			for (int i = 0; i < groups.length; i++) {
				data.addGroup(groups[i][0], groups[i][1]); //TODO Om gruppen redan existerar?
			}
			mainController.updateGroupList();
			ClientLogger.logInfo("Received and processed groupChats");
		} else {
			ClientLogger.logError("receiveGroupChats(): Received non-String[][] object.");
		}
	}

	private void receiveBufferedMessages(Object bufferedMsgsObj) {
		if (bufferedMsgsObj instanceof Message[]) {
			Message[] messages = (Message[])bufferedMsgsObj;
			int nbrOfMessages = messages.length;
			ClientLogger.logInfo("Received buffered messages.");
			for (int i = 0; i < nbrOfMessages; i++) {
				receiveMessage(messages[i]);
			}
			ClientLogger.logInfo("Processed " + nbrOfMessages + " buffered messages.");
		} else {
			ClientLogger.logError("receiveGroupChats(): Received non-Message[] object.");
		}
	}

	private void receiveMessage(Object obj) {
		if(obj instanceof Message) {
			Message message = (Message)obj;
			Contact senderContact = null;
			boolean isGroupMsg = message.isGroupMessage();
			if (isGroupMsg == true) {
				senderContact = data.getGroup(message.getRecipient());
			} else {
				senderContact = data.getContact(message.getSender());
			}
			if(senderContact != null) {
				String senderName = (isGroupMsg == true) ? ((GroupChat)senderContact).getGroupId() : senderContact.getName();
				ClientLogger.logInfo("Recevied message from: " + senderName);
				if(mainController.isContactSelected(senderName)) {
					mainController.addMessageToConversation(senderContact, message, false);
				} else {
					senderContact.addUnreadMessage(message);
					mainController.notifyNewMessage(senderName, message.isGroupMessage());
				}
			} else {
				ClientLogger.logError("receiveMessage(): Sender not in contact/group list: " + message.getSender() + "/" + message.getRecipient());
			}
		} else {
			ClientLogger.logError("receiveMessage(): Received non-Message object.");
		}
	}
	
	private void receiveSearchResults(Object resultsObj) {
		if (resultsObj instanceof String[]) {
			String[] results = (String[])resultsObj;
			mainController.updateSearchResults(results);
			ClientLogger.logInfo("Received searchResults.");
		} else {
			ClientLogger.logError("receiveSearchResults(): Received non-String[] object.");
		}
	}
	
	private void receiveContactRequest(Object requestObj) {
		if (requestObj instanceof String) {
			String requestFromUserName = (String)requestObj;
			ClientLogger.logInfo("Received new contact request from: " + requestFromUserName);
			mainController.notifyNewContactRequest(requestFromUserName);
		} else {
			ClientLogger.logError("receiveContactRequest(): Received non-String object.");
		}
	}

	private void startListener() {
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		} else {
			ClientLogger.logError("Error: startListener(): Listener thread already running.");
		}
	}
	
	private void stopListener() {
		if(thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	@Override
	public void run() {
		if (isConnected() == true) {
			ClientLogger.logInfo("Listener started...");
			String request = "";
			Object obj;
			try {
				while (!Thread.interrupted()) {
					socket.setSoTimeout(0);
					obj = ois.readObject();
					if (obj instanceof String) {
						request = (String) obj;
						ClientLogger.logInfo("Received request(" + request + ")");
						socket.setSoTimeout(500);
						switch (request) {
						case "NewMessage":
							receiveMessage(ois.readObject());
							break;
						case "ContactList":
							receiveContactList(ois.readObject());
							break;
						case "GroupChats":
							 receiveGroupChats(ois.readObject());
							break;
						case "BufferedMessages":
							 receiveBufferedMessages(ois.readObject());
							break;
						case "SearchResult":
							receiveSearchResults(ois.readObject());
							break;
						case "NewContactRequest":
							receiveContactRequest(ois.readObject());
							break;
						case "Disconnect":
							disconnect();
							break;
						default:
							ClientLogger.logError("Unknown request");
						}
					} else {
						ClientLogger.logError("Received non-string request from server");
					}
				}
			} catch (SocketTimeoutException e) {
				System.out.println("Socket timeout bruh");
				this.thread = null;
				startListener();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				 disconnect();
			}
		} else {
			ClientLogger.logError("Listener stopped: Client not connected or logged in.");
		}
	}
}
