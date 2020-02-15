package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServerConnection implements Runnable {
	private int listeningPort;
	private ClientsManager clientsManager;
	private static Map<String, ClientConnectionDB> clientThreads = Collections.synchronizedMap(new HashMap<>());
	private DBHandler dbh = new DBHandler();
	private ThreadPool threadPool;
	private LogListener logListener;

	public ServerConnection(int listeningPort, ClientsManager clientsManager, ThreadPool threadPool) {
		this.listeningPort = listeningPort;
		this.clientsManager = clientsManager;
		this.threadPool = threadPool;
	}
	
	public void addListener(LogListener listener) {
		this.logListener = listener;
	}
	
	public void start() {
		this.threadPool.execute(this);
	}
	
	public void shutdownServer() {
		clientsManager.saveData();
		System.exit(0);
	}

	public static void setThreadName(String username, ClientConnectionDB con) {
		clientThreads.put(username, con);
	}

	public static ClientConnectionDB getClientThread(String username) {
		return clientThreads.get(username);
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {
			logListener.logInfo("Server listening on: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());
//			clientsManager.loadData();
//			clientsManager.addTestData();
			while (!Thread.interrupted()) {
				Socket socket = serverSocket.accept();
				//new ClientConnection(socket, clientsManager, threadPool, logListener);
				new ClientConnectionDB(socket, clientsManager, dbh, threadPool, logListener);
			}
		} catch (IOException e) {
			logListener.logError("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
