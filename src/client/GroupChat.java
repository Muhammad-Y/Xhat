package client;

public class GroupChat extends Contact {
	private String groupId;
	
	public GroupChat(String groupName, String groupId) {
		super(groupName);
		this.groupId = groupId;
	}

	public String getGroupId() {
		return groupId;
	}
}
