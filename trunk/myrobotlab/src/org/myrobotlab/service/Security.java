package org.myrobotlab.service;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

// http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/
// http://juliusdavies.ca/commons-ssl/ssl.html
// http://stackoverflow.com/questions/4319496/how-to-encrypt-and-decrypt-data-in-java

public class Security extends Service implements AuthorizationProvider {

	private static final long serialVersionUID = 1L;
	
	// TODO - concept (similar in Drupal) - anonymous, authenticated, admin .. default groups ?
	transient private static final HashMap <String,Boolean> allowExportByName = new HashMap <String,Boolean>();
	transient private static final HashMap <String,Boolean> allowExportByType = new HashMap <String,Boolean>();
	
	public final static Logger log = LoggerFactory.getLogger(Security.class);
	
	// below is authorization 
	transient private static final HashMap <String,Group> groups = new HashMap <String,Group>();
	// users only map to groups - groups have the only access rules
	transient private static final HashMap <String,User> users = new HashMap <String,User>();
	
	// many to 1 mapping - currently does not support many to many Yay !
	// transient private static final HashMap <String,String> userToGroup = new HashMap <String,String>();

	//transient private boolean defaultAccess = true;
	
	transient private boolean defaultAllowExport = true;

	private String defaultNewGroupId = "anonymous";
	
	public static class User
	{
		// timestamp - single access login
		public String userId;
		public String password; // encrypt
		public String groupId; // support only 1 group now Yay !
	}
	
	public static class Group
	{
		// TODO - single access login
		// timestamp - 
		public String groupId;
		public boolean defaultAccess = true;
		public HashMap<String, Boolean> accessRules = new HashMap<String, Boolean>();
	}
	
	public void createDefaultGroups()
	{
		Group g = new Group();
		g.groupId = "anonymous";
		g.defaultAccess = false;
		groups.put("anonymous", g);
		
		g = new Group();
		g.groupId = "authenticated";
		g.defaultAccess = true;
		groups.put("authenticated", g);
	}
	
	public Security(String n) {
		super(n);
		createDefaultGroups();
		
		allowExportByType.put("XMPP", false);
		allowExportByType.put("RemoteAdapter", false);
		allowExportByType.put("WebGUI", false);

		allowExportByType.put("Java", false);
		allowExportByType.put("Python", false);
		
		allowExportByType.put("Security", false);
		allowExportByType.put("Runtime", false);

		setSecurityProvider(this);
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
	
	public boolean setDefaultNewGroupId(String userId, String groupId)
	{
		if (!users.containsKey(userId))
		{
			log.error(String.format("user %s does not exist can not change groupId", userId));
			return false;
		}
		
		
		if (!groups.containsKey(groupId))
		{
			log.error(String.format("group %s does not exist can not change groupId", groupId));
			return false;
		}
		
		users.get(userId).groupId = groupId;
		
		return false;
		
	}
	
	
	@Override
	public boolean allowExport(String serviceName) {
		
		if (allowExportByName.containsKey(serviceName)){
			return allowExportByName.get(serviceName);
		}
		
		ServiceInterface si = Runtime.getService(serviceName);

		if (si == null)
		{
			error("%s could not be found for export", serviceName);
			return false;
		}
		
		String fullType = si.getClass().getCanonicalName();
		if (allowExportByType.containsKey(fullType)){
			return allowExportByType.get(fullType);
		}
		return defaultAllowExport;
	}
	
	public Boolean allowExportByName(String name, Boolean access)
	{
		return allowExportByName.put(name, access);
	}
	
	public Boolean allowExportByType(String type, Boolean access)
	{
		return allowExportByType.put(Encoder.type(type), access);
	}


	@Override
	public boolean isAuthorized(Message msg) {
		return isAuthorized(msg.security, msg.name, msg.method);
	}
	

	@Override
	public boolean isAuthorized(HashMap<String, String> security, String serviceName, String method) {

		/* check not needed
		if (security == null)
		{
			// internal messaging
			return defaultAccess;
		}
		*/
		
		// TODO - super cache Radix Tree ??? super key --  uri user:password@mrl://someService/someMethod - not found | ALLOWED || DENIED
		
		// user versus binary token
		if (security.containsKey("user")) // && password || token
		{
			String fromUser = security.get("user");
			
			// user scheme found - get the group
			if (!users.containsKey(fromUser)){
				// invoke UserNotFound / throw
				return false;
			} else {
			
				User user = users.get(fromUser);
				// check MD5 hash of password
				// FIXME OPTIMIZE - GENERATE KEY user.group.accessRule - ALLOW ?
				// I'm looking for a specific object method - should have that key
				if (!groups.containsKey(user.groupId)) // FIXME - optimize only need a group look up not a user l
				{
					// credentials supplied - no match
					// invoke Group for this user not found
					return false;
				} else {
					// credentials supplied - match - check access rules
					Group group = groups.get(user.groupId);
					// make message key
					// service level
					if (group.accessRules.containsKey(serviceName)){
						return group.accessRules.get(serviceName);
					}
					
					// method level
					String methodLevel = String.format("%s.%s", serviceName, method);
					if (group.accessRules.containsKey(methodLevel)){
						return group.accessRules.get(methodLevel);
					}
					
					return group.defaultAccess;
				}
			}
			
		} else {
			// invoke UnavailableSecurityScheme
			return false;
		}
	}


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
	
			//Serializable s = new SerializableImage(null, null);
		
		
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
	
	// default group permissions - for new user/group
	// anonymous
	// authenticated

	public boolean setGroup(String userId, String groupId){
		if (!users.containsKey(userId)){
			log.error(String.format("user %s does not exist", userId));
			return false;
		}
		if (!groups.containsKey(groupId)){
			log.error(String.format("group %s does not exist", groupId));
			return false;
		}
		
		User u = users.get(userId);
		u.groupId = groupId;
		return true;
	}
	
	public boolean addUser(String user) {
		return addUser(user, null, null);
	}

	
	public boolean addUser(String userId, String password, String groupId) {
	
		if (users.containsKey(userId))
		{
			log.warn(String.format("user %s already exists", userId));
			return false;
		}
		User u = new User();
		u.userId = userId;
		u.password = password;
		if (groupId == null)
		{
			u.groupId = defaultNewGroupId;
		} else {
			u.groupId = groupId;
		}
		if (!groups.containsKey(u.groupId)){
			log.error(String.format("could not add user %s groupId %s does not exist", userId, groupId));
			return false;
		}
		users.put(userId, u);
		return true;
	}
	
	public boolean addGroup(String groupId){
		return addGroup(groupId, false);
	}
	
	public boolean addGroup(String groupId, boolean defaultAccess)
	{
		Group g = new Group();
		g.groupId = groupId;
		g.defaultAccess = defaultAccess;
		
		if (groups.containsKey(groupId))
		{
			log.warn(String.format("group %s already exists", groupId));
			return false;
		}
		
		groups.put(groupId, g);
		return true;
	}

	
}
