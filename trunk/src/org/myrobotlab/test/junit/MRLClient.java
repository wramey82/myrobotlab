package org.myrobotlab.test.junit;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.service.data.PinData;

public class MRLClient {
	
	public final static Logger LOG = Logger.getLogger(MRLClient.class.getCanonicalName());

	public static boolean sendMessage (String host, int port, String serviceName, String method, Object ... params)
	{

		// create message - status or control
		Message msg = new Message();
		msg.name = serviceName;
		msg.method = method;
		msg.sender = "MRLClient";
		msg.sendingMethod = "sendMessage";
		msg.data = params;
		
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

	public static boolean sendMessage(String serviceName, String method, Object ... params)
	{
		return sendMessage ("localhost", 6767, serviceName, method, params);
	}
		
	public static boolean sendNotifyRequest (String host, int port, String serviceName, String outMethod, String name, String inMethod, Class<?>[] paramTypes)
	{
		NotifyEntry ne = new NotifyEntry(outMethod, name, inMethod, paramTypes);
		return sendMessage(host, port, serviceName, "notify", new Object[]{ne});
	}

	public static boolean sendNotifyRequest (String serviceName, String outMethod, String name, String inMethod, Class<?>[] paramTypes)
	{
		return sendNotifyRequest("localhost", 6767, serviceName, outMethod, name, inMethod, paramTypes);
	}
	
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		Random dummyData = new Random();
		PinData pinData = new PinData();

		// set up a message route from sensors to the GUI
		// this has nothing to do with the functionality of the SensorMonitor
		// its just setting up graphics display - this could
		// be done on the MRL side.. but I just decided to do it here
		MRLClient.sendNotifyRequest ("sensors",  "publishSensorData", "gui", "inputSensorData", new Class<?>[]{PinData.class});

		pinData.pin = 2;
		pinData.source = "mySource";
		pinData.function = 17;
		pinData.value = 500;

		// the sensor monitor will need a setup call - to add trace data
		MRLClient.sendMessage("sensors", "addTraceData", new Object[]{pinData});
		
		// now just send the data !
		for (int i = 0; i < 10000; ++i)
		{			
			pinData.value = 500 +  (2-dummyData.nextInt(4));			
			MRLClient.sendMessage("sensors", "sensorInput", new Object[]{pinData});
		}

	}
}
