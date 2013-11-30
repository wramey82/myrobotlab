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

package org.myrobotlab.control.widget;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.myrobotlab.framework.ServiceEntry;

public class InteractiveTableModel extends AbstractTableModel {
	/**
	 * String[] columnNamesx = {"name","class","status","category",
	 * "method","direction","lastModified","dataClass"};
	 */
	private static final long serialVersionUID = 1L;
	public static final int HOSTNAME_INDEX = 0;
	public static final int SERVICEPORT_INDEX = 1;
	public static final int NAME_INDEX = 2;
	public static final int CLASS_INDEX = 3;
	public static final int STATUS_INDEX = 4;
	public static final int CATEGORY_INDEX = 5;
	public static final int METHOD_INDEX = 6;
	public static final int DIRECTION_INDEX = 7;
	public static final int LASTMODIFIED_INDEX = 8;
	public static final int DATACLASS_INDEX = 9;
	public static final int HIDDEN_INDEX = 10;

	protected String[] columnNames;
	protected Vector dataVector;

	public InteractiveTableModel(String[] columnNames, Vector dataVector) {
		this.columnNames = columnNames;
		this.dataVector = dataVector;
	}

	public String getColumnName(int column) {
		return columnNames[column];
	}

	public boolean isCellEditable(int row, int column) {
		if (column == HIDDEN_INDEX)
			return false;
		else
			return true;
	}

	public Class getColumnClass(int column) {
		switch (column) {
		case HOSTNAME_INDEX:
		case SERVICEPORT_INDEX:
		case NAME_INDEX:
		case CLASS_INDEX:
		case STATUS_INDEX:
		case CATEGORY_INDEX:
		case METHOD_INDEX:
		case DIRECTION_INDEX:
		case LASTMODIFIED_INDEX:
		case DATACLASS_INDEX:
			return String.class;
		default:
			return Object.class;
		}
	}

	public Object getValueAt(int row, int column) {
		ServiceEntry record = (ServiceEntry) dataVector.get(row);
		switch (column) {
		case HOSTNAME_INDEX:
			return record.host;
		case SERVICEPORT_INDEX:
			return record.servicePort;
		case NAME_INDEX:
			return record.name;
		case CLASS_INDEX:
			return record.serviceClass;
		case STATUS_INDEX:
			return "";
		case CATEGORY_INDEX:
			return "";
		case METHOD_INDEX:
			return "";// record.method;
		case DIRECTION_INDEX:
			return "";// record.direction_;
		case LASTMODIFIED_INDEX:
			return record.lastModified;
		case DATACLASS_INDEX:
			return "";// record.dataClass;
		default:
			return new Object();
		}
	}

	public void setValueAt(Object value, int row, int column) {
		ServiceEntry record = (ServiceEntry) dataVector.get(row);
		switch (column) {
		case HOSTNAME_INDEX:
			record.host = (String) value;
			break;
		case SERVICEPORT_INDEX:
			// record.servicePort = (String)value;
			break;
		case NAME_INDEX:
			record.name = (String) value;
			break;
		case CLASS_INDEX:
			record.serviceClass = (String) value;
			break;
		case STATUS_INDEX:
			// record.status = (String)value;
			break;
		case CATEGORY_INDEX:
			// record.category_ = (String)value;
			break;
		case METHOD_INDEX:
			// record.method = (String)value;
			break;
		case DIRECTION_INDEX:
			// record.direction_ = (String)value;
			break;
		case LASTMODIFIED_INDEX:
			// record.lastModified = (String)value;
			break;
		case DATACLASS_INDEX:
			// record.dataClass = (String)value;
			break;
		default:
			System.out.println("invalid index");
		}
		fireTableCellUpdated(row, column);
	}

	public int getRowCount() {
		return dataVector.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public boolean hasEmptyRow() {
		if (dataVector.size() == 0)
			return false;
		ServiceEntry audioRecord = (ServiceEntry) dataVector.get(dataVector.size() - 1);
		if (audioRecord.host == "" &&
		// audioRecord.servicePort.equals("") &&
				audioRecord.name.equals("") && audioRecord.serviceClass.equals("") // &&
		// audioRecord.status.equals("") &&
		// audioRecord.category_.equals("")&&
		// audioRecord.method.equals("") &&
		// audioRecord.direction_.equals("") &&
		// audioRecord.lastModified.equals("") &&
		// audioRecord.dataClass.equals("")
		) {
			return true;
		} else
			return false;
	}

	public void addEmptyRow() {
		dataVector.add(new ServiceEntry());
		fireTableRowsInserted(dataVector.size() - 1, dataVector.size() - 1);
	}

	public void set(Vector v) {
		dataVector = v;
		fireTableStructureChanged();
	}

	public void add(ServiceEntry se) {
		dataVector.add(se);
		fireTableRowsInserted(dataVector.size() - 1, dataVector.size() - 1);
	}
}
