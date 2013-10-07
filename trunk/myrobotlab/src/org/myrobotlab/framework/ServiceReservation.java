package org.myrobotlab.framework;


/**
 * ServiceRegistration is a data object containing information regarding
 * a "peer" service within a "composite" service.
 * The Composite service utilizes or controls multiple peer services.  In
 * order to do so it needs an internal key used only by the composite service.
 * With this key the composite gets a reference to the peer.  In some situations
 * it will be necessary to use a peer with a different name.
 * reserveAs is a method which will re-bind the composite to a differently named
 * peer service.
 *
 */
public class ServiceReservation {
	/**
	 * 
	 */
	public String key;
	public String actualName;
	public String simpleTypeName;
	public String comment;
	
	public ServiceReservation(String key, String simpleTypeName, String comment)
	{
		this.key = key;
		this.actualName = key;
		this.simpleTypeName = simpleTypeName;
		this.comment = comment;
	}
}
