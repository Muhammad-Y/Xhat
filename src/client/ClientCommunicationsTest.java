package client;

import common.Message;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class ClientCommunicationsTest {

    /**
     * for localhost server: Run InitServer.main()
     */
    // VALID INPUT:

    @org.junit.jupiter.api.Test
    void login() {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        // Act
        boolean loggedIn = cc.login("Test1", "password");
        // Assert
        assertTrue(loggedIn);
        // tear down
        cc.disconnect();
    }

    @org.junit.jupiter.api.Test
    void sendNewContactRequest() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        String[] request = {"Test2", "false"};
        // Act
        cc.sendNewContactRequest(request);
        Thread.sleep(5000);
        DBH.open();
        Statement query = DBH.con.createStatement();
        ResultSet rs = query.executeQuery("SELECT 1 FROM contactrequests WHERE to_id = 10 AND from_id = 66");
        DBH.close();
        // Assert
        assertTrue(rs.next());

        // tear down
        DBH.open();
        Statement update = DBH.con.createStatement();
        update.executeUpdate("DELETE FROM contactrequests WHERE to_id = 10 AND from_id = 66");
        DBH.close();
        cc.disconnect();
    }

    @org.junit.jupiter.api.Test
    void sendTextMessageToOnlineUser() throws Exception {
        // Arrange
        // Run InitServer.main()
        ClientCommunications cc1 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc1.login("Test1", "password");
        ClientCommunications cc2 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc2.login("Test6", "password");
        Thread.sleep(5000);
        byte[] payload = "sending testmess".getBytes();
        // Act
        boolean success = cc1.sendMessage(new Message("Test6", false, null, Message.TYPE_TEXT, payload));
        Thread.sleep(5000);
        // Assert
        assertTrue(success);
        // tear down
        cc1.disconnect();
        cc2.disconnect();
    }

    @org.junit.jupiter.api.Test
    void sendTextMessageToOfflineUser() throws Exception {
        // Arrange
        ClientCommunications cc1 = new ClientCommunications("127.0.0.1", 5555, new Data());
        ClientCommunications cc2 = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc1.login("Test1", "password");
        Thread.sleep(5000);
        byte[] payload = "sending testmess".getBytes();
        // Act
        boolean success = cc1.sendMessage(new Message("Test6", false, null, Message.TYPE_TEXT, payload));
        Thread.sleep(10000);
        cc1.disconnect();
        Thread.sleep(5000);

        if(success) {
            cc2.login("Test6", "password");
            Thread.sleep(20000);
        }
        // Assert
        assertTrue(success);

        // Tear down
        cc2.disconnect();
    }



    @org.junit.jupiter.api.Test
    void searchUser() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        String request = "test1";
        String[] result = {"Test10", "Test11", "Test12", "Test13"};
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

    @org.junit.jupiter.api.Test
    void createNewGroup() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        String[] newGroup = {"unitTestGroup", "Test1", "Test6", "Test15"};
        // Act
        cc.createNewGroup(newGroup);
        Thread.sleep(5000);
        DBH.open();
        Statement query = DBH.con.createStatement();
        ResultSet rs = query.executeQuery(
                "SELECT groupname, username FROM groups " +
                        "JOIN groupmembers ON g_id = group_id " +
                        "JOIN users ON u_id = user_id AND groupname = newGroup[0]");
        DBH.close();
        // Assert
        boolean equals = true;
        int index = 0;
        while (rs.next()) {
            if (!rs.getString(1).equals(newGroup[0])
                    && rs.getString(2).equals(newGroup[++index])) {
                equals = false;
            }
        }
        assertTrue(equals);

        // manual tear down
        // select group_id from groups where groupname = 'unitTestGroup'
        // delete from groupmembers where g_id = ?
        // delete from groups where groupname = 'unitTestGroup'
    }

    @org.junit.jupiter.api.Test
    void leaveGroup() {
    }

    @org.junit.jupiter.api.Test
    void removeContact() throws Exception {
        // Arrange
        ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, new Data());
        cc.login("Test1", "password");
        Thread.sleep(5000);
        String request = "Test6";
        // Act
        cc.removeContact(request);
        Thread.sleep(5000);
        DBH.open();
        Statement query = DBH.con.createStatement();
        ResultSet rs = query.executeQuery("SELECT * FROM contacts " +
                "WHERE u_id = 66 AND c_id = 27" +
                "OR u_id = 27 AND c_id = 66");
        DBH.close();
        // Assert
        assertTrue(!rs.next());

        // tear down
        DBH.open();
        Statement update = DBH.con.createStatement();
        update.executeUpdate("INSERT INTO contacts VALUES (66, 27), (27, 66)");
        DBH.close();
    }

    @org.junit.jupiter.api.Test
    void isConnected() {
    }

    @org.junit.jupiter.api.Test
    void register() {
    }

    @org.junit.jupiter.api.Test
    void disconnect() {
    }

    @org.junit.jupiter.api.Test
    void testDisconnect() {
    }

    @org.junit.jupiter.api.Test
    void getUserKey() {
    }

    @org.junit.jupiter.api.Test
    void run() {
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
}