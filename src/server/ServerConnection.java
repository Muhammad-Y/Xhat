package server;

import server.database.DBHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerConnection implements Runnable {
	private int listeningPort;
	private ClientsManager clientsManager;
	private static Map<String, ClientConnectionDB> clientThreads = Collections.synchronizedMap(new HashMap<>());
	private DBHandler dbh;
	private ThreadPool threadPool;
	private LogListener logListener;
	private String user, password;

	public ServerConnection(int listeningPort, ClientsManager clientsManager, ThreadPool threadPool,
							String dbUser, String dbPassword) {
		this.listeningPort = listeningPort;
		this.clientsManager = clientsManager;
		this.threadPool = threadPool;
		user = dbUser;
		password = dbPassword;
	}
	
	public void addListener(LogListener listener) {
		this.logListener = listener;
	}
	
	public void start() {
		this.threadPool.execute(this);
	}
	
	public void shutdownServer() {
		System.exit(0);
	}

	public static void setThreadName(String username, ClientConnectionDB con) {
		clientThreads.put(username, con);
	}

	public static ClientConnectionDB getClientThread(String username) {
		return clientThreads.get(username);
	}

	public static void closeClientThread(String username) {
		clientThreads.remove(username);
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {
			logListener.logInfo("Server listening on: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());
			dbh  = new DBHandler(logListener, user, password);
			dbh.open();
			dbh.resetOnlineStatus();
			dbh.close();
			while (!Thread.interrupted()) {
				Socket socket = serverSocket.accept();
				//new ClientConnection(socket, clientsManager, threadPool, logListener);
				new ClientConnectionDB(socket, clientsManager, dbh, threadPool, logListener);
			}
		} catch (IOException | SQLException e) {
			logListener.logError("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
