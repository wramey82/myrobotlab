package org.myrobotlab.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.framework.Service;
import org.myrobotlab.memory.Node;

// CANNOT BE SERIALIZED YET !!!!!! - 
public class Serializer extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Serializer.class.getCanonicalName());

	public Serializer(String n) {
		super(n, Serializer.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	public boolean store(Node n) {

		Connection conn = null;
		Statement stmt = null;
		try {
			// STEP 2: Register JDBC driver
			Class.forName("com.mysql.jdbc.Driver");

			// STEP 3: Open a connection
			System.out.println("Connecting to a selected database...");
			String connectionURL = "jdbc:mysql://" + host + "/" + database;
			conn = DriverManager.getConnection(connectionURL, user, password);
			System.out.println("Connected database successfully...");

			// STEP 4: Execute a query
			System.out.println("Inserting records into the table...");
			stmt = conn.createStatement();

			String sql = "INSERT INTO Registration " + "VALUES (100, 'Zara', 'Ali', 18)";
			stmt.executeUpdate(sql);

		} catch (SQLException se) {
			// Handle errors for JDBC
			logException(se);
		} catch (Exception e) {
			// Handle errors for Class.forName
			logException(e);
		} finally {
			// finally block used to close resources
			try {
				if (stmt != null)
					conn.close();
			} catch (SQLException se) {
			}// do nothing
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				logException(se);
			}// end finally try
		}// end try
		return true;
	}

	@Override
	public String getToolTip() {
		return "used as a general serializer";
	}

	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	String host = null;
	String database = null;
	String user = null;
	String password = null;

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		Serializer serializer = new Serializer("serializer");

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		if (!cmdline.containsKey("-user") || !cmdline.containsKey("-password")) {
			serializer.user = cmdline.getSafeArgument("-user", 0, "");
			serializer.password = cmdline.getSafeArgument("-password", 0, "");
			serializer.host = cmdline.getSafeArgument("-host", 0, "");
			serializer.database = cmdline.getSafeArgument("-database", 0, "");
		}

		Node n = new Node();
		n.word = "hand";
		serializer.store(n);
		serializer.startService();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */

	}

}
