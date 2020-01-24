package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConnection implements Runnable {
	private int listeningPort;
	private ClientsManager clientsManager;
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

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(listeningPort)) {
			logListener.logInfo("Server listening on: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());
			clientsManager.loadData();
//			clientsManager.addTestData();
			while (!Thread.interrupted()) {
				Socket socket = serverSocket.accept();
				ClientConnection connectingClient = new ClientConnection(socket, clientsManager, threadPool, logListener);
			}
		} catch (IOException e) {
			logListener.logError("Server error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
