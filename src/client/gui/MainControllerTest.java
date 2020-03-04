package client.gui;

import client.ClientCommunications;
import client.Contact;
import client.Data;
import common.Message;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainControllerTest {

    @Test
    void setDownloadPath() {
        // Arrange
        MainController mc = new MainController(
                new ClientCommunications("127.0.0.1", 5555, new Data()), new Data());
        String home = System.getProperty("user.home");
        String separator = System.getProperty("file.separator");
        // Act
        mc.setDownloadPath();
        // Assert
        assertEquals(home + separator + "Downloads" + separator, mc.downloadPath);
    }

    @Test
    void updateGroupList() throws Exception {
        // Arrange
        MainController mc = new MainController(
                new ClientCommunications("127.0.0.1", 5555, new Data()), new Data());
        String[][] groups = {
                {"testGroup1", "1"},
                {"testGroup2", "2"},
                {"testGroup3", "3"}
        };
        for(int i = 0; i < groups.length; i++) {
            mc.data.addGroup(groups[i][0], groups[i][1]);
        }
        // Act
        mc.updateGroupList();
        Thread.sleep(3000);
        // Assert
        assertEquals(groups[0][0], mc.groupsModel.get(0).getText());
        assertEquals(groups[1][0], mc.groupsModel.get(1).getText());
        assertEquals(groups[2][0], mc.groupsModel.get(2).getText());
    }

    @Test
    void updateContactsList() throws Exception {
        // Arrange
        MainController mc = new MainController(
                new ClientCommunications("127.0.0.1", 5555, new Data()), new Data());
        String[][] contacts = new String[][]{
                {"testContact1", "false"},
                {"testContact2", "true"},
                {"testContact3", "false"}
        };
        for (String[] strings : contacts) {
            Contact contact = new Contact(strings[0]);
            contact.setIsOnline(Boolean.parseBoolean(strings[1]));
            mc.data.addContact(contact);
        }
        // Act
        mc.updateContactsList();
        Thread.sleep(3000);
        // Assert
        assertEquals(contacts[0][0], mc.contactsModel.get(0).getText());
        assertEquals(contacts[1][0], mc.contactsModel.get(1).getText());
        assertEquals(contacts[2][0], mc.contactsModel.get(2).getText());
    }

    @Test
    void getUserKey() {
    }

    @Test
    void createNewGroup() {
    }

    @Test
    void leaveGroupChat() {
    }

    @Test
    void removeContact() {
    }

    @Test
    void notifyNewMessage() {
    }

    @Test
    void showConversation() {
    }

    @Test
    void isContactSelected() {
    }

    @Test
    void addMessageToConversation() {
    }

    @Test
    void disconnected() {
    }

    @Test
    void searchContact() {
    }

    @Test
    void searchUser() {
    }

    @Test
    void updateSearchResults() {
    }

    @Test
    void sendNewContactRequest() {
    }

    @Test
    void notifyNewContactRequest() {
    }

    @Test
    void restartDisconnectTimer() {
    }
}