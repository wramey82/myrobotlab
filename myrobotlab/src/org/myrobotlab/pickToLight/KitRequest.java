package org.myrobotlab.pickToLight;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

//@XmlRootElement(namespace = "de.vogella.xml.jaxb.model")
@XmlRootElement//(name = "KitElement")

public class KitRequest {
	
	// XmLElementWrapper generates a wrapper element around XML representation
	 // @XmlElementWrapper(name = "bookList")
	  // XmlElement sets the name of the entities
	  @XmlElement//(name = "list")
	ArrayList<KitElement> list;

}
