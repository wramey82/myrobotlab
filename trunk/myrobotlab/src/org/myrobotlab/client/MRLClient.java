package org.myrobotlab.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceWrapper;

/**
 * MRLClient
 * class used to interface with a remote running instance or MyRobotLab.  The instance of MRL
 * must be running a RemoteAdapter service.  This class can send and recieve messages
 * from the MRL instance using only a few methods.  
 * The following code is an example of creating a client, registering a MRL instance,
 * sending a subscribe message to a TestCatcher service named "catcher01".
 * Whenever catcher01's catchInteger method is invoked a message will be sent back
 * to the MRL client.  And finally a "send" which sends a message to trigger the callback
 * event.
 * 
 * 		MRLClient api = new MRLClient();
 * 		Receiver client = new Receiver();
 *
 *		api.register("localhost", 6767, client);
 *		api.subscribe("catchInteger", "catcher01", "myMsg", Integer.TYPE);
 *		api.send("catcher01", "catchInteger", 5);
 *
 */
public class MRLClient implements Receiver {

	public final static Logger log = Logger.getLogger(MRLClient.class.getCanonicalName());
	DatagramSocket socket = null;
	UDPListener listener = null;

	// global
	String host = null;
	int port = -1;
	
	// inbound
	byte[] inBuffer = new byte[65535]; // datagram max size
	ByteArrayInputStream inByteStream = null;
	DatagramPacket inDataGram = null;
	Receiver client = null;

	// out bound
	ByteArrayOutputStream outByteStream = null;
	ObjectOutputStream outObjectStream = null;

	/**
	 * method which registers with a running MRL instance.  It does this by sending a service
	 * directory update to the MRL's RemoteAdapter.  From MRL's perspective it will appear
	 * as if this is another MRL instance with a single service.  The bogus service name
	 * is controlled by overriding Receiver.getMyName()
	 * 
	 * Once a MRLClient registers is may send messages and subscribe to events.
	 * 
	 * @param host - target host or ip of the running MRL instance's RemoteAdapter
	 * @param port - target port of the RemoteAdapter
	 * @param client - call back interface to recieve messages
	 * @return
	 */
	final public boolean register(String host, int port, Receiver client) {
		
		if (host == null || port < -1)
		{
			log.error("host and port need to be set for registering");
			return false;
		}
		
		// check if client is correct
		if (client.getMyName() == null) {
			log.error("client must return a non null String in \"getMyName\"");
			return false;
		}

		this.host = host;
		this.port = port;		
		this.client = client;
		socket = getSocket();
		
		try {
			ServiceDirectoryUpdate sdu = new ServiceDirectoryUpdate();
			sdu.serviceEnvironment = new ServiceEnvironment(sdu.remoteURL);
			// pushing bogus Service with name into SDU
			ServiceWrapper sw = new ServiceWrapper(client.getMyName(), null,
					sdu.serviceEnvironment);
			sdu.serviceEnvironment.serviceDirectory.put(client.getMyName(), sw);

			send(null, "registerServices", sdu);

			// start listening on the new socket
			listener = new UDPListener("udp_" + host + "_" + port);
			listener.start(); 
					
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * method to send a messages to a running MRL instances. @see register(String, int, Receiver) must
	 * be called before "send" can be used.
	 * 
	 * @param name - name of the service to receive the message (message destination)
	 * @param method - method to invoke
	 * @param data - parameter data for the method
	 * @return
	 */
	final synchronized public boolean send(String name, String method, Object... data) {
		
		Message msg = new Message();
		msg.name = name;
		msg.method = method;
		msg.sender = getMyName(); 
		msg.sendingMethod = "send";
		msg.data = data;

		// send it
		try {
			// ObjectStreams must be recreated
			outByteStream.reset();
			outObjectStream = new ObjectOutputStream(outByteStream); 
			outObjectStream.writeObject(msg);
			outObjectStream.flush();
			byte[] b = outByteStream.toByteArray();

			DatagramPacket packet = new DatagramPacket(b, b.length, InetAddress.getByName(host), port);

			if (socket == null)
			{
				log.error("socket is null... can not send messages");
				return false;
			}
			socket.send(packet);

		} catch (Exception e) {
			log.error("threw [" + e.getMessage() + "]");
			return false;
		}
		return true;
	}
	
	/**
	 * method to initialize the necessary data components for UDP communication
	 */
	private DatagramSocket getSocket()
	{		
		try {
			socket = new DatagramSocket();
			// inbound
			inByteStream = new ByteArrayInputStream(inBuffer);
			inDataGram = new DatagramPacket(inBuffer, inBuffer.length);
	
			// outbound
			outByteStream = new ByteArrayOutputStream();
		} catch (IOException e) {
			log.error(e.getMessage());
		}

		return socket;
	}

	class UDPListener extends Thread {
		
		boolean isRunning = false;

		public UDPListener(String n) {
			super(n);
		}

		public void shutdown() {
			if ((socket != null) && (!socket.isClosed())) {
				socket.close();
				socket = null;
			}
			
			isRunning = false;
			listener.interrupt();
			listener = null;
		}

		public void run() {

			try {
				log.info(getName() + " UDPListener listening on "
						+ socket.getLocalAddress() + ":"
						+ socket.getLocalPort());

				isRunning = true;

				while (isRunning) {
					socket.receive(inDataGram); // blocks
					ObjectInputStream o_in = new ObjectInputStream(inByteStream);
					try {
						Message msg = (Message) o_in.readObject();
						// must reset length field!
						inDataGram.setLength(inBuffer.length); 
						// reset so next read is from start of byte[] again
						inByteStream.reset(); 

						if (msg == null) {
							log.error("UDP null message");
						} else {

							// client API
							if (client != null) {
								client.receive(msg);
							}
						}

					} catch (ClassNotFoundException e) {
						log.error("ClassNotFoundException - possible unknown class send from MRL instance");
						log.error(e.getMessage());
					}
					inDataGram.setLength(inBuffer.length); // must reset length
															// field!
					inByteStream.reset(); // reset so next read is from start of
											// byte[]
					// again
				} // while isRunning

			} catch (Exception e) {
				log.error("UDPListener could not listen");
			}
		}
	}

	/**
	 * Subscribes to a remote MRL service method. When the method is called on
	 * the remote system an event message with return data is sent. It is
	 * necessary to registerForServices before subscribing.
	 * 
	 * @param outMethod - the name of the remote method to hook/subscribe to
	 * @param serviceName - service name of the remote service
	 * @param inMethod - inMethod can be used as an identifier
	 * @param paramTypes
	 */
	public void subscribe(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes) {
		MRLListener listener = new MRLListener(outMethod, getMyName(), inMethod,
				paramTypes);
		send(serviceName, "addListener", listener);
	}

	public void unsubscribe(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes) {
		MRLListener listener = new MRLListener(outMethod, getMyName(), inMethod,
				paramTypes);
		send(serviceName, "removeListener", listener);
	}
	
	@Override
	public void receive(Message msg) {
		log.info("received " + msg);
	}

	@Override
	public String getMyName() {
		return "mrlClient";
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		MRLClient client = new MRLClient();
		
		client.host = cmdline.getSafeArgument("-host", 0, "localhost");
		client.port = Integer.parseInt(cmdline.getSafeArgument("-host", 0, "6767"));
		String service = cmdline.getSafeArgument("-service", 0, "myService");
		String method = cmdline.getSafeArgument("-method", 0, "doIt");
		int paramCount = cmdline.getArgumentCount("-data");
		
		Object[] data = new Object[paramCount];
		
		for (int i = 0; i < paramCount; ++i)
		{
			try {
				Integer d = Integer.parseInt(cmdline.getSafeArgument("-data", i, ""));
				data[i] = d;
			} catch (Exception e) {
				data[i] = cmdline.getSafeArgument("-data", i, "");
			}
		}
		
		client.register(client.host, client.port, client);
		client.send(service, method, data);
	}

	static public String help() {
		return "java -jar MRLClient.jar -host [localhost] -port [6767] -service [myService] -method [doIt] -data \"data1\" \"data2\" \"data3\"... \n"
				+ "host: the name or ip of the instance of MyRobotLab which the message should be sent."
				+ "port: the port number which the foreign MyRobotLab is listening to."
				+ "service: the Service the message is to be sent."
				+ "method: the method to be invoked on the Service"
				+ "data: the method's parameters.";

	}
	
}