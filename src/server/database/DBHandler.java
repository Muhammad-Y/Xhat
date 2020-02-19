package server.database;

import common.ResultCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.Random;

import server.Group;
import server.LogListener;
import server.User;

public final class DBHandler {
    private Connection conn;
    private LogListener logListener;

    public DBHandler(LogListener logListener) {
        this.logListener = logListener;
        try {
            Class.forName("org.postgresql.Driver").getConstructor().newInstance();
        } catch (Exception e) {
            logListener.logError("database.DBHandler error: " + e.toString());
        }
    }

    public void open() {
        try {
            conn = DriverManager.
                    getConnection("jdbc:postgresql://xhat-db.cnzyqrtrhsgw.us-east-1.rds.amazonaws.com/postgres",
                            "postgres", "cEe9hIxcFfljtgRoRfv3");
        } catch (SQLException e) {
            logListener.logError("Connection failed.");
        }
    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            logListener.logError("database.DBHandler error: " + e.toString());
        }
    }

    public ResultSet checkUsername(String username) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.checkIfUserExists);
        pst.setObject(1, username);
        return pst.executeQuery();
    }

    public int registerNewUser(User user) throws SQLException {
            if(!checkUsername(user.toString()).next()) {
                PreparedStatement pst = conn.
                        prepareStatement(Statements.registerNewUser);
                pst.setObject(1, user.getUserName());
                pst.setObject(2, user.getProtectedPassword());
                pst.setObject(3, new java.sql.Timestamp(System.currentTimeMillis()));
                pst.executeUpdate();
                return ResultCode.ok;
            }
        return ResultCode.userNameAlreadyTaken;
    }

    public void addContact(String user, String contact) throws SQLException {
            ResultSet rs = getUserIdPair(user, contact);
            PreparedStatement pst = conn.prepareStatement(Statements.addContact);
            while(rs.next()) {
                pst.setObject(1, rs.getInt(1));
                pst.setObject(2, rs.getInt(2));
                pst.setObject(3, rs.getInt(2));
                pst.setObject(4, rs.getInt(1));
            }
            pst.executeUpdate();
        removeContactRequest(user, contact);
    }

    public void removeContact(String user, String contact) throws SQLException {
        ResultSet rs = getUserIdPair(user, contact);
        PreparedStatement pst = conn.prepareStatement((Statements.deleteContact));
        while(rs.next()) {
            pst.setObject(1, rs.getInt(1));
            pst.setObject(2, rs.getInt(2));
        }
        pst.executeUpdate();
    }

    public void addContactRequest(String to, String from) throws SQLException {
            ResultSet rs = getUserIdPair(to, from);
            PreparedStatement pst = conn.prepareStatement(Statements.addContactRequest);
            while(rs.next()) {
                pst.setObject(1, rs.getInt(1));
                pst.setObject(2, rs.getInt(2));
            }
            pst.executeUpdate();
    }

    public void removeContactRequest(String user, String contact) throws SQLException {
            ResultSet rs = getUserIdPair(user, contact);
            PreparedStatement pst = conn.prepareStatement(Statements.deleteContactRequest);
            while (rs.next()) {
                pst.setObject(1, rs.getInt(1));
                pst.setObject(2, rs.getInt(2));

            }
            pst.executeUpdate();
    }

    public ResultSet getPendingContactRequest(String to, String from) throws SQLException {
        ResultSet rs = getUserIdPair(to, from);
        PreparedStatement pst = conn.prepareStatement(Statements.getPendingContactRequest);
        while(rs.next()) {
            pst.setInt(1, rs.getInt(1));
            pst.setInt(2, rs.getInt(2));
        }
        return pst.executeQuery();
    }

    public String[] getContactRequestsArray(String username) throws  SQLException {
        String[] contactRequests;
        ResultSet rs1 = getUserId(username);
        PreparedStatement pst = conn.prepareStatement(Statements.getContactRequests,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        while(rs1.next()) {
            pst.setObject(1, rs1.getInt(1));
        }
        ResultSet rs2 = pst.executeQuery();
        int rowCount = 0;
        if (rs2.last()) {
            rowCount = rs2.getRow();
            rs2.beforeFirst();
        }
        contactRequests = new String[rowCount];
            for (int i = 0; rs2.next(); i++) {
                contactRequests[i] = rs2.getString(1);
            }
        return contactRequests;
    }

    private ResultSet getUserId(String username) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.getUserId);
        pst.setObject(1, username);
        return pst.executeQuery();
    }

    private ResultSet getUserIdPair(String user, String contact) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.getUserIdPair);
        pst.setString(1, user);
        pst.setString(2, contact);
        return pst.executeQuery();
    }

    public ResultSet getContacts(String username) throws SQLException {
            PreparedStatement pst = conn.
                    prepareStatement(Statements.getContactsNameAndStatus,
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, username);
            return pst.executeQuery();
    }

    public String[][] getContactsArray(String username) throws SQLException {
        String[][] contactsArray;
            ResultSet rs = getContacts(username);
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
            return contactsArray;
    }

    public ResultSet getGroups(String username) throws SQLException {
            PreparedStatement pst = conn.
                    prepareStatement(Statements.getGroupsAndMembers,
                            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            pst.setString(1, username);
            return pst.executeQuery();
    }

    public String[][] getGroupsArray(String username) throws SQLException {
        ResultSet rs = getGroups(username);
        int rowCount = 0;
        while(rs.next()) {
            rowCount++;
        }
        rs.beforeFirst();
        String[][] groupsArray = new String[rowCount][2];
        int i = 0;
        while(rs.next()) {
            groupsArray[i][0] = rs.getString(2);
            groupsArray[i][1] = String.valueOf((rs.getInt(1)));
            i++;
        }
        return groupsArray;
    }

    public void addGroup(String groupname, String[] members){
        try {
            PreparedStatement pst = conn.prepareStatement(Statements.insertIntoGroups);
            pst.setString(1, groupname);
            pst.execute();
            for(String member : members) {
                pst = conn.prepareStatement(Statements.addGroupMember);
                pst.setString(1, groupname);
                pst.setString(2, member);
                pst.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getGroupID(String groupName) {
        try {
            PreparedStatement pst = conn.prepareStatement(Statements.getGroupID);
            pst.setString(1, groupName);
            ResultSet rs = pst.executeQuery();
            String groupID = "";
            while(rs.next())
                groupID = rs.getString(1);
            return groupID;
        }catch (Exception e){

        }
        return null;
    }

    public String[] searchUser(String searchString, User fromUser) {
        ArrayList<String> results = new ArrayList<>();
        open();
        try {
            PreparedStatement pst = conn.
                    prepareStatement(Statements.getPotentialContacts);
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

    public void updateOnlineStatus(String username) {
        open();
        try{
            PreparedStatement pst = conn.
                    prepareStatement(Statements.getUserOnlineStatus);
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if(rs.next()) {
                pst = conn.prepareStatement(Statements.setToOffline);
            } else {
                pst = conn.prepareStatement(Statements.setToOnline);
            }
            pst.setString(1, username);
            pst.executeUpdate();
        }catch (SQLException e) {
            e.printStackTrace();
        }
        close();
    }

    public void resetOnlineStatus() throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.resetOnlineStatus);
        pst.executeUpdate();
    }

    public boolean verifyLogin(String username, String password) {
        open();
        try{
            PreparedStatement pst = conn.
                    prepareStatement(Statements.validateLogin);
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

