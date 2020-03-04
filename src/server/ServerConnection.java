package server;

import org.postgresql.core.SqlCommand;
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

	public ServerConnection(int listeningPort, ClientsManager clientsManager, ThreadPool threadPool) {
		this.listeningPort = listeningPort;
		this.clientsManager = clientsManager;
		this.threadPool = threadPool;
		this.dbh = new DBHandler();
	}

	public void addListener(LogListener listener) {
		this.logListener = listener;
		dbh.addListener(listener);
	}

	public void start() {
		this.threadPool.execute(this);
	}
	
	public void shutdownServer() {
		try {
			StorageHandler.writeToFile(clientsManager.users, "users.dat");
			StorageHandler.writeToFile(clientsManager.groups, "groups.dat");
		} catch (IOException e) { e.printStackTrace(); }
		System.exit(0);
	}

	public static void setThreadName(String username, ClientConnectionDB con) {
		clientThreads.put(username, con);
	}

	public static ClientConnectionDB getClientThread(String username) {
		return clientThreads.get(username);
	}

	public static void deleteClientThread(String username) {
		clientThreads.remove(username);
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {
			logListener.logInfo("Server listening on: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());
			dbh  = new DBHandler();
	/*		dbh.open();
			dbh.resetOnlineStatus();
			dbh.close();*/
			while (!Thread.interrupted()) {
				Socket socket = serverSocket.accept();
				new ClientConnectionDB(socket, clientsManager, dbh, threadPool, logListener);
			}
		} catch (IOException e) {
			logListener.logError("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
