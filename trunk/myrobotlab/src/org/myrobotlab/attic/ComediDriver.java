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

package org.myrobotlab.attic;

import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.ComediDriverConfig;
import org.myrobotlab.service.data.IOAddress;
import org.myrobotlab.service.data.IOSequence;
import org.myrobotlab.service.data.IOSequenceEntry;

public class ComediDriver extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(ComediDriver.class);

	public ComediDriverConfig config;

	public ComediDriver(String n) {
		super(n, ComediDriver.class.getCanonicalName());
		System.loadLibrary("IOPort");
	}

	public void foobar() {
		log.warn("foobar");
	}

	public static void write(IOAddress io) {
		log.info("write");
		jcomedidiowrite(io.subdevice, io.channel, io.data);
	}

	/*
	 * TODO - optimization - create and use a repeat sequence in the jni dll/so
	 */
	public void repeat(StringBuffer inNamedSequence) // TODO - sequence thread
														// list?
	{
		// create a thread - volatile isDone
		// until done - iterate through a sequence
		//
		String namedSequence = inNamedSequence.toString();

		log.info("write");
		log.info("{}", config.sequenceMap_);
		if (!config.sequenceMap_.containsKey(namedSequence)) {
			log.error("repeat " + namedSequence + " sequence was requested but " + namedSequence + " does not exist in config");
			return;
		}

		IOSequence ios = config.sequenceMap_.get(namedSequence);
		for (int i = 0; i < 101; ++i) {
			for (int j = 0; j < ios.sequenceList.size(); ++j) {
				IOSequenceEntry se = ios.sequenceList.get(j);
				jcomedidiowrite(se.ioAddress.subdevice, se.ioAddress.channel, se.ioAddress.data);

				try {
					Thread.sleep(se.timeInMilliSeconds_.intValue());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	public static int read(IOAddress io) {
		int readValue = 0;
		jcomedidioread(io.subdevice, io.channel, readValue);
		return readValue;
	}

	public static native void jcomedidiowrite(int subdev, int channel, int oneByte);

	public static native void jcomedidioread(int subdev, int channel, int oneByte);

	@Override
	public String getDescription() {
		return "used to interface with DIO (digital input/output computer cards)";
	}

}
