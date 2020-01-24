package server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public final class StorageHandler {
	
	public static void writeToFile(Object obj, String fileName) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("data/" + fileName))) {
			oos.writeObject(obj);
			oos.flush();
			oos.close();
		}
	}

	public static Map<String,User> loadUsersFromFile() throws FileNotFoundException, IOException, ClassNotFoundException {
		Map<String,User> users = null;
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("data/users.dat"))) {
			Object obj = ois.readObject();
			if (obj instanceof Map<?,?>) {
				users = (Map<String,User>) obj;
			} else {
				throw new IOException("users.dat not instance of Map");
			}
		}
		return users;
	}

	public static Map<String,Group> loadGroupsFromFile() throws FileNotFoundException, IOException, ClassNotFoundException {
		Map<String,Group> groups = null;
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("data/groups.dat"))) {
			Object obj = ois.readObject();
			if (obj instanceof Map<?,?>) {
				groups = (Map<String, Group>) obj;
			} else {
				throw new IOException("groups.dat not instance of Map");
			}
		}
		return groups;
	}

}
