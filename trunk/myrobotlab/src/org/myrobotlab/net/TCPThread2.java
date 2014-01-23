package org.myrobotlab.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class TCPThread2 extends Thread {

	public final static Logger log = LoggerFactory.getLogger(TCPThread2.class);

	URI uri;
	Service myService;
	Socket socket = null;
	CommData data = new CommData();
	ObjectInputStream in = null;
	ObjectOutputStream out = null;
	boolean isRunning = false;

	public TCPThread2(Service service, URI url, Socket socket) throws UnknownHostException, IOException {
		super(String.format("%s_%s", service.getName(), url));
		this.myService = service;
		this.uri = url;
		if (socket == null) {
			socket = new Socket(url.getHost(), url.getPort());
		}
		this.socket = socket;
		out = new ObjectOutputStream((socket.getOutputStream()));
		out.flush();
		in = new ObjectInputStream(socket.getInputStream());
		this.start();
	}

	/**
	 * listening for inbound messages
	 */
	@Override
	public void run() {
		try {
			isRunning = true;

			while (socket != null && isRunning) {

				Message msg = null;
				Object o = null;

				o = in.readObject();
				msg = (Message) o;

				if (msg == null) {
					log.error("msg deserialized to null");
				} else {
					// FIXME - normalize to single method - check for data
					// type too ? !!!
					if (msg.method.equals("register")) {

						ServiceInterface sw = (ServiceInterface) msg.data[0];

						// IMPORTANT - (should be in Encoder) - create the
						// key for foreign service environment
						String mrlURI = String.format("mrl://%s/tcp://%s:%d", myService.getName(), socket.getInetAddress().getHostAddress(), socket.getPort());
						URI uri = new URI(mrlURI);

						// check if the URI is already defined - if not - we
						// will
						// send back the services which we want to export -
						// Security will filter appropriately
						ServiceEnvironment foreignProcess = Runtime.getServiceEnvironment(uri);
						
						// HMMM a vote for String vs URI here - since we
						// need to
						// catch syntax !!!
						sw.setHost(uri);

						// if security ... msg within msg
						// getOutbox().add(createMessage(Runtime.getInstance().getName(),
						// "register", inboundMsg));
						Runtime.register(sw, uri);// <-- not an INVOKE !!!
						
						if (foreignProcess == null) {
							// not defined we will send export
							// TODO - Security filters - default export
							// (include exclude) - mapset of name
							ServiceEnvironment localProcess = Runtime.getLocalServicesForExport();

							Iterator<String> it = localProcess.serviceDirectory.keySet().iterator();
							String name;
							ServiceInterface si;
							while (it.hasNext()) {
								name = it.next();
								si = localProcess.serviceDirectory.get(name);

								Message sendService = myService.createMessage("", "register", si);
								send(sendService);
							}

						}

						//  FIXME - I moved the registration before sending service - fix in XMPP !
					}
					++data.rx;
					myService.getOutbox().add(msg);
				}
			} // while

			// closing connections TODO - why wouldn't you close the others?
			in.close();
			out.close();

		} catch (Exception e) {
			isRunning = false;
			socket = null;
			Logging.logException(e);
		}

		releaseConnect();

		// connection has been broken
		// myService.invoke("connectionBroken", url); FIXME
	}

	public void releaseConnect() {
		try {
			log.error("removing {} from registry", uri);
			Runtime.release(uri);
			log.error("shutting down thread");
			isRunning = false;
			log.error("attempting to close streams");
			in.close();
			out.close();
			log.error("attempting to close socket");
			socket.close();
		} catch (Exception dontCare) {
		}
	}


	public synchronized void send(Message msg) {
		try {
			
			if (msg.data != null && msg.data[0] != null && msg.data[0] instanceof Service){
				log.info(String.format("serializing %s", ((Service)msg.data[0]).getName()));
			}
			out.writeObject(msg);
			out.flush();
			out.reset(); // magic line OMG - that took WAY TO LONG TO FIGURE
							// OUT !!!!!!!
			++data.tx;

		} catch (Exception e) {
			myService.error(e);
			releaseConnect();
		}
	}

}
