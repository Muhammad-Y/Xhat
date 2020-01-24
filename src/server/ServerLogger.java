package server;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class ServerLogger {
	private final static Logger serverErrorLog = Logger.getLogger("serverErrorLog");
	private static FileHandler serverFile = null;

	public static void logInfo(String info) {
		System.out.println(info);
	}
	
	public static void logCommunication(String com) {
		System.out.println(com);
	}

	public static void logError(String error) {
		System.err.println(error);
		try {
			serverFile = new FileHandler("myapp-log.%u.%g.txt");
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		serverFile.setFormatter(new SimpleFormatter());
		serverErrorLog.addHandler(serverFile);
		System.out.println(serverFile);
		serverErrorLog.severe(error);
		
	}
	}


