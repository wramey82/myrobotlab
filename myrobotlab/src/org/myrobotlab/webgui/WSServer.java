package org.myrobotlab.webgui;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.WebGUI;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class WSServer extends WebSocketServer {

	public final static Logger log = LoggerFactory.getLogger(WSServer.class.getCanonicalName());

	private Outbox outbox;
	private WebGUI webgui;
	
	public WSServer( WebGUI webgui, int port) throws UnknownHostException {
		super( new InetSocketAddress( port ) );
		this.webgui = webgui;
		this.outbox = webgui.getOutbox();
	}

	public WSServer( InetSocketAddress address ) {
		super( address );
	}
	
	// FIXME - SEND MESSAGES TO ALL NOTIFYING OF CONNECTIONS !!!

	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		String clientkey = String.format("%s:%d", conn.getRemoteSocketAddress().getAddress().getHostAddress(), conn.getRemoteSocketAddress().getPort());
		log.info(clientkey);
		webgui.clients.put(clientkey, clientkey);
		// this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		String clientkey = String.format("%s:%d", conn.getRemoteSocketAddress().getAddress().getHostAddress(), conn.getRemoteSocketAddress().getPort());
		webgui.clients.remove(clientkey);
		//this.sendToAll( conn + " has left the room!" );
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		//this.sendToAll( message );
		//System.out.println( conn.getLocalSocketAddress() + ": " + message );
		//System.out.println("[" + message + "]" );
		log.info("webgui <---to--- client {}", message);
		Gson gson = new Gson();	
		//Gson gson = new GsonBuilder().setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").create();
		Message msg = gson.fromJson(message, Message.class);
		//log.info("{}",msg);
		log.info("parsed message");
		outbox.add(msg);
	}

	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}
	
	

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	// FIXME - on release all - this throws an exception and doesn't complete - but is it worth
	// the overhead of a try???
	public void sendToAll( String text ) {
		Collection<WebSocket> con = connections();
		synchronized ( con ) {
			for( WebSocket c : con ) {
				c.send( text );
			}
		}
	}

	/*
	public static void main( String[] args ) throws InterruptedException , IOException {
		WebSocketImpl.DEBUG = true;
		int port = 8887; // 843 flash policy port
		try {
			port = Integer.parseInt( args[ 0 ] );
		} catch ( Exception ex ) {
		}
		WSServer s = new WSServer(null, port);
		s.start();
		System.out.println( "WSServer started on port: " + s.getPort() );

		BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
		while ( true ) {
			String in = sysin.readLine();
			s.sendToAll( in );
		}
	}
	*/
}
