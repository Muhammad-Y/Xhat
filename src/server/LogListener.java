package server;

public interface LogListener {

	public void logInfo(String info);
	
	public void logCommunication(String com);

	public void logError(String error);

}
