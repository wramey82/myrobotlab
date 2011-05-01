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

package org.myrobotlab.control;

import java.io.ByteArrayOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JLabel;

import org.myrobotlab.service.GUIService;

public class AudioCaptureGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	// JList files;

	boolean stopCapture = false;
	ByteArrayOutputStream byteArrayOutputStream;
	AudioFormat audioFormat;
	TargetDataLine targetDataLine;
	AudioInputStream audioInputStream;
	SourceDataLine sourceDataLine;

	// TODO - consider normalizing against static functions of the AudioCapture
	// service !
	public class PlayThread extends Thread {
		byte tempBuffer[] = new byte[10000];

		public void run() {
			try {
				int cnt;
				// Keep looping until the input
				// read method returns -1 for
				// empty stream.
				while ((cnt = audioInputStream.read(tempBuffer, 0,
						tempBuffer.length)) != -1) {
					if (cnt > 0) {
						// Write data to the internal
						// buffer of the data line
						// where it will be delivered
						// to the speaker.
						sourceDataLine.write(tempBuffer, 0, cnt);
					}// end if
				}// end while
				// Block and wait for internal
				// buffer of the data line to
				// empty.
				sourceDataLine.drain();
				sourceDataLine.close();
			} catch (Exception e) {
				System.out.println(e);
				System.exit(0);
			}// end catch
		}// end run
	}// end inner class PlayThread

	public AudioCaptureGUI() {
		this("unknown", null);
	}

	public AudioCaptureGUI(String name, GUIService myService) {
		super(name, myService);
		display.add(new JLabel("no gui"));
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}

}
