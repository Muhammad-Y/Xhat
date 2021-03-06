package server.database;

public class Statements {

    static final String getPotentialContacts =
            "WITH temp AS (SELECT user_id FROM users WHERE username = (?)) " +
                    "SELECT username FROM users WHERE NOT user_id IN " +
                    "(SELECT u_id FROM contacts JOIN temp " +
                    "ON temp.user_id = u_id OR temp.user_id = c_id) " +
                    "AND username ILIKE (?)";

    static final String getUserOnlineStatus =
            "SELECT 1 FROM users_online WHERE username = (?)";

    static final String setToOffline =
            "DELETE FROM users_online WHERE username = (?)";

    static final String setToOnline =
            "INSERT INTO users_online VALUES (?)";

    static final String validateLogin =
            "SELECT 1 FROM users WHERE username = (?) AND password = (?)";

    static final String getContactsNameAndStatus =
            "WITH temp AS (SELECT username FROM users WHERE user_id IN " +
                    "(SELECT c_id FROM contacts JOIN users ON contacts.u_id = users.user_id " +
                    "AND users.username = (?))) " +
                    "SELECT temp.username, users_online.username FROM temp " +
                    "LEFT JOIN users_online ON temp.username = users_online.username";

    static final String getUserId =
            "SELECT user_id FROM users WHERE username = (?)";

    static final String getUserIdPair =
            "SELECT u1.user_id, u2.user_id FROM users AS u1 " +
                    "JOIN users AS u2 ON u1.username = (?) AND u2.username = (?)";

    static final String deleteContactRequest =
            "DELETE FROM contactrequests WHERE to_id = (?) AND from_id = (?)";

    static final String addContact =
            "INSERT INTO contacts VALUES (?,?),(?,?)";

    static final String deleteContact =
            "DELETE FROM contacts WHERE u_id = (?) AND c_id = (?)";

    static final String registerNewUser =
            "INSERT INTO users (username, password, created_on) VALUES (?,?,?)";

    static final String checkIfUserExists =
            "SELECT 1 FROM users WHERE username = (?)";

    static final String insertIntoGroups =
            "INSERT INTO groups (groupname) VALUES (?)";

    static final String getGroupID =
            "Select group_id from groups where groupname = (?)";

    static final String addGroupMember =
            "INSERT INTO groupmembers VALUES (" +
                    "(select group_id from groups where groupname = (?)), " +
                    "(select user_id from users where username = (?))" +
                    ")";

    static final String removeGroupMember =
            "DELETE FROM groupmembers WHERE u_id = (?)";

    static final String getGroupsAndMembers =
            "select group_id, groupname from groups " +
                    "inner join groupmembers on groups.group_id=groupmembers.g_id " +
                    "inner join users on groupmembers.u_id=users.user_id " +
                    "where users.username = (?)";

    static final String getPendingContactRequest =
            "SELECT 1 FROM contactrequests WHERE to_id = (?) AND from_id = (?)";

    static final String getContactRequests =
            "SELECT username FROM users JOIN contactrequests AS cr " +
                    "ON cr.from_id = users.user_id AND cr.to_id = (?)";

    static final String addContactRequest =
            "INSERT INTO contactrequests VALUES (?,?)";

    static final String resetOnlineStatus =
            "DELETE FROM users_online";

    static final String setLoginTime =
            "UPDATE users SET last_login = (?) WHERE username = (?)";
}
