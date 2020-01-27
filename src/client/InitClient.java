package client;

import client.gui.LogInController;

public class InitClient {

	public static void main(String[] args) {
		Data data = new Data();
		ClientCommunications clientCommunications = new ClientCommunications("127.0.0.1", 5555, data);
		LogInController logInController = new LogInController(clientCommunications, "Test1", "password");

//test222
	}

}
