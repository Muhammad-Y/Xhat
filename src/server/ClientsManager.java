package server;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ClientsManager {
	private ThreadPool threadPool;
	private Map<String,User> users = Collections.synchronizedMap(new HashMap<>());
	private Map<String,Group> groups = Collections.synchronizedMap(new HashMap<>());
	
	public ClientsManager(ThreadPool threadPool) {
		this.threadPool = threadPool;
	}
	
	public void addUser(User user) {
		synchronized (users) {
			String key = user.getUserName().toLowerCase();
			users.put(key, user);
		}
	}

	public User getUser(String userName) {
		synchronized (users) {
			return users.get(userName.toLowerCase());
		}
	}

	public Group newGroup(User founder, String groupName, String[] memberNames, String groupID) {
		boolean success = true;
		String groupId;
		Group newGroup = null;
		LinkedList<User> members = new LinkedList<>();
		User user = getUser(memberNames[0]);
		if (user != null && user.equals(founder)) members.add(user);
		else {
			success = false;
			System.out.println("Founder is not first member");
		}
		for (int i = 1; success && i < memberNames.length; i++) {
			if (memberNames[i] != null)
				user = getUser(memberNames[i]);
			if (user != null)
				members.add(user);
			else success = false;
		}
		if (success) {
			newGroup = new Group(groupName, members, groupID);
			groupId = newGroup.getGroupId();
			groups.put(groupId, newGroup);
		}
		System.out.println(success);
		return newGroup;
	}

	public Group getGroup(String groupId) {
		synchronized (groups) {
			return groups.get(groupId);
		}
	}
}
