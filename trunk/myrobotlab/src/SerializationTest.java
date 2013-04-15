import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Clock;
import org.myrobotlab.service.RemoteAdapter;
import org.slf4j.Logger;

public class SerializationTest implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(RemoteAdapter.class.getCanonicalName());

	
	//Socket socket;
	ServerSocket serverSocket;
	ObjectInputStream in = null;
	ObjectOutputStream out = null;
	ObjectInputStream sin = null;
	ObjectOutputStream sout = null;
	boolean isRunning = true;
	Socket socket;
	Socket client;
	
	public void test() {
		try {

			Server s = new Server();
			s.start();
			
			client = new Socket("192.168.0.73", 6767);
			out = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
			out.flush();// some flush before using :)
			// http://stackoverflow.com/questions/3365261/does-a-buffered-objectinputstream-exist
			// in = new ObjectInputStream(socket.getInputStream());
			in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));

			Clock c = new Clock("boo");
			c.data = "HELLO WORLD";
			c.isClockRunning = true;
			Message msg = new Message();
			msg.data = new Object[]{c};
			out.writeObject(c);
			out.flush();
			
			// Message msg = null;
			Object o = null;
			o = in.readObject();

			// msg = (Message) o;
		} catch (Exception e) {
			Logging.logException(e);
		}

	}
	
	public  class Server extends Thread
	{
		public void run()
		{
			try {
		   serverSocket = new ServerSocket(6767, 10);
			
			while (isRunning) {
				socket = serverSocket.accept();
				URI url = new URI("tcp://" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
				log.info("new connection [{}]", url);
				sout = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				sout.flush();// some flush before using :)
				sin = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				
				Object o = null;
				o = sin.readObject();
				log.info("reconstituted o {}", o);
			}
			
			} catch(Exception e) 
			{
				Logging.logException(e);
			}			

		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		SerializationTest test = new SerializationTest();
		test.test();
	}

}
