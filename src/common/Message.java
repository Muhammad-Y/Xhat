package common;

import java.io.Serializable;
import java.time.Instant;

/**
 * Message lagrar data som är associerad med ett visst meddelande.
 */
public class Message implements Serializable {
	public static final int TYPE_TEXT = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_FILE = 2;
	private Instant serverReceivedTime;
	private Instant clientReceivedTime;
	private String sender;
	private String recipient;
	private boolean isGroupMsg;
	private int type;
	private final byte[] stegoData;

	/**
	 * Skapar ett Message-objekt.
	 * @param recipient Identifierare för mottagaren.
	 * @param isGroupMsg Anger om mottagaren är en grupp.
	 * @param stegoData Ett bild-objekt som innehåller meddeladets payload krypterat med steganografi.
	 */
	public Message(String recipient, boolean isGroupMsg, int type, byte[] stegoData) {
		this.recipient = recipient;
		this.isGroupMsg = isGroupMsg;
		this.type = type;
		this.stegoData = stegoData;
	}
	
	/**
	 * Returnerar tiden då servern tog emot meddelandet om sådan data finns, annars null.
	 * @return Tiden då servern tog emot meddelandet om sådan data finns, annars null.
	 */
	public Instant getServerReceivedTime() {
		return serverReceivedTime;
	}

	/**
	 * Anger tiden då servern tog emot meddelandet.
	 * @param serverReceivedTime Tiden då servern tog emot meddelandet.
	 */
	public void setServerReceivedTime(Instant serverReceivedTime) {
		this.serverReceivedTime = serverReceivedTime;
	}
	
	/**
	 * Returnerar tiden då klienten tog emot meddelandet om sådan data finns, annars null.
	 * @return Tiden då klienten tog emot meddelandet om sådan data finns, annars null.
	 */
	public Instant getClientReceivedTime() {
		return clientReceivedTime;
	}

	/**
	 * Anger tiden då klienten tog emot meddelandet.
	 * @param clientReceivedTime Tiden då klienten tog emot meddelandet.
	 */
	public void setClientReceivedTime(Instant clientReceivedTime) {
		this.clientReceivedTime = clientReceivedTime;
	}

	/**
	 * Returnerar avsändarens userName.
	 * @return Avsändarens userName.
	 */
	public String getSender() {
		return sender;
	}
	
	/**
	 * Anger avsändarens användarnamn.
	 * @param userName Avsändarens användarnamn.
	 */
	public void setSender(String userName) {
		this.sender = userName;
	}
	
	public String getRecipient() {
		return recipient;
	}
	
	public boolean isGroupMessage() {
		return isGroupMsg;
	}
	
	public int getType() {
		return type;
	}

	/**
	 * Returnerar bild-objektet som innehåller meddeladets payload krypterat med steganografi.
	 * @return Bild-objektet om sådant finns, annars null.
	 */
	public byte[] getStegoData() {
		return stegoData;
	}
}
