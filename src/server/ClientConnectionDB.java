package server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;

import common.Encryption;
import common.Message;
import common.ResultCode;
import org.apache.commons.io.FileUtils;
import server.database.DBHandler;

public class ClientConnectionDB implements Runnable, UserListener {
    private User user;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ClientsManager clientsManager;
    private DBHandler dbh;
    private ThreadPool threadPool;
    private boolean loggedIn;
    private LogListener logListener;

    public ClientConnectionDB(Socket socket, ClientsManager clientsManager, DBHandler dbh, ThreadPool threadPool, LogListener listener) {
        this.socket = socket;
        this.clientsManager = clientsManager;
        this.dbh = dbh;
        this.threadPool = threadPool;
        this.logListener = listener;
        this.loggedIn = false;
        try {
            socket.setSoTimeout(10000);
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
     *
     * @return Den User som klienten loggat in som.
     */
    public User getUser() {
        return user;
    }

    public DBHandler getDBH() {return dbh;}

    public boolean isConnected() {
        boolean connected = false;
        if (socket != null) {
            connected = !socket.isClosed();
        }
        return connected;
    }

    private void disconnectClient() {
        if (isConnected()) {
            this.loggedIn = false;
            try {
                dbh.updateOnlineStatus(user.toString());
                user.setClientConnection(null);
                notifyContacts();
                oos.writeObject("Disconnect");
            } catch (Exception e1) {
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                }
            }
            logListener.logInfo("Client disconnected: " + ((user != null) ? user.toString() : "null"));
        }
    }

    private void notifyContacts() {
        try {
            dbh.open();
            ResultSet rs = dbh.getContacts(user.toString());
            while (rs.next()) {
                if (rs.getString(2) != null)
                    ServerConnection.getClientThread(rs.getString(1)).transferContactList();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        dbh.close();
    }

    private void receiveMessage(Object obj) throws ClassNotFoundException, IOException {
        Message message;
        if (obj instanceof Message && (message = (Message) obj).getRecipient() != null) {
            message.setServerReceivedTime(Calendar.getInstance().toInstant());
            message.setSender(user.toString());
            transferMessageToRecipients(message);
        } else {
            logListener.logError("receiveMessage() Received invalid message-obj from " + user.toString());
        }
    }

    private void receiveContactRequest(Object requestObj) {
        String[] newContactRequest;
        if (requestObj instanceof String[]) {
            newContactRequest = (String[]) requestObj;
            boolean declineRequest = Boolean.parseBoolean(newContactRequest[1]);
            String requestedUser = newContactRequest[0];
            dbh.open();
            try {
                if (dbh.checkUsername(requestedUser).next()) {
                    if (declineRequest) {
                        dbh.removeContactRequest(requestedUser, user.toString());
                    } else {
                        if (dbh.getPendingContactRequest(user.toString(), requestedUser).next()) {
                            dbh.addContact(user.toString(), requestedUser);
                            dbh.removeContactRequest(user.toString(), requestedUser);
                            transferContactList();
                            ClientConnectionDB cc = ServerConnection.getClientThread(requestedUser);
                            if (cc != null) {
                                cc.transferContactList();
                            }
                        } else {
                            dbh.addContactRequest(requestedUser, user.toString());
                            logListener.logInfo("Added contact request from " + user.toString() + " to " + requestedUser);
                            ClientConnectionDB cc = ServerConnection.getClientThread(requestedUser);
                            if (cc != null) {
                                cc.transferContactRequest(user.toString());
                            }
                        }
                    }
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
            dbh.close();
        } else {
            logListener.logError("receiveContactRequest() Received invalid contactRequest-obj from " + user.toString());
        }
    }

    /**
     * Uppdaterar INTE kontaktlistorna i nuläget
     * @param contactObj
     */
    private void receiveRemoveContact(Object contactObj) {
        if (contactObj instanceof String) {
            dbh.open();
            try {
                dbh.removeContact(user.toString(), (String)contactObj);
                dbh.removeContact((String)contactObj, user.toString());
                logListener.logInfo("receiveRemoveContact() " + user.toString() + " removed " + (String)contactObj + " from contacts");
                transferContactList();
                ClientConnectionDB cc = ServerConnection.getClientThread((String)contactObj);
                if (cc != null) {
                    cc.transferContactList();
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
            dbh.close();

        } else {
            logListener.logInfo("receiveRemoveContact() contact not found: " + (String)contactObj);
        }
    }

    private void receiveNewGroup(Object obj) throws IOException, NoSuchAlgorithmException {
        String[] newGroup = (String[]) obj;
        if (obj instanceof String[]) {// && (newGroup = (String[]) obj).length >= 4) {
            String groupName = newGroup[0];
            String[] memberNames = new String[newGroup.length - 1];
            for (int i = 1; i < newGroup.length; i++) memberNames[i - 1] = newGroup[i];
          /*  dbh.addGroup(groupName, memberNames);
            Group group = clientsManager.newGroup(user, groupName, memberNames);
            String newGroupId = group.getGroupId();
            if (newGroupId != null) {
                logListener.logInfo("newGroup() new group by " + user.getUserName() + " created: " + groupName + ", ID: " + newGroupId);
                transferEncryptionKeyToRecipients(group);
            }
            */
        }
        else logListener.logError("newGroup() Received invalid newGroup-obj from " + user.toString());
    }

    private void receiveLeaveGroup(Object groupIdObj) throws IOException {
        if (groupIdObj instanceof String) {
            Group group = clientsManager.getGroup((String) groupIdObj);
            if (group != null) {
                if (group.removeMember(user) && user.removeGroup(group)) {
                    logListener.logInfo("receiveLeaveGroup() " + user.toString() + " removed from group: " + group.getGroupName());
                }
            }
        } else {
            logListener.logInfo("receiveLeaveGroup() group not found: " + (String) groupIdObj);
        }
    }

    private LinkedList<User> getRecipients(Message message) {
        LinkedList<User> recipients = new LinkedList<User>();
        String recipient = message.getRecipient();
        if (message.isGroupMessage()) {
            Group group = clientsManager.getGroup(recipient);
            if (group != null && group.isMember(user)) {
                recipients = group.getMembers();
                recipients.remove(user);
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
                ClientConnectionDB clientConnection = ServerConnection.getClientThread(user.toString());
                if (clientConnection != null) {
                    clientConnection.transferMessage(message);
                } else {
                    user.addMessageToBuffer(message);
                    logListener.logInfo("Buffered message for: " + user.toString());
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
            user.addMessageToBuffer(message);
            disconnectClient();
        }
    }

    private void transferEncryptionKeyToRecipients(Group group) throws IOException, NoSuchAlgorithmException {
        KeyPair keyPair = Encryption.doGenkey(group.getGroupId());
        for (User user : group.getMembers()) {
            ArrayList<Object> list = new ArrayList<>();
            list.add(group.getGroupId());
            list.add(keyPair.getPrivate().getEncoded());
            ClientConnectionDB clientConnection = user.getClientConnection();
            clientConnection.transferEncryptionKey(list, 1);
        }
    }

    /**
     * Send contactList & groupChats
     *
     * @throws IOException
     */
    private void transferContactList() throws IOException {
        dbh.open();
        try {
            String[][] contacts = dbh.getContactsArray(user.toString());
            System.out.println(Arrays.toString(contacts));
            int nbrOfContacts = contacts.length;
            oos.writeObject("ContactList");
            oos.writeObject(contacts);
            oos.flush();
            logListener.logInfo("Transfered " + nbrOfContacts + " contacts to: " + user.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void transferGroupChats() throws IOException {
        dbh.open();
        try {
            String[][] groups = dbh.getGroupsArray(user.toString());
            oos.writeObject("GroupChats");
            oos.writeObject(groups);
            oos.flush();
            logListener.logInfo("Transfered groups list to: " + user.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbh.close();
    }

    private void transferBufferedMessages() throws IOException {
        Message[] bufferedMessages = user.getBufferedMessagesArray();
        int nbrOfMessages = bufferedMessages.length;
        if (nbrOfMessages > 0) {
            oos.writeObject("BufferedMessages");
            oos.writeObject(bufferedMessages);
            oos.flush();
            user.removeNBufferedMessages(nbrOfMessages);
            logListener.logInfo(nbrOfMessages + " buffered messages transferred to: " + user.toString());
        }
    }

    private void transferContactRequests() throws IOException {
        String[] contactRequests = null;
        dbh.open();
        try {
            contactRequests = dbh.getContactRequestsArray(user.toString());
        } catch (SQLException e) { e.printStackTrace(); }
        dbh.close();
        if (contactRequests != null) {
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
        if (searchObj instanceof String && (searchString = (String) searchObj).length() >= 2) {
            String[] results = dbh.searchUser(searchString, user);
            oos.writeObject("SearchResult");
            oos.writeObject(results);
            logListener.logInfo("searchUser() transferred search results to: " + user.toString());
        } else {
            logListener.logError("searchUser() Received invalid string-obj from " + user.toString());
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
                credentials = (String[]) credentialsObj;
                String username, password;
                username = credentials[0];
                password = credentials[1];
                if (dbh.verifyLogin(username, password)) {
                    oos.writeBoolean(true);
                    oos.flush();
                    user = clientsManager.getUser(username);
                    if(user == null){
                        user = new User(username, password);
                        clientsManager.addUser(user);
                    }
                    ClientConnectionDB clientConnection = ServerConnection.getClientThread(user.toString());
                    if (clientConnection != null) clientConnection.disconnectClient();
                    ServerConnection.setThreadName(user.toString(), this);
                    user.setClientConnection(ServerConnection.getClientThread(user.toString()));
                    success = true;
                }
                else {
                    logListener.logError("Client login failed: wrong userName or password.");
                    oos.writeBoolean(false);
                    oos.flush();
                }
            }
            else logListener.logError("login() Received non-string[] credentials from client.");
        } catch (IOException e) {
            logListener.logError("Client login timeout");
        }
        return success;
    }

    private void register(Object credentialsObj) {
        try {
            if (credentialsObj instanceof String[]) {
                int result = ResultCode.ok;
                String[] credentials = (String[]) credentialsObj;
                String userName = credentials[0];
                String password = credentials[1];
                boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
                boolean passwordOk = password.length() > 0 && password.length() <= 20;
                if (userNameOk && passwordOk) {
                    User newUser = new User(userName, password);
                    dbh.open();
                    try {
                        result = dbh.registerNewUser(newUser);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    dbh.close();
                    if (result == ResultCode.ok) {
                        clientsManager.addUser(newUser);
                        logListener.logInfo("User registered: " + userName);
                    }
                    else if (result == ResultCode.userNameAlreadyTaken) logListener.logInfo("User name already taken: " + userName);
                }
                else {
                    if (!userNameOk && !passwordOk) {
                        logListener.logError("Registration failed: Username and password have wrong format.");
                        result = ResultCode.wrongCredentials;
                    }
                    else if (!userNameOk) {
                        logListener.logError("Registration failed: Username has wrong format.");
                        result = ResultCode.wrongUsernameFormat;
                    }
                    else {
                        logListener.logError("Registration failed: Password has wrong format.");
                        result = ResultCode.wrongPasswordFormat;
                    }
                }
                oos.writeInt(result);
                oos.flush();
                if (result == ResultCode.ok) {
                    KeyPair keyPair = Encryption.doGenkey(userName);
                    transferEncryptionKey(keyPair.getPrivate().getEncoded(), 0);
                }
            }
            else logListener.logError("register() Received non-string[] credentials from client.");
        } catch (IOException | NoSuchAlgorithmException e) {
            logListener.logError("Client registration timeout");
        }
    }

    private void transferEncryptionKey(Object o, int type) {
        try {
            if (type == 0) {
                oos.writeObject(o);
                logListener.logCommunication("Sent encryption key " + o);
            }
            if (type == 1) {
                oos.writeObject("EncryptionKey");
                oos.writeObject(o);
                oos.writeObject(FileUtils.readFileToByteArray(new File("data/" + o + ".pub")));
                logListener.logCommunication("Sent encryption key for " + o);
            }
            oos.flush();
        } catch (IOException e) {
            disconnectClient();
            e.printStackTrace();
        }
    }

    @Override
    public void updateContactList(User contact) {
        if (isConnected()) {
            try {
                transferContactList();
            } catch (IOException e) {
                disconnectClient();
            }
        }
    }

    @Override
    public void updateGroupList(Group group) {
        if (isConnected()) {
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
            if (!loggedIn) {
                String request = "";
                Object obj;
                socket.setSoTimeout(50);
                obj = ois.readObject();
                if (obj instanceof String) {
                    request = (String) obj;
                    logListener.logInfo("Received request(" + request + ") from: " + socket.getInetAddress().getHostAddress());
                    socket.setSoTimeout(1500);
                    switch (request) {
                        case "Login":
                            loggedIn = login(ois.readObject());
                            if (loggedIn) {
                                logListener.logInfo("User logged in: " + user.toString());
                                dbh.updateOnlineStatus(user.toString());
                                transferContactList();
                                //transferGroupChats();
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
            if (loggedIn) {
                String request = "";
                Object obj;
                socket.setSoTimeout(50);
                while (!Thread.interrupted()) {
                    obj = ois.readObject();
                    if (obj instanceof String) {
                        request = (String) obj;
                        logListener.logInfo("Received request(" + request + ") from " + user.toString());
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
                            case "RemoveContact":
                                receiveRemoveContact(ois.readObject());
                                break;
                            case "UserKey":
                                transferEncryptionKey(ois.readObject(), 1);
                                break;
                            default:
                                logListener.logError("Unknown request");
                        }
                    } else {
                        logListener.logError("Received non-string request from " + user.toString());
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            threadPool.execute(this);
        } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
            logListener.logError("Server: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            disconnectClient();
        }
    }
}

