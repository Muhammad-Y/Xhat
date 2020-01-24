package client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * Klassen lagrar en användares kontakter, grupper och konversationer.
 *
 */
public class Data {
	private HashMap<String, Contact> contacts = new HashMap<>();
	private HashMap<String, GroupChat> groups = new HashMap<>();
	
	public Data() {
		//read from disk if we want
	}
	
	public void addContact(Contact contact) {
		contacts.put(contact.getName(), contact);
	}
	
	public Contact getContact(String contactName) {
		return contacts.get(contactName);
	}
	
	public boolean isContact(String contactName) {
		return contacts.containsKey(contactName);
	}
	
	public Collection<Contact> getContacts() {
		return contacts.values();
	}
	
	public Set<String> getContactKeys() {
		return contacts.keySet();
	}
	
	public void addGroup(String groupName, String groupId) {
		groups.put(groupId, new GroupChat(groupName, groupId));
	}
	
	public GroupChat getGroup(String groupId) {
		return groups.get(groupId);
	}
	
	public void clearGroups() {
		groups.clear();
	}
	
	public Collection<GroupChat> getGroups() {
		return groups.values();
	}
	
}
