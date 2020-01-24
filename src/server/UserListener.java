package server;

public interface UserListener {
	public void updateContactList(User contact);
	public void updateGroupList(Group group);
	public void newContactRequest(String userName);
}
