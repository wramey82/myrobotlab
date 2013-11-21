package org.myrobotlab.service;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

// http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/
// http://juliusdavies.ca/commons-ssl/ssl.html
// http://stackoverflow.com/questions/4319496/how-to-encrypt-and-decrypt-data-in-java

public class Security extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Security.class);
	

	public Security(String n) {
		super(n, Security.class.getCanonicalName());
	}

	@Override
	public void startService() {
		super.startService();
	}

	@Override
	public String getDescription() {
		return "security service";
	}
	
	//private HashMap<String, ByteArrayOutputStream> persistantStore = new HashMap<String, ByteArrayOutputStream>();
	
	private HashMap<String, byte[]> keys = new HashMap<String, byte[]>();
	
	private String storeLocation = null;
	
	/*
	public boolean loadKeyStore(String location)
	{
		
	}
	*/
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
	
			Serializable s = new SerializableImage(null, null);
		
		
			// passphrase - key
			// A better way to create a key is with a SecretKeyFactory using a salt:

			String passphrase = "correct horse battery staple";
			MessageDigest digest = MessageDigest.getInstance("SHA");
			digest.update(passphrase.getBytes());
			SecretKeySpec key = new SecretKeySpec(digest.digest(), 0, 16, "AES");
			// 

			Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.ENCRYPT_MODE, key);
			byte[] ciphertext = aes.doFinal("my cleartext".getBytes());
			log.info(new String(ciphertext));

			aes.init(Cipher.DECRYPT_MODE, key);
			String cleartext = new String(aes.doFinal(ciphertext));
			
			log.info(cleartext);

		} catch (Exception e) {
			Logging.logException(e);
		}

		Security security = new Security("security");
		security.startService();

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
