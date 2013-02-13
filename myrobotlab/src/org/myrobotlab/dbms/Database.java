/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.dbms;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;


public class Database {

	// public static enum RDBMSTypes {MYSQL, MSSQL} -- can't use it too
	// restrictive
	// RDBMSTypes DatabaseTypes;
	// public RDBMSTypes RDBMSType;

	private java.sql.Connection Connection_ = null;

	private DatabaseConfig Config_ = new DatabaseConfig();

	private final static Logger log = LoggerFactory.getLogger(Database.class);

	public String getConnectionUrl() {
		if (Config_.RDBMSType.compareTo("MYSQL") == 0) // TODO enum here
		{
			Config_.URL_ += (Config_.driverURL);
			Config_.URL_ += (Config_.hostname);
			Config_.URL_ += (":");
			Config_.URL_ += (Config_.portNumber);
			Config_.URL_ += ("/");
			Config_.URL_ += (Config_.databaseName_);
		} else if (Config_.RDBMSType.compareTo("MSSQL") == 0) { // TODO emum
																// here
			Config_.URL_ += (Config_.driverURL);
			Config_.URL_ += (Config_.hostname);
			Config_.URL_ += (":");
			Config_.URL_ += (Config_.portNumber);
			Config_.URL_ += (";databaseName=");
			Config_.URL_ += (Config_.databaseName_);
			Config_.URL_ += (";selectMethod=");
			Config_.URL_ += (Config_.SelectMethod_);
			Config_.URL_ += (";");
		}

		return Config_.URL_;
	}

	public Database(final DatabaseConfig config) {
		Config_ = config;
	}

	// TODO - enum for RDBMSType would be good here as an input param
	public Database(final String HostName, final String databaseName, final String UserName, final String Password, final String RDBMSType) {
		Config_.hostname = HostName;
		Config_.databaseName_ = databaseName;
		Config_.userName = UserName;
		Config_.Password_ = Password;
		Config_.RDBMSType = RDBMSType;

		if (Config_.RDBMSType.compareTo("MSSQL") == 0) {
			Config_.driverURL = "jdbc:microsoft:sqlserver://";
			Config_.portNumber = 1433;
			Config_.driverName = "com.microsoft.jdbc.sqlserver.SQLServerDriver";
		}

	}

	public Database(final String HostName, final String databaseName, final String UserName, final String Password) {
		this(HostName, databaseName, UserName, Password, "MYSQL"); // TODO -
																	// enum
																	// would be
																	// nice here
	}

	public java.sql.Connection getConnection() {
		try {
			Class.forName(Config_.driverName);
			log.info(getConnectionUrl());
			Connection_ = java.sql.DriverManager.getConnection(getConnectionUrl(), Config_.userName, Config_.Password_);
		} catch (Exception e) {
			log.error(getConnectionUrl());
			e.printStackTrace();
			System.out.println("Error Trace in getConnection() : " + e.getMessage());
		}
		return Connection_;
	}

}
