package org.myrobotlab.framework;

public class Error extends Throwable {
	private static final long serialVersionUID = 1L;
	
	public Error(){
		super();
	}
	
	public Error(String msg){		
		super(msg);
	}
	
	public Error(Throwable throwable){
		super(throwable);
	}

	public Error(String msg, Throwable throwable){
		super(msg, throwable);
	}

}
