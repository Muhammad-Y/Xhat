package client;

import client.gui.LogInController;

public class InitClient {

	public void dooit(String name , String pass) {
		Data data = new Data();

		//3.132.184.132
		//use this instance if you want to connect to the server on the Virtual Machine. If the VM is off use localhost
		// ClientCommunications clientCommunications = new ClientCommunications("3.132.184.132", 5555, data);

		//use this instance if you want to connect to localhost.Do not forget to start the server first on your computer (InitServer)
		ClientCommunications clientCommunications = new ClientCommunications("localhost", 5555, data);

		LogInController logInController = new LogInController(clientCommunications, name, pass);

	}


	public static void main(String[] args) {
		Data data = new Data();

		//3.132.184.132
		//use this instance if you want to connect to the server on the Virtual Machine. If the VM is off use localhost
		// ClientCommunications clientCommunications = new ClientCommunications("3.132.184.132", 5555, data);

		//use this instance if you want to connect to localhost.Do not forget to start the server first on your computer (InitServer)
		ClientCommunications clientCommunications = new ClientCommunications("localhost", 5555, data);

		LogInController logInController = new LogInController(clientCommunications, "Test1", "password");

	}

}
