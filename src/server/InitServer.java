package server;

import java.sql.*;

public class InitServer {

	public static void main(String[] args) {
		ThreadPool threadPool = new ThreadPool(30);
		threadPool.start();

		// DB test
		//		try   {
		//
		//			Class.forName("org.postgresql.Driver");
		//
		//		}
		//		catch(ClassNotFoundException e) {
		//			System.out.println("Class not found "+ e);
		//		}
		//		try {
		//			 Connection conn = DriverManager.
		//					getConnection("jdbc:postgresql://xhat-db.cnzyqrtrhsgw.us-east-1.rds.amazonaws.com/postgres",
		//							"postgres", "cEe9hIxcFfljtgRoRfv3");
		//			int limit = 1000;
		//			PreparedStatement stmt = conn.
		//					prepareStatement("select * from users");
		//			//stmt.setInt(1, limit);
		//			ResultSet rs = stmt.executeQuery();
		//			while(rs.next()) {
		//				System.out.println(rs.getString(2)+" "+rs.getString(3)+" har id "+rs.getInt(1));
		//			}
		//			conn.close();
		//		} catch (SQLException e) {
		//			e.printStackTrace();
		//		}
		//		// DB test

//		ServerLogger serverLogger = new ServerLogger();
		ClientsManager clientsManager = new ClientsManager(threadPool);
		ServerConnection serverConnection = new ServerConnection(5555, clientsManager, threadPool);
		new ServerController(serverConnection);
	}

}
