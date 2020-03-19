package Test;

import client.ClientCommunications;
import client.Data;
import common.Message;

public class whiteBoxTest {
     private Data data = new Data();

     public void sendmessage() {
         ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, data);
         cc.login("hej55", "test");
         //Message message = new Message("test1",false,"hej",1,null);
         //Message message1 = new Message("test1",false,"C:\\Users\\alexa\\Desktop\\kursliteratur",1,null);
         Message message2 = new Message("test1",false,"hisdfhifsdihfihfehifihfihuhuefwieufhwehfuiwehfweuhfuwehfiwehfuhweuhweguweoofguwehöfuhwoeufweugweuogwehgueougwegoweuogwegghweogweghowehgwe",1,null);
         //cc.sendMessage(message);
         //cc.sendMessage(message1);
         cc.sendMessage(message2);
         //cc.disconnect();

     }

    public void create() {
        try {
            ClientCommunications cc = new ClientCommunications("127.0.0.1", 5555, data);
            cc.register("hej54", "test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void login() {
        ClientCommunications clientCommunications = new ClientCommunications("127.0.0.1", 5555, data);
        clientCommunications.login("hej55", "test");
    }

    public void logout() throws InterruptedException {
        ClientCommunications clientCommunications = new ClientCommunications("127.0.0.1", 5555, data);
        clientCommunications.login("hej55", "test");
        clientCommunications.disconnect();
    }

    public void spam() {
        ClientCommunications clientCommunications = new ClientCommunications("127.0.0.1", 5555, data);
        clientCommunications.login("hej55", "test");
        Message message = new Message("test1",false,null,1,null);
        for(int i =0; i<60; i++) {
            clientCommunications.sendMessage(message);
        }
     }

     public void clientFriendsList() {
         ClientCommunications clientCommunications = new ClientCommunications("127.0.0.1", 5555, data);
         clientCommunications.login("hej55", "test");

     }

     public void friendReqNotification() {
         ClientCommunications clientCommunications = new ClientCommunications("127.0.0.1", 5555, data);
         clientCommunications.login("hej55", "test");

         ClientCommunications clientCommunications2 = new ClientCommunications("127.0.0.1", 5555, data);
         clientCommunications2.login("test4","password");

     }

     public void connectToVM() {
         ClientCommunications clientCommunications = new ClientCommunications("3.132.184.132", 5555, data);
         clientCommunications.register("hej12", "test");
         clientCommunications.login("hej12", "test");
         clientCommunications.disconnect();
     }



    public static void main(String[] args) throws Exception{
        whiteBoxTest wbt = new whiteBoxTest();
        //wbt.sendmessage();
        //wbt.create();
        //wbt.login();
        //wbt.logout();
        //wbt.spam();
        //wbt.clientFriendsList();
        //wbt.friendReqNotification();
        //wbt.connectToVM();
    }



}
