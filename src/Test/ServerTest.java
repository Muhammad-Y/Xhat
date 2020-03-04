package Test;

import common.Encryption;
import org.junit.jupiter.api.*;
import server.*;
import server.database.DBHandler;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    User user1 = new User("TestUser1", "12345"), user2 = new User("TestUser2", "12345");
    String testText = "Test123";
    File testFile = new File("C:\\Users\\TheComputer\\Documents\\test.txt");

    @Test
    void FK_L_1(){
        DBHandler dbHandler = new DBHandler(null);
        try {
            dbHandler.open();
            dbHandler.registerNewUser(user1);
            String user = dbHandler.searchUser("TestUser1", user2)[0];
            assertEquals( "TestUser1", user);
            dbHandler.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void FK_DBK_1(){
        boolean passed;
        try {
            Class.forName("org.postgresql.Driver").getConstructor().newInstance();
            Connection conn = DriverManager.
                    getConnection("jdbc:postgresql://xhat-db.cnzyqrtrhsgw.us-east-1.rds.amazonaws.com/postgres",
                            "postgres", "cEe9hIxcFfljtgRoRfv3");
            passed = true;
        } catch (Exception e) {
           passed = false;
        }
        assertEquals(true, passed);
    }

    @Test
    void KK_AV_5_1_Valid(){
        String userName = "Mattias123";
        boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
        assertEquals(true, userNameOk);
    }

    @Test
    void KK_AV_5_1_InvalidCharacter(){
        String userName = "Mattias123!";
        boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
        assertEquals(false, userNameOk);
    }

    @Test
    void KK_AV_5_1_Length_Over_Boundary(){
        String userName = "Mattias123!123456789";
        boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
        assertEquals(false, userNameOk);
    }

    @Test
    void KK_AV_5_1_Length_Under_Boundary(){
        String userName = "";
        boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
        assertEquals(false, userNameOk);
    }

    @Test
    void KK_AV_5_1_Length_Upper_Boundary(){
        String userName = "1234567890";
        boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
        assertEquals(true, userNameOk);
    }

    @Test
    void KK_AV_5_1_Length_Lower_Boundary(){
        String userName = "A";
        boolean userNameOk = userName.length() > 0 && userName.length() <= 10 && !hasSpecialCharacters(userName);
        assertEquals(true, userNameOk);
    }

    @Test
    void KK_AV_5_2_Valid(){
        String password = "123456789";
        boolean passwordOk = password.length() > 0 && password.length() <= 20;
        assertEquals(true, passwordOk);
    }

    @Test
    void KK_AV_5_2_Length_Over_Boundary(){
        String password = "MattiasMattias123456789x";
        boolean passwordOk = password.length() > 0 && password.length() <= 20;
        assertEquals(false, passwordOk);
    }

    @Test
    void KK_AV_5_2_Length_Under_Boundary(){
        String password = "";
        boolean passwordOk = password.length() > 0 && password.length() <= 20;
        assertEquals(false, passwordOk);
    }

    @Test
    void KK_AV_5_2_Length_Upper_Boundary(){
        String password = "12345678900987654321";
        boolean passwordOk = password.length() > 0 && password.length() <= 20;
        assertEquals(true, passwordOk);
    }

    @Test
    void KK_AV_5_2_Length_LowerBoundary() {
        String password = "1";
        boolean passwordOk = password.length() > 0 && password.length() <= 20;
        assertEquals(true, passwordOk);
    }

    private boolean hasSpecialCharacters(String string) {
        for (Character c : string.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return true;
            }
        }
        return false;
    }

}
