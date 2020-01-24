package client;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class ClientLogger {
	private final static Logger clientErrorLog = Logger.getLogger("clientErrorLog");
	private static FileHandler errorFile = null;
	
	
	public static void logInfo(String info) {
		System.out.println(info);
	}
	
	public static void logCommunication(String com) {
		System.out.println(com);
	}

	public static void logError(String error) {
		try {
			errorFile = new FileHandler("myapp-log.%u.%g.txt");
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		errorFile.setFormatter(new SimpleFormatter());
		clientErrorLog.addHandler(errorFile);
		System.out.println(errorFile);
		System.err.println(error);
		clientErrorLog.severe(error);
		
	}
}
