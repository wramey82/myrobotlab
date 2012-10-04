package org.myrobotlab.service.interfaces;

import java.net.URI;

import org.myrobotlab.framework.Message;

public interface CommunicationInterface {

	public void send(final Message msg);
	public void send(final URI url, final Message msg);
	public void setComm(final Communicator comm);
	public Communicator getComm();
	
}
