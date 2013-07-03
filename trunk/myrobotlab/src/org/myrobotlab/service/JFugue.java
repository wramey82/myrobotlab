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

package org.myrobotlab.service;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.jfugue.Player;
import org.jfugue.Rhythm;
import org.myrobotlab.framework.Service;

public class JFugue extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(JFugue.class.getCanonicalName());
	transient public Player player = new Player();

	// TODO - look at JavaSoundDemo - they have a synth & mixer there
	
	public JFugue(String n) {
		super(n, JFugue.class.getCanonicalName());
	}

	public void play(String s) {
		player.play(s);
	}

	public void play(Rhythm rythm) {
		player.play(rythm);
	}

	public void play(Integer i) { // play tone
		// player.play("[A" + i + "]w");
		player.play("[" + i + "]");
	}

	public void playRythm() {
		Rhythm rhythm = new Rhythm();
		rhythm.setLayer(1, "O..oO...O..oOO..");
		rhythm.setLayer(2, "..*...*...*...*.");
		rhythm.addSubstitution('O', "[BASS_DRUM]i");
		rhythm.addSubstitution('o', "Rs [BASS_DRUM]s");
		rhythm.addSubstitution('*', "[ACOUSTIC_SNARE]i");
		rhythm.addSubstitution('.', "Ri");
		play(rhythm);
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		JFugue jfugue = new JFugue("jfugue");
		
		jfugue.playRythm();
		
		jfugue.play("C D E F G A B");
		jfugue.play("A A A B B B");
		jfugue.playRythm();
		jfugue.play(30);
		jfugue.play(31);
		jfugue.play(40);
		jfugue.play(55);
		jfugue.play("E5s A5s C6s B5s E5s B5s D6s C6i E6i G#5i E6i | A5s E5s A5s C6s B5s E5s B5s D6s C6i A5i Ri");
	}

	@Override
	public String getToolTip() {
		return "service wrapping Jfugue - http://www.jfugue.org/ used for music and sound generation";
	}

}
