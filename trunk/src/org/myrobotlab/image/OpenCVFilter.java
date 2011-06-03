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

package org.myrobotlab.image;

import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashMap;
import java.lang.reflect.*;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public abstract class OpenCVFilter implements Serializable{

	public final static Logger LOG = Logger.getLogger(OpenCVFilter.class.toString());

	private static final long serialVersionUID = 1L;
	ConfigurationManager cfg = null; // TODO - remove
	final public String name;
	OpenCV myService = null; // transient?

	final static public String INPUT_IMAGE_NAME = "inputImageName";
	final static public String OUTPUT_IMAGE_NAME = "outputImageName";
	final static public String USE_INPUT_IMAGE_NAME = "useInputImageName";
	final static public String USE_OUTPUT_IMAGE_NAME = "useOutputImageName";
	final static public String ROI_NAME = "roiName";
	final static public String USE_ROI = "useROI";

	static HashMap<String, Object> globalData = new HashMap<String, Object>();

	//static HashMap<String, IplImage> imageMap = new HashMap<String, IplImage>();
	
	public static final String FILTER_CFG_ROOT = "displayFilter/filter/";
	public final String FILTER_INSTANCE_CFG_ROOT;

	public OpenCVFilter(OpenCV service, String filterName) {
		this.name = filterName;
		this.myService = service;
		this.FILTER_INSTANCE_CFG_ROOT = myService.getCFG().getServiceRoot() + "/" + FILTER_CFG_ROOT  + name;
		cfg = new ConfigurationManager(FILTER_INSTANCE_CFG_ROOT);
		cfg.set(INPUT_IMAGE_NAME, "output");
		cfg.set(OUTPUT_IMAGE_NAME, "output");
		cfg.set(USE_INPUT_IMAGE_NAME, false);
		cfg.set(USE_OUTPUT_IMAGE_NAME, false);
		cfg.set(ROI_NAME, "roi");
		cfg.set(USE_ROI, false);
	}

	public void copyDataFrom (OpenCVFilter filter)
	{
		if (this == filter)
		{   // data is myself - operating on local copy
			return;
		}
		
	      Class<?> filterData = filter.getClass();
	      Class<?> activeFilter = this.getClass();
	      //Field fields[] = filterData.getDeclaredFields();
	      Field fields[] = filterData.getDeclaredFields();
	      for (int j = 0, m = fields.length; j < m; j++) {
	    	  try {
	    		  //if (fields[j].)
	    		  Field f = fields[j];

	    		  
	    		  if (Modifier.isPublic(f.getModifiers()) 
	    				  && !(f.getName().equals("LOG"))
	    				  && !Modifier.isTransient(f.getModifiers()))
	    		  {
	    			  	    			  
		    		  Type t = f.getType();
	    			  
	    			  //LOG.info(Modifier.toString(f.getModifiers()));
		    		  //f.isAccessible()
		    		  
		    		  LOG.info("setting " + f.getName());
		    		  if (t.equals(java.lang.Boolean.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setBoolean(this, f.getBoolean(filter));		    			  
		    		  } else if (t.equals(java.lang.Character.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setChar(this, f.getChar(filter));
		    		  } else if (t.equals(java.lang.Byte.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setByte(this, f.getByte(filter));
		    		  } else if (t.equals(java.lang.Short.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setShort(this, f.getShort(filter));
		    		  } else if (t.equals(java.lang.Integer.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setInt(this, f.getInt(filter));
		    		  } else if (t.equals(java.lang.Long.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setLong(this, f.getLong(filter));
		    		  } else if (t.equals(java.lang.Float.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setFloat(this, f.getFloat(filter));
		    		  } else if (t.equals(java.lang.Double.TYPE))
		    		  {
		    			  activeFilter.getDeclaredField(f.getName()).setDouble(this, f.getDouble(filter));
		    		  } else {
		    			  LOG.info("cloning object " + f.getName());
		    			  activeFilter.getDeclaredField(f.getName()).set(this, f.get(filter));
		    		  }
	    			  
	    		  }
			} catch (Exception e) {
				Service.logException(e);
			}
	        //System.out.println(names[i] + ", " + fields[j].getName() + ", "
	    	  //+ fields[j].getType().getName() + ", " + Modifier.toString(fields[j].getModifiers()));
	      }
		
	}
	
	// TODO - remove begin ------------------------
	public Object setCFG(String name, Float value) // hmm what TODO ? Object
													// won't work Object[]
													// perhaps
	{
		return cfg.set(name, value);
	}

	public Object setCFG(String name, Integer value) // hmm what TODO ? Object
														// won't work Object[]
														// perhaps
	{
		return cfg.set(name, value);
	}

	public Object setCFG(String name, Boolean value) // hmm what TODO ? Object
														// won't work Object[]
														// perhaps
	{
		return cfg.set(name, value);
	}

	public Object setCFG(String name, String value) // hmm what TODO ? Object
													// won't work Object[]
													// perhaps
	{
		return cfg.set(name, value);
	}

	/*
	 * public Object setCFG(String name, Object value) // hmm what TODO ? Object
	 * won't work Object[] perhaps { return cfg.set(name, value); }
	 */

	// TODO - remove end ------------------------
	
	// TODO - remove use Annotations
	public abstract String getDescription();

	public abstract void loadDefaultConfiguration();

	public abstract IplImage process(IplImage image);

	public abstract BufferedImage display(IplImage image, Object[] data);

	// TODO - dispose()??

}
