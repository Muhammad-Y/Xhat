package server;

import java.io.IOException;

public class InitServer {

	public static void main(String[] args) {
		ThreadPool threadPool = new ThreadPool(30);
		threadPool.start();

//		ServerLogger serverLogger = new ServerLogger();
		ClientsManager clientsManager = new ClientsManager(threadPool);
		try {
			clientsManager.setUsers(StorageHandler.loadUsersFromFile());
			clientsManager.setGroups(StorageHandler.loadGroupsFromFile());
		} catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }

		ServerConnection serverConnection = new ServerConnection(5555, clientsManager, threadPool);
		new ServerController(serverConnection);
	}

}
