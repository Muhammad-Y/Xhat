package server;

import java.io.Serializable;
import java.util.LinkedList;

public class Group implements Serializable {
	private final String groupName;
	private final String groupId;
	private LinkedList<User> members;
	
	public Group(String groupName, LinkedList<User> members, String groupId) {
		this.groupName = groupName;
		this.members = members;
		this.groupId = groupId;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public LinkedList<User> getMembers() {
		LinkedList<User> members = new LinkedList<User>();
		for(User user : this.members) {
			members.add(user);
		}
		return members;
	}
	
	public boolean removeMember(User member) {
		return members.remove(member);
	}
	
	public boolean isMember(User user) {
		return members.contains(user);
	}
}
