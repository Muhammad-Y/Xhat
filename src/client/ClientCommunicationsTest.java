package client;

import common.Message;

import java.io.*;
import java.sql.*;

import common.ResultCode;
import org.junit.jupiter.api.Test;
import server.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OBS!
 * För att köra dessa tester: Starta först InitServer (i server-paketet)
 * Kör sedan testerna metod för metod.
 */
class ClientCommunicationsTest {

    // VALID INPUT

    @Test
    void validLogin() {     // FK_LIU_1, FK_LIU_1.2, FK_DBK_1
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        // Act
        boolean loggedIn = cc.login("Test1", "password");
        // Assert
        assertTrue(loggedIn);
        // Tear down
        cc.disconnect();
    }

    @Test
    void validContactRequest() throws Exception {   // FK_KO_3.1, KK_AV_4, FK_KO_3.3
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        String[] request = {"Test2", "false"};
        Thread.sleep(5000);
        // Act
        cc.sendNewContactRequest(request);
        Thread.sleep(5000);
        // Assert
        DBH.open();
        Statement query = DBH.con.createStatement();
        ResultSet rs = query.executeQuery("SELECT 1 FROM contactrequests WHERE to_id = 10 AND from_id = 66");
        DBH.close();
        assertTrue(rs.next());
        // Tear down
        DBH.open();
        Statement update = DBH.con.createStatement();
        update.executeUpdate("DELETE FROM contactrequests WHERE to_id = 10 AND from_id = 66");
        DBH.close();
        cc.disconnect();
    }

    @Test
    void validTextMessageToOnlineUser() throws Exception {      // FK_KO_1.1, FK_KO_1.3
        // Arrange
        ClientCommunications cc1 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc1.login("Test1", "password");
        ClientCommunications cc2 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc2.login("Test6", "password");
        byte[] payload = "sending testmess".getBytes();
        Thread.sleep(5000);
        // Act
        boolean success = cc1.sendMessage(new Message("Test6", false, null, Message.TYPE_TEXT, payload));
        Thread.sleep(5000);
        // Assert
        assertTrue(success);
        // Tear down
        cc1.disconnect();
        cc2.disconnect();
    }

    @Test
    void validTextMessageToOfflineUser() throws Exception {     // FK_KO_1.1, FK_KO_1.3, FK_L_4, FK_L_5
        // Arrange
        ClientCommunications cc1 = new ClientCommunications("127.0.0.1", 5555, new Data());
        ClientCommunications cc2 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc1.login("Test1", "password");
        byte[] payload = "testmess".getBytes();
        Thread.sleep(5000);
        // Act
        boolean success = cc1.sendMessage(new Message("Test6", false, null, Message.TYPE_TEXT, payload));
        Thread.sleep(5000);
        cc1.disconnect();
        Thread.sleep(5000);
        // Assert
        if (success) {
            cc2.login("Test6", "password");
            Thread.sleep(5000);
        }
        assertTrue(success);
        // Tear down
        cc2.disconnect();
    }

    @Test
    void validSearch() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        String request = "unit";
        String[] result = {"unitTest1", "unitTest2", "unitTest3", "unitTest4"};
        // Act
        cc.searchUser(request);
        Thread.sleep(5000);
        // Assert
        boolean equals = true;
        for (int i = 0; equals && i < result.length; i++) {
            if (!result[i].equals(cc.results[i])) {
                equals = false;
            }
        }
        assertTrue(equals);
    }

    @Test
    void validRemoveContact() throws Exception {    // FK_KO_3.2, KK_AV_3.1
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        String request = "Test6";
        // Act
        cc.removeContact(request);
        Thread.sleep(5000);
        // Assert
        DBH.open();
        Statement query = DBH.con.createStatement();
        ResultSet rs = query.executeQuery("SELECT * FROM contacts " +
                "WHERE u_id = 66 AND c_id = 27" +
                "OR u_id = 27 AND c_id = 66");
        DBH.close();
        assertTrue(!rs.next());
        // Tear down
        DBH.open();
        Statement update = DBH.con.createStatement();
        update.executeUpdate("INSERT INTO contacts VALUES (66, 27), (27, 66)");
        DBH.close();
    }

    @Test
    void validRegistration() throws Exception {     // FK_L_1.1
        // Arrange
        String name = "unitTest5", password = "word";
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        // Act
        int result = cc.register(name, password);
        Thread.sleep(5000);
        // Assert
        DBH.open();
        PreparedStatement pst = DBH.con.prepareStatement("SELECT 1 FROM users WHERE username = (?)");
        pst.setObject(1, name);
        ResultSet rs = pst.executeQuery();
        DBH.close();
        assertTrue(rs.next() && result == ResultCode.ok);
        // Tear down
        DBH.open();
        pst = DBH.con.prepareStatement("DELETE FROM users WHERE username = (?)");
        pst.setObject(1, name);
        pst.executeUpdate();
        DBH.close();
        cc.disconnect();
    }

    // INVALID INPUT

    @Test
    void invalidLogin() throws Exception {  // FK_LIU_1.1
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        // Act
        boolean loggedIn = cc.login("Fest1", "password");
        // Assert
        assertFalse(loggedIn);
        // Tear down
        cc.disconnect();
    }

    @Test
    void invalidContactRequest() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        String[] request = {"Test6", "false"};
        Thread.sleep(5000);
        // Act
        cc.sendNewContactRequest(request);
        Thread.sleep(5000);
        // Assert
        DBH.open();
        Statement query = DBH.con.createStatement();
        ResultSet rs = query.executeQuery("SELECT 1 FROM contactrequests WHERE to_id = 10 AND from_id = 66");
        DBH.close();
        assertFalse(rs.next());
        // Tear down
        DBH.open();
        Statement update = DBH.con.createStatement();
        update.executeUpdate("DELETE FROM contactrequests WHERE to_id = 10 AND from_id = 66");
        DBH.close();
        cc.disconnect();
    }

    @Test
    void invalidTextMessageToOnlineUser() throws Exception { // KK_PRE_2
        // Arrange
        ClientCommunications cc1 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc1.login("Test1", "password");
        ClientCommunications cc2 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc2.login("Test6", "password");
        StringBuilder sb = new StringBuilder();
        while(sb.length() <= 3000) {
            sb.append("a");
        }
        byte[] payload = sb.toString().getBytes();
        Thread.sleep(5000);
        // Act
        boolean success = cc1.sendMessage(new Message("Test6", false, null, Message.TYPE_TEXT, payload));
        Thread.sleep(5000);
        // Assert
        assertFalse(success);
        // Tear down
        cc1.disconnect();
        cc2.disconnect();
    }

    @Test
    void invalidTextMessageToOfflineUser() throws Exception {   // KK_PRE_2
        // Arrange
        ClientCommunications cc1 = new ClientCommunications("127.0.0.1", 5555, new Data());
        ClientCommunications cc2 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc1.login("Test1", "password");
        StringBuilder sb = new StringBuilder();
        while(sb.length() <= 3000) {
            sb.append("a");
        }
        byte[] payload = sb.toString().getBytes();
        Thread.sleep(5000);
        // Act
        boolean success = cc1.sendMessage(new Message("Test6", false, null, Message.TYPE_TEXT, payload));
        Thread.sleep(5000);
        cc1.disconnect();
        Thread.sleep(5000);
        // Assert
        if (success) {
            cc2.login("Test6", "password");
            Thread.sleep(5000);
        }
        assertFalse(success);
        // Tear down
        cc2.disconnect();
    }

    @Test
    void invalidSearch() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        boolean equals = false;
        String request = "u";
        String[] result = {"unitTest1", "unitTest2", "unitTest3", "unitTest4"};
        // Act
        cc.searchUser(request);
        Thread.sleep(5000);
        // Assert
        if(cc.results != null) {
            equals = true;
            for (int i = 0; i < result.length; i++)
                if (!result[i].equals(cc.results[i])) {
                    equals = false;
                    break;
                }
        }
        assertFalse(equals);
    }

    @Test
    void invalidRegistration() throws Exception {   // FK_L_1.3
        // Arrange
        String name = "Test1", password = "password";
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        // Act
        int result = cc.register(name, password);
        Thread.sleep(5000);
        // Assert
        DBH.open();
        PreparedStatement pst = DBH.con.prepareStatement("SELECT 1 FROM users WHERE username = (?)");
        pst.setObject(1, name);
        ResultSet rs = pst.executeQuery();
        DBH.close();
        assertTrue(rs.next() && result == ResultCode.userNameAlreadyTaken);
    }


    private static class DBH {
        private static Connection con;

        public DBH() {
            try {
                Class.forName("org.postgresql.Driver").getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void open() {
            try {
                con = DriverManager.
                        getConnection("jdbc:postgresql://xhat-db.cnzyqrtrhsgw.us-east-1.rds.amazonaws.com/postgres",
                                "postgres", "cEe9hIxcFfljtgRoRfv3");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static void close() {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static class LocalServer extends Thread {

        public void run() {
            ThreadPool threadPool = new ThreadPool(30);
            threadPool.start();

            //		ServerLogger serverLogger = new ServerLogger();
            ClientsManager clientsManager = new ClientsManager(threadPool);
            try {
                clientsManager.setUsers(StorageHandler.loadUsersFromFile());
                clientsManager.setGroups(StorageHandler.loadGroupsFromFile());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            ServerConnection serverConnection = new ServerConnection(5555, clientsManager, threadPool);
            new ServerController(serverConnection);
        }
    }
}