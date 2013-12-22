package org.myrobotlab.pickToLight;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "KitElement")
//If you want you can define the order in which the fields are written
//Optional
//@XmlType(propOrder = { "address", "name", "publisher", "isbn" })
public class KitElement {
	
	String address;
	Integer quantity;

}
