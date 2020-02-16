package server;

import common.ResultCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import common.ResultCode;

public final class DBHandler {
    private Connection conn;
    private boolean verbose = true;

    public DBHandler() {
        try {
            Class.forName("org.postgresql.Driver").getConstructor().newInstance();
        } catch (Exception e) {
            System.out.println("database.DBHandler error: " + e.toString());
        }
    }

    public void open() {
        if (verbose)
            System.out.println("Opening connection...");
        try {
            conn = DriverManager.
                    getConnection("jdbc:postgresql://xhat-db.cnzyqrtrhsgw.us-east-1.rds.amazonaws.com/postgres",
                            "postgres", "cEe9hIxcFfljtgRoRfv3");
            if (verbose)
                System.out.println("Connection open.");
        } catch (SQLException e) {
            System.out.println("Connection failed.");
        }
    }

    public void close() {
        try {
            conn.close();
            if (verbose)
                System.out.println("Connection closed.");
        } catch (SQLException e) {
            System.out.println("database.DBHandler error: " + e.toString());
        }
    }

    private ResultSet checkUsername(String username) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT 1 FROM users WHERE username = (?) ");
        pst.setObject(1, username);
        return pst.executeQuery();
    }

    public int addUser(User user) {
        open();
        try {
            if(!checkUsername(user.toString()).next()) {
                PreparedStatement pst = conn.
                        prepareStatement("INSERT INTO users (username, password, created_on) VALUES (?,?,?)");
                pst.setObject(1, user.getUserName());
                pst.setObject(2, user.getProtectedPassword());
                pst.setObject(3, new java.sql.Timestamp(System.currentTimeMillis()));
                pst.executeUpdate();
                close();
                return ResultCode.ok;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ResultCode.userNameAlreadyTaken;
    }

    public void addContact(String user, String contact) {
        open();
        try {
            ResultSet rs = getUserId(user, contact);
            PreparedStatement pst1 = conn.
                    prepareStatement("INSERT INTO contacts VALUES (?,?)");
            PreparedStatement pst2 = conn.
                    prepareStatement("INSERT INTO contacts VALUES (?,?)");
            int count = 1;
            while(rs.next()) {
                pst1.setObject(1, rs.getInt(1));
                pst1.setObject(2, rs.getInt(2));
                pst2.setObject(1, rs.getInt(2));
                pst2.setObject(2, rs.getInt(1));
            }
            pst1.executeUpdate();
            pst2.executeUpdate();
            close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        removeContactRequest(user, contact);
        // updateContactList(user);
    }

    public void removeContactRequest(String user, String contact) {
        open();
        try {
            ResultSet rs = getUserId(user, contact);
            PreparedStatement pst = conn.
                    prepareStatement("DELETE FROM contactrequests WHERE to_id = (?) AND from_id = (?)");

            while(rs.next()) {
                pst.setObject(1, rs.getInt(1));
                pst.setObject(2, rs.getInt(2));
            }
            pst.executeUpdate();
            close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ResultSet getUserId(String user, String contact) throws SQLException {
        String query = "SELECT u1.user_id, u2.user_id FROM users AS u1 " +
                "JOIN users AS u2 ON u1.username = (?) AND u2.username = (?)";
        PreparedStatement pst = conn.
                prepareStatement(query);
        pst.setString(1, user);
        pst.setString(2, contact);
        return pst.executeQuery();
    }

    public ResultSet getContacts(User user) {
        open();
        try {
            PreparedStatement pst = conn.
                    prepareStatement("WITH temp AS (SELECT username FROM users WHERE user_id IN " +
                                    "(SELECT c_id FROM contacts JOIN users ON contacts.u_id = users.user_id " +
                                    "AND users.username = (?))) " +
                                    "SELECT temp.username, users_online.username FROM temp " +
                                    "LEFT JOIN users_online ON temp.username = users_online.username",
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, user.toString());
            return pst.executeQuery();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ResultSet getGroups(User user) {
        open();
        try {
            PreparedStatement pst = conn.
                    prepareStatement("SELECT g_id FROM groupmembers " +
                                    "JOIN users ON groupmembers.u_id = users.user_id " +
                                    "AND users.username = (?)",
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, user.toString());
            return pst.executeQuery();
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[][] getContactsArray(User user) {
        String[][] contactsArray;
        try{
            ResultSet rs = getContacts(user);
            int rowCount = 0;
            if (rs.last()) {
                rowCount = rs.getRow();
                rs.beforeFirst();
            }
            contactsArray = new String[rowCount][2];
            int i = 0;
            while(rs.next()) {
                contactsArray[i][0] = rs.getString(1);
                contactsArray[i][1] = (rs.getString(2) != null) ? "true" : "false";
                i++;
            }
            close();
            return contactsArray;
        }catch(SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String[][] getGroupsArray(User user) {
        String[][] groupsArray = {};
        return groupsArray;
    }

    public User getUser(String userName) {
        return null;
    }

    public String[] searchUser(String searchString, User fromUser) {
        ArrayList<String> results = new ArrayList<>();
        open();
        try {
            PreparedStatement pst = conn.
                    prepareStatement("WITH temp AS (SELECT user_id FROM users WHERE username = (?)) " +
                    "SELECT username FROM users WHERE NOT user_id IN " +
                    "(SELECT u_id FROM contacts JOIN temp " +
                    "ON temp.user_id = u_id OR temp.user_id = c_id) " +
                            "AND username LIKE (?)");
            pst.setString(1, fromUser.toString());
            pst.setString(2, searchString + '%');
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                results.add(rs.getString(1));
            }
            return results.toArray(new String[results.size()]);
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void isOnline(String username) {
        open();
        try{
            PreparedStatement pst = conn.
                    prepareStatement("SELECT 1 FROM users_online WHERE username = (?)");
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) {
                pst = conn.
                        prepareStatement("DELETE FROM users_online WHERE username = (?)");
                pst.setString(1, username);
                pst.executeUpdate();
            } else {
                pst = conn.
                        prepareStatement("INSERT INTO users_online VALUES (?)");
                pst.setString(1, username);
                pst.executeUpdate();
            }
            close();
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean verifyLogin(String username, String password) {
        open();
        try{
            PreparedStatement pst = conn.
                    prepareStatement("SELECT 1 FROM users WHERE username = (?) AND password = (?)");
            pst.setString(1, username);
            pst.setString(2, sha256(password));
            return pst.executeQuery().next();
        }catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String sha256(String password) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
}

