package client;

import client.gui.LogInController;

public class InitClient {

	public static void main(String[] args) {
		Data data = new Data();

		//3.132.184.132
		//use this instance if you want to connect to the server on the Virtual Machine. If the VM is off use localhost
		//ClientCommunications clientCommunications = new ClientCommunications("3.132.184.132", 5555, data);


		//use this instance if you want to connect to localhost.Do not forget to start the server first on your computer (InitServer)
		ClientCommunications clientCommunications = new ClientCommunications("10.2.8.166", 5555, data);

		LogInController logInController = new LogInController(clientCommunications, "Test1", "password");

	}

}
