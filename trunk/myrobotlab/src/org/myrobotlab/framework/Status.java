package org.myrobotlab.framework;

import java.io.Serializable;

public class Status implements Serializable {
	
	public final static String DEBUG = "DEBUG";
	public final static String INFO = "INFO";
	public final static String WARN = "WARN";
	public final static String ERROR = "ERROR";
	
	public String name;
	public String level;
	public String code;
	public String detail;

	public Status(String name, String level, String code, String detail)
	{
		this.name = name;
		this.level = level;
		this.code = code;
		this.detail = detail;
	}
	
	public Status(String detail)
	{
		this.detail = detail;
	}
	
	public boolean isDebug() {return DEBUG.equals(level);}
	public boolean isInfo() {return INFO.equals(level);}
	public boolean isWarn() {return WARN.equals(level);}
	public boolean isError() {return ERROR.equals(level);}
}
