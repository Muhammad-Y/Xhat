package server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import common.ResultCode;

public class ClientsManager {
	private ThreadPool threadPool;
	private Map<String,User> users = Collections.synchronizedMap(new HashMap<>());
	private Map<String,Group> groups = Collections.synchronizedMap(new HashMap<>());
	
	public ClientsManager(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}
	
	public void addTestData() {
		User testUser1 = new User("Test1", "password");
		User testUser2 = new User("Test2", "password");
		User testUser3 = new User("Test3", "password");
		User testUser4 = new User("Test4", "password");
		User youUser = new User("You", "password");
		User abc = new User("abc","abc");
		User one23 = new User("123","123");
		addUser(testUser1);
		addUser(testUser2);
		addUser(testUser3);
		addUser(testUser4);
		addUser(youUser);
		addUser(abc);
		addUser(one23);
		testUser1.addContact(testUser2);
		testUser1.addContact(testUser3);
		testUser1.addContact(testUser4);
		testUser1.addContact(youUser);
		testUser2.addContact(testUser1);
		testUser2.addContact(testUser3);
		testUser2.addContact(testUser4);
//		testUser3.addContact(testUser1);
//		testUser3.addContact(testUser2);
//		testUser3.addContact(testUser4);
		testUser4.addContact(testUser1);
		testUser4.addContact(testUser2);
		testUser4.addContact(testUser3);
		testUser2.addContact(youUser);
		youUser.addContact(testUser1);
		youUser.addContact(testUser2);
		youUser.addContact(testUser3);
		youUser.addContact(youUser);
		System.out.println("Added test data");
	}
	
	public int addUser(User user) {
		synchronized (users) {
			String key = user.getUserName().toLowerCase();
			if (!users.containsKey(key)) {
				users.put(key, user);
				return ResultCode.ok;
			} else {
				return ResultCode.userNameAlreadyTaken;
			}
		}
	}

	public User getUser(String userName) {
		synchronized (users) {
			return users.get(userName.toLowerCase());
		}
	}

	public String newGroup(User founder, String groupName, String[] memberNames) {
		boolean success = true;
		String groupId = null;
		LinkedList<User> members = new LinkedList<>();
		User user = getUser(memberNames[0]);
		if (user != null && user.equals(founder)) {
			members.add(user);
		} else {
			success = false;
			System.out.println("Founder is not first member");
		}
		for (int i = 1; success == true && i < memberNames.length; i++) {
			if (memberNames[i] != null) {
				user = getUser(memberNames[i]);
			}
			if (user != null && founder.isContactWith(user)) {
				members.add(user);
			} else {
				success = false;
				System.out.println("Founder is not contact with member: " + user.getUserName());
			}
		}
		if (success == true) {
			Group newGroup = new Group(groupName, members);
			groupId = newGroup.getGroupId();
			groups.put(groupId, newGroup);
			for(User member : members) {
				member.addGroup(newGroup);
			}
		}
		return groupId;
	}

	public Group getGroup(String groupId) {
		synchronized (groups) {
			return groups.get(groupId);
		}
	}
	
	public String[] searchUser(String searchString, User fromUser) {
		ArrayList<String> results = new ArrayList<>();
		synchronized (users) {
			for (User potentialContact : users.values()) {
				String userName = potentialContact.getUserName();
				if (!potentialContact.equals(fromUser)) {
					if (userName.toLowerCase().startsWith(searchString.toLowerCase())) {
						if (!fromUser.isContactWith(potentialContact)) {
							results.add(userName);
						}
					}
				}
			}
		}
		return results.toArray(new String[results.size()]);
	}

	public void saveData() {
		synchronized (users) {
			try {
				StorageHandler.writeToFile(users, "users.dat");
				System.out.println("Saved users.dat");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		synchronized (groups) {
			try {
				StorageHandler.writeToFile(groups, "groups.dat");
				System.out.println("Saved groups.dat");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void loadData() {
		try {
			Map<String, User> users = StorageHandler.loadUsersFromFile();
			Map<String, Group> groups = StorageHandler.loadGroupsFromFile();
			this.users = Collections.synchronizedMap(users);
			System.out.println("Loaded users.dat");
			this.groups = Collections.synchronizedMap(groups);
			System.out.println("Loaded groups.dat");
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("Could not load data: " + e.getMessage());
		}
	}
}
