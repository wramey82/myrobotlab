package org.myrobotlab.client;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.data.PinData;

public class MRLClient implements Runnable {

	public final static Logger LOG = Logger.getLogger(MRLClient.class.getCanonicalName());

	private String host 			= "localhost";
	private int port 				= 6767;
	private String serviceName 		= "myService";
	private String method			= "doIt";
	private Object[] data 			= null;
	
	// TODO - specify protocol and transport TCP UDP SOAP XML Native etc
	
	/**
	 * 
	 * The method to send a message from an application to a running instance of MyRobotLab.
	 * TODO - examples
	 * 
	 * @param host - hostname or ip of computer which is running a MyRobotLab instance
	 * @param port - port which the MyRobotLab instance is listening too
	 * @param serviceName - destination service name
	 * @param method - method to invoke
	 * @param params - parameters
	 * @return
	 */
	final public boolean sendMessage(final String host, final int port,
			final String serviceName, final String method, final Object... data) 
	{
		// caching data
		this.host = host;
		this.port = port;
		this.serviceName = serviceName;
		this.method = method;
		this.data = data;
		
		return sendMessage();		
	}

	public boolean sendMessage(String serviceName, String method,
			Object... params) {
		return sendMessage(host, port, serviceName, method, params);
	}

	public boolean sendMessage(String method, Object... params) 
	{
		return sendMessage(host, port, serviceName, method, params);
	}		
	
	/**
	 * @return
	 */
	final public boolean sendMessage()
	{
		// create message - status or control
		Message msg = new Message();
		msg.name = serviceName;
		msg.method = method;
		msg.sender = "MRLClient"; // TODO variable
		msg.sendingMethod = "sendMessage"; // TODO variable
		msg.data = data;

		// send it
		try {

			DatagramSocket socket = new DatagramSocket();
			ByteArrayOutputStream b_out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(b_out);

			oos.writeObject(msg);
			oos.flush();
			byte[] b = b_out.toByteArray();

			DatagramPacket packet = new DatagramPacket(b, b.length,
					InetAddress.getByName(host), port);

			socket.send(packet);
			oos.reset();

		} catch (Exception e) {
			LOG.error("threw [" + e.getMessage() + "]");
			return false;
		}

		return true;
		
	}	
	
	// TODO remove notify request - change to addListener / removeListener
	// TODO - change name to addListenerRequest
	// TODO - removeListenerRequest
	public boolean sendNotifyRequest(String host, int port,
			String serviceName, String outMethod, String name, String inMethod,
			Class<?>[] paramTypes) {
		NotifyEntry ne = new NotifyEntry(outMethod, name, inMethod, paramTypes);
		return sendMessage(host, port, serviceName, "notify",
				new Object[] { ne });
	}

	public boolean sendNotifyRequest(String serviceName,
			String outMethod, String name, String inMethod,
			Class<?>[] paramTypes) {
		return sendNotifyRequest("localhost", 6767, serviceName, outMethod,
				name, inMethod, paramTypes);
	}

	private volatile Thread flag;

	public void stop() {
		flag = null;
	}

	public void run() {
		Thread thisThread = Thread.currentThread();
		flag = thisThread;
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		// service creation
		RemoteAdapter remote = new RemoteAdapter("remote");
		remote.startService();

		Random dummyData = new Random();
		PinData pinData = new PinData();


		// set up a message route from sensors to the GUI
		// this has nothing to do with the functionality of the SensorMonitor
		// its just setting up graphics display - this could
		// be done on the MRL side.. but I just decided to do it here
		
		/*
		MRLClient.sendNotifyRequest("sensors", "publishSensorData", "gui",
				"inputSensorData", new Class<?>[] { PinData.class });
		*/
		pinData.pin = 2;
		pinData.source = "mySource";
		pinData.function = 17;
	    pinData.value = 500;
       //pinData.value = (int) PhysicsCar_use_For_Testing.compVal;
		
		
	   
	   
	   
	   // the sensor monitor will need a setup call - to add trace data
	    /*
		MRLClient.sendMessage("sensors", "addTraceData",
				new Object[] { pinData });
				*/

		// now just send the data !
		//for (int i = 0; i < 1000; ++i) {
		while (flag == thisThread) {
	
		//pinData.value += (2 - dummyData.nextInt(4));
			//pinData.value = map((int) MRL_PhysicsCar_Threads.compVal, -180, 180, 50, 550);
			/*
			MRLClient.sendMessage("sensors", "sensorInput",
					new Object[] { pinData });
					*/
			System.out.println("++++++++++++++++++ RIGHT HERE +++++++++++++++++++");
			//System.out.println( PhysicsCar_use_For_Testing.compVal);
			System.out.println( pinData.value);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				 e.printStackTrace();
			}
		}//end while flag		
		remote.releaseService();
	}//end run			
	
	static public String help() {
		return "java -jar MRLClient.jar -host [localhost] -port [6767] -service [myService] -method [doIt] -data \"data1\" \"data2\" \"data3\"... \n"
				+ "host: the name or ip of the instance of MyRobotLab which the message should be sent."
				+ "port: the port number which the foreign MyRobotLab is listening to."
				+ "service: the Service the message is to be sent."
				+ "method: the method to be invoked on the Service"
				+ "data: the method's parameters."
				;
		
	}
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);
		
	}
	
}//end class
