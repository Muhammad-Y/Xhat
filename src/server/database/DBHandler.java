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

/**
 * Klass som hanterar kommunikationen med databasen
 */
public final class DBHandler {
    private Connection conn;
    private LogListener logListener;

    public DBHandler() {
        try {
            Class.forName("org.postgresql.Driver").getConstructor().newInstance();
        } catch (Exception e) {
            logListener.logError("database.DBHandler error: " + e.toString());
        }
    }

    /**
     * Lägger till lyssnare för serverloggen. Används här främst vid felhantering
     *
     * @param listener Loglistener för servern
     */
    public void addListener(LogListener listener) {
        this.logListener = listener;
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

    /**
     * Kollar om ett givet användarnamn förekommer i users-tabellen i databasen
     *
     * @param username användarnamnet som ska kontrollersas
     * @return true om användarnamnet finns i databasen
     * @throws SQLException
     */
    public boolean checkUsername(String username) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.checkIfUserExists);
        pst.setObject(1, username);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }

    /**
     * Lägger till ny användare (user_id (SERIAL), username, password (sha256-krypterat), created_on, last_login = null)
     * i users-tabellen i databasen.
     *
     * @param user objekt som innehåller det användarnamn och lösenord som ska registreras
     * @return statuskod ok eller userNameAlreadyTaken
     * @throws SQLException
     */
    public int registerNewUser(User user) throws SQLException {
        if (!checkUsername(user.toString())) {
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

    /**
     * Lägger till två rader i contacts-tabellen (u_id = user, c_id = contact) och (u_id = contact, c_id = user)
     *
     * @param user    användaren som mottagit kontaktförfrågan
     * @param contact användaren som skickat kontaktförfrågan
     * @throws SQLException
     */
    public void addContact(String user, String contact) throws SQLException {
        ResultSet rs = getUserIdPair(user, contact);
        PreparedStatement pst = conn.prepareStatement(Statements.addContact);
        while (rs.next()) {
            pst.setObject(1, rs.getInt(1));
            pst.setObject(2, rs.getInt(2));
            pst.setObject(3, rs.getInt(2));
            pst.setObject(4, rs.getInt(1));
        }
        pst.executeUpdate();
        removeContactRequest(user, contact);
    }

    /**
     * tar bort en rad i contacts-tabellen (måste anropas två gånger för att fungera)
     *
     * @param user    användaren som initierat anropet till remove
     * @param contact kontakten som ska tas bort
     * @throws SQLException
     */
    public void removeContact(String user, String contact) throws SQLException {
        ResultSet rs = getUserIdPair(user, contact);
        PreparedStatement pst = conn.prepareStatement((Statements.deleteContact));
        while (rs.next()) {
            pst.setObject(1, rs.getInt(1));
            pst.setObject(2, rs.getInt(2));
        }
        pst.executeUpdate();
    }

    /**
     * lägger till en ny rad i contactrequests-tabellen (to_id = user_id för to (param 1), from_id = user_id
     * för from (param 2)
     *
     * @param to   användaren som mottar kontaktförfrågan
     * @param from användaren som skickar kontaktförfrågan
     * @throws SQLException
     */
    public void addContactRequest(String to, String from) throws SQLException {
        ResultSet rs = getUserIdPair(to, from);
        PreparedStatement pst = conn.prepareStatement(Statements.addContactRequest);
        while (rs.next()) {
            pst.setObject(1, rs.getInt(1));
            pst.setObject(2, rs.getInt(2));
        }
        pst.executeUpdate();
    }

    /**
     * tar bort en rad i contactrequest-tabellen när en användare svarat på kontaktförfrågan
     *
     * @param user    motsvarar to_id i contactrequests
     * @param contact motsvarar from_id i contactrequests
     * @throws SQLException
     */
    public void removeContactRequest(String user, String contact) throws SQLException {
        ResultSet rs = getUserIdPair(user, contact);
        PreparedStatement pst = conn.prepareStatement(Statements.deleteContactRequest);
        while (rs.next()) {
            pst.setObject(1, rs.getInt(1));
            pst.setObject(2, rs.getInt(2));

        }
        pst.executeUpdate();
    }

    /**
     * kontrollerar om det finns en kontaktförfrågan till användaren "to" (param1) från användaren "from" (param2)
     * i contactrequests-tabellen
     *
     * @param to   målet för kontakförfrågan
     * @param from avsändaren av kontakförfrågan
     * @return true om to_id och from_id matchar to & from (:s user_id:s)
     * @throws SQLException
     */
    public boolean getPendingContactRequest(String to, String from) throws SQLException {
        ResultSet rs = getUserIdPair(to, from);
        PreparedStatement pst = conn.prepareStatement(Statements.getPendingContactRequest);
        while (rs.next()) {
            pst.setInt(1, rs.getInt(1));
            pst.setInt(2, rs.getInt(2));
        }
        rs = pst.executeQuery();
        return rs.next();
    }

    /**
     * hämtar alla kontaktförfrågningar som skickats till en given användare (param)
     *
     * @param username användare vars user_id sak matcha to_id i contactrequests
     * @return en stringArray med användarnamnen till de användare som skickat kontakförfrågan till (param)
     * @throws SQLException
     */
    public String[] getContactRequestsArray(String username) throws SQLException {
        String[] contactRequests;
        ResultSet rs1 = getUserId(username);
        PreparedStatement pst = conn.prepareStatement(Statements.getContactRequests,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        while (rs1.next()) {
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

    /**
     * hämtar user_id kopplat till användaren i users-tabellen med användarnamnet (param)
     *
     * @param username användarnamnet som används som söknyckel
     * @return resultatet av queryt Statements.getUserId
     * @throws SQLException
     */
    private ResultSet getUserId(String username) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.getUserId);
        pst.setObject(1, username);
        return pst.executeQuery();
    }

    /**
     * hämtar user_id för två användare samtidigt (vilket kan verka onödigt)
     *
     * @param user    motsvarar 1:a kolumnen i contacts och contactrequests
     * @param contact motsvarar 2:a kolumnen i contacts och contactrequests
     * @return resultatet av queryt Statements.getUserIdPair
     * @throws SQLException
     */
    private ResultSet getUserIdPair(String user, String contact) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.getUserIdPair);
        pst.setString(1, user);
        pst.setString(2, contact);
        return pst.executeQuery();
    }

    /**
     * hämtar lista med användarnamn och onlinestatus för allla användare i (param):s kontaktlista
     *
     * @param username användaren vars kontakter ska hämtas
     * @return resultatet av queryt Statements.getContactsNameAndStatus
     * @throws SQLException
     */
    public ResultSet getContacts(String username) throws SQLException {
        PreparedStatement pst = conn.
                prepareStatement(Statements.getContactsNameAndStatus,
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        pst.setString(1, username);
        return pst.executeQuery();
    }

    /**
     * lagrar kontaktlsitan (med användarnamn och online-status (true/false)) i en 2DStringArray
     * som kan skickas till anslutna klienter för att uppdatera deras kontaktlistor
     *
     * @param username användaren vars kontakter ska hämtas
     * @return kontaktlistan (inkl. online-status) som en 2DStringArray
     * @throws SQLException
     */
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
        while (rs.next()) {
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
        while (rs.next()) {
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

    /**
     * hämtar användarnamn från users-tabellen (minus de användare som redan finns på den anropande
     * användarens kontaktlista). Sökningen returnerar alla usernames som börjar på samma bokstäver
     * (fr.o.m de två första) som söksträngen. Sökningen är case-oberoende.
     *
     * @param searchString textsträng som matats in av klienten och som matchas mot usernames i users-tabellen
     * @param fromUser användaren vars kontaklista ska uteslutas från sökningen
     * @return en stringArray med usernames
     * @throws SQLException
     */
    public String[] searchUser(String searchString, User fromUser) throws SQLException {
        ArrayList<String> results = new ArrayList<>();
        PreparedStatement pst = conn.
                prepareStatement(Statements.getPotentialContacts);
        pst.setString(1, fromUser.toString());
        pst.setString(2, searchString + '%');
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            results.add(rs.getString(1));
        }
        return results.toArray(new String[results.size()]);
    }

    /**
     * lägger till eller tar bort en given användare från users_online-tabellen.
     * Om användarnamnet finns i tabellen tas det bort - om inte - läggs det till.
     *
     * @param username användarnamnet som ska kontrolleras
     * @throws SQLException
     */
    public void updateOnlineStatus(String username) throws SQLException {
        PreparedStatement pst = conn.
                prepareStatement(Statements.getUserOnlineStatus);
        pst.setString(1, username);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            pst = conn.prepareStatement(Statements.setToOffline);
        } else {
            pst = conn.prepareStatement(Statements.setToOnline);
        }
        pst.setString(1, username);
        pst.executeUpdate();
    }

    /**
     * rensar users_online-tabellen. Anropas automatiskt när servern startar för att
     * nollställa listan över anslutna användare ifall nån ligger kvar och slaskar efter
     * en eventuell serverkrasch.
     *
     * @throws SQLException
     */
    public void resetOnlineStatus() throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.resetOnlineStatus);
        pst.executeUpdate();
    }

    /**
     * uppdaterar last_login (timestamp för senaste inloggning) hos användaren varje gång denna
     * loggar in i systemet. Ska användas för att automatiskt ta bort användarkonton som varit inaktiva 6 mån
     * (enligt krav) och för att automatiskt logga ut dem efter en viss tids inaktivitet (också enligt krav, tror jag)
     *
     * @param username aktuell inloggande användare
     * @throws SQLException
     */
    public void setLoginTime(String username) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(Statements.setLoginTime);
        pst.setObject(1, new java.sql.Timestamp(System.currentTimeMillis()));
        pst.setObject(2, username);
        pst.executeUpdate();
    }

    /**
     * kontrollerar användarnamn och lösenord när användare loggar in i systemet.
     * lösenordet som användaren angett krypteras med sha256-metoden innan det skickas
     *
     * @param username klientens användarnamn
     * @param password klientens (okrypterade) lösenord
     * @return true om queryt Statements.validateLogin returnerar träff
     */
    public boolean verifyLogin(String username, String password) {
        try {
            PreparedStatement pst = conn.
                    prepareStatement(Statements.validateLogin);
            pst.setString(1, username);
            pst.setString(2, sha256(password));
            return pst.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * krypteringsalgoritmen för användarlösenord (som de sparas i databasen)
     *
     * @param password okrypterat lösenord
     * @return krypterat lösenord
     */
    private static String sha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

