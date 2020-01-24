package server;

public class InitServer {

	public static void main(String[] args) {
		ThreadPool threadPool = new ThreadPool(30);
		threadPool.start();
//		ServerLogger serverLogger = new ServerLogger();
		ClientsManager clientsManager = new ClientsManager(threadPool);
		ServerConnection serverConnection = new ServerConnection(5555, clientsManager, threadPool);
		ServerController serverController = new ServerController(serverConnection);
	}

}
