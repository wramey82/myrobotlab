package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Message;

public interface CommunicationInterface {

	public void send(final Message msg);
	public void setComm(final Communicator comm);
	public Communicator getComm();
	
	//void sendLocal(Message msg);
	
}
