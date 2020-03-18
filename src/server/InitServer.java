package server;

import client.InitClient;

public class InitServer {

	public static void main(String[] args) {
		ThreadPool threadPool = new ThreadPool(30);
		threadPool.start();

//		ServerLogger serverLogger = new ServerLogger();
		ClientsManager clientsManager = new ClientsManager(threadPool);
		ServerConnection serverConnection = new ServerConnection(2020, clientsManager, threadPool);
		new ServerController(serverConnection);

	//	InitClient c1 = new InitClient();
		//c1.dooit("test1212","1212");
	//	InitClient c2 = new InitClient();
	//	c1.dooit("test1313","1313");

	//	InitClient c3 = new InitClient();
	//	c1.dooit("test1414","1414");

	}

}
