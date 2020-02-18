package common;

import sun.security.rsa.RSAPublicKeyImpl;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Contains static methods for generating RSA keypairs, encrypting and decrypting files using AES/RSA encryption.
 * 
 * @author Mattias Jönsson
 * @version 1.0
 * @since 2020-02-11
 *
 */
public class Encryption{
	static SecureRandom sRandom = new SecureRandom();
	/**
	 * Encrypts and decrypts a file
	 * @param cipher The cipher that is used to process the file
	 * @param inStream Stream to the file to be processed
	 * @param outStream Stream to the file to be processed
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	private static void processFile(Cipher cipher,InputStream inStream,OutputStream outStream) throws IllegalBlockSizeException,BadPaddingException,IOException {
		byte[] inBuffer = new byte[1024];
		int length;
		while ((length = inStream.read(inBuffer)) != -1) {
			byte[] outBuffer = cipher.update(inBuffer, 0, length);
			if ( outBuffer != null )
				outStream.write(outBuffer);
		}
		byte[] outBuffer = cipher.doFinal();
		if ( outBuffer != null )
			outStream.write(outBuffer);
	}

	/**
	 * Generates a RSA keypair
	 * @return KeyPair The RSA keypair
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static KeyPair doGenkey(String username) throws NoSuchAlgorithmException,IOException {
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		keyPairGen.initialize(2048);
		KeyPair keyPair = keyPairGen.generateKeyPair();
		try (FileOutputStream outStream = new FileOutputStream("data/"+username+".pub")) {
			outStream.write(keyPair.getPublic().getEncoded());
		}
		return keyPair;
	}

	public static PrivateKey getPrivate(String filename) throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	public static PublicKey getPublic(String filename) throws Exception {
		byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(spec);
	}

	public PrivateKey getGroupKey(String keyPath) throws Exception {
		byte[] bytes = Files.readAllBytes(Paths.get(keyPath));
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey pubKey = keyFactory.generatePublic(keySpec);
		RSAPublicKeyImpl publicKey = (RSAPublicKeyImpl) pubKey;
		PrivateKey pvt = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(publicKey.getModulus(), publicKey.getPublicExponent()));
		return pvt;
	}

	/**
	 * Encrypts a file with an RSA key
	 * @param inputFile The file to be encrypted
	 * @param key The RSA key used to encrypt the file
	 * @return File The encrypted file is returned
	 * @throws Exception
	 */
	public static File encryptFile(File inputFile, String keyPath, String key)throws Exception{
		Cipher cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		byte[] bytes = Files.readAllBytes(Paths.get(keyPath));
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey pubKey = keyFactory.generatePublic(keySpec);
		cipherRSA.init(Cipher.ENCRYPT_MODE, pubKey);

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		SecretKey secretKey = keyGen.generateKey();

		byte[] iv = new byte[128/8];
		sRandom.nextBytes(iv);
		IvParameterSpec ivSpec = new IvParameterSpec(iv);

		try (FileOutputStream outStream = new FileOutputStream(inputFile + ".enc")) {
			byte[] fileBytes = cipherRSA.doFinal(secretKey.getEncoded());
			outStream.write(fileBytes);
			System.err.println("AES Key Length: " + fileBytes.length);
			outStream.write(iv);
			System.err.println("IV Length: " + iv.length);
			Cipher cipherAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipherAES.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
			try (FileInputStream inStream = new FileInputStream(inputFile)) {
				processFile(cipherAES, inStream, outStream);
			}
		}
		File encrypted = new File(inputFile+".enc");
		return (encrypted);
	}

	/**
	 * Decrypts a file with an RSA key
	 * @param inputFile The file to be decrypted
	 * @param key The RSA key used to decrypt the file
	 * @return File The decrypted file is returned
	 * @throws Exception
	 */
	public static File decryptFile(File inputFile, String keyPath, String key) throws Exception{
		File decrypted = null;
		Cipher cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(keyPath)));
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
		cipherRSA.init(Cipher.DECRYPT_MODE, privateKey);

		try (FileInputStream inStream = new FileInputStream(inputFile)) {
			SecretKeySpec secretKeySpec = null;
			byte[] fileBytes = new byte[256];
			inStream.read(fileBytes);
			byte[] keyBytes = cipherRSA.doFinal(fileBytes);
			secretKeySpec = new SecretKeySpec(keyBytes, "AES");
			byte[] iv = new byte[128/8];
			inStream.read(iv);
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			Cipher cipherAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipherAES.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
			decrypted = new File(inputFile.getPath().replace(".enc", ""));
			decrypted = setFileName(decrypted);
			try (FileOutputStream outStream = new FileOutputStream(decrypted)){
				processFile(cipherAES, inStream, outStream);
			}
		}
		return(decrypted);
	}

	/**
	 * Encrypts text with an RSA key
	 * @param text The text to be encrypted
	 * @param keyPath The path to the key used to encrypt the text
	 * @return byte[] The encrypted file is returned
	 * @throws Exception
	 */
	public static String encryptText(String msg, String keyPath) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, getPublic(keyPath));
		return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}

	/**
	 * Decrypts a file with an RSA key
	 * @param inputFile The file to be decrypted
	 * @param keyPath The path to the key used to decrypt the file
	 * @return File The decrypted file is returned
	 * @throws Exception
	 */
	public static String decryptText(String msg, String keyPath) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, getPrivate(keyPath));
		return new String(cipher.doFinal(Base64.getDecoder().decode(msg)), "UTF-8");
	}


	/**
	 * Sets the filename, adds a number within parentheses if a file with the same name already exits
	 * 
	 * @param file 
	 * @return a file with the new name
	 * @throws IOException
	 */
	public static File setFileName(File file) throws IOException {
		String filename = file.getPath();
		String simpleName = file.getPath().substring(0,filename.indexOf("."));            

		File newFile = new File(filename);
		int fileNo = 1;
		String newFileName = "";
		if (newFile.exists() && !newFile.isDirectory()) {
			while(newFile.exists()){
				newFileName = simpleName+"(" + fileNo++ + ")"+getExtension(file.getName());
				newFile = new File(newFileName);
			}
		} 
		return newFile;
	}

	/**
	 * Gets the extension of a filename
	 *
	 * @param name
	 * @return
	 */
	private static String getExtension(String name) {return name.substring(name.lastIndexOf(".")); }
}


