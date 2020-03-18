package server;

public class InitServer {

	public static void main(String[] args) {
		ThreadPool threadPool = new ThreadPool(30);
		threadPool.start();

//		ServerLogger serverLogger = new ServerLogger();
		ClientsManager clientsManager = new ClientsManager(threadPool);
		ServerConnection serverConnection = new ServerConnection(2020, clientsManager, threadPool);
		new ServerController(serverConnection);
	}

}
