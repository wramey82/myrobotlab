/**
 * 
 */
package org.myrobotlab.ui.autocomplete;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.myrobotlab.control.JavaCompletionProvider;

/**
 * @author SwedaKonsult
 *
 */
public class MRLCompletionProvider extends JavaCompletionProvider {
	
	/**
	 * Overriding base class declaration in order to load methods
	 * that should be easy to find and use in Jython. Still calls
	 * out the base class in order to load the Java keywords.
	 */
	protected void loadCompletions() {
		super.loadCompletions();
		// TODO: this needs to be dynamically loaded
		loadClassMethods(org.myrobotlab.service.GUIService.class);
		loadClassMethods(org.myrobotlab.service.AFMotorShield.class);
		loadClassMethods(org.myrobotlab.service.Arduino.class);
		loadClassMethods(org.myrobotlab.service.Arm.class);
		loadClassMethods(org.myrobotlab.service.AudioCapture.class);
		loadClassMethods(org.myrobotlab.service.AudioFile.class);
		loadClassMethods(org.myrobotlab.service.ChessGame.class);
		loadClassMethods(org.myrobotlab.service.ChumbyBot.class);
		loadClassMethods(org.myrobotlab.service.Clock.class);
		loadClassMethods(org.myrobotlab.service.Drupal.class);
		loadClassMethods(org.myrobotlab.service.FaceTracking.class);
		loadClassMethods(org.myrobotlab.service.FSM.class);
		loadClassMethods(org.myrobotlab.service.GeneticProgramming.class);
		loadClassMethods(org.myrobotlab.service.GoogleSTT.class);
		loadClassMethods(org.myrobotlab.service.Graphics.class);
		loadClassMethods(org.myrobotlab.service.HTTPClient.class);
		loadClassMethods(org.myrobotlab.service.IPCamera.class);
		loadClassMethods(org.myrobotlab.service.JFugue.class);
		loadClassMethods(org.myrobotlab.service.Joystick.class);
		loadClassMethods(org.myrobotlab.service.Jython.class);
		loadClassMethods(org.myrobotlab.service.Keyboard.class);
		loadClassMethods(org.myrobotlab.service.Logging.class);
		loadClassMethods(org.myrobotlab.service.MagaBot.class);
		loadClassMethods(org.myrobotlab.service.Motor.class);
		loadClassMethods(org.myrobotlab.service.OpenCV.class);
		loadClassMethods(org.myrobotlab.service.ParallelPort.class);
		loadClassMethods(org.myrobotlab.service.PICAXE.class);
		loadClassMethods(org.myrobotlab.service.PID.class);
		loadClassMethods(org.myrobotlab.service.PlayerStage.class);
		loadClassMethods(org.myrobotlab.service.Proxy.class);
		loadClassMethods(org.myrobotlab.service.RecorderPlayer.class);
		loadClassMethods(org.myrobotlab.service.Red5.class);
		loadClassMethods(org.myrobotlab.service.RemoteAdapter.class);
		loadClassMethods(org.myrobotlab.service.RobotPlatform.class);
		loadClassMethods(org.myrobotlab.service.Roomba.class);
		loadClassMethods(org.myrobotlab.service.Runtime.class);
		loadClassMethods(org.myrobotlab.service.Scheduler.class);
		loadClassMethods(org.myrobotlab.service.SensorMonitor.class);
		loadClassMethods(org.myrobotlab.service.Serializer.class);
		loadClassMethods(org.myrobotlab.service.Servo.class);
		loadClassMethods(org.myrobotlab.service.Simbad.class);
		loadClassMethods(org.myrobotlab.service.Skype.class);
		loadClassMethods(org.myrobotlab.service.SLAM.class);
		loadClassMethods(org.myrobotlab.service.SoccerGame.class);
		loadClassMethods(org.myrobotlab.service.Speech.class);
		loadClassMethods(org.myrobotlab.service.Sphinx.class);
		loadClassMethods(org.myrobotlab.service.ThingSpeak.class);
		loadClassMethods(org.myrobotlab.service.TrackingService.class);
		loadClassMethods(org.myrobotlab.service.TweedleBot.class);
		loadClassMethods(org.myrobotlab.service.WebServer.class);
		loadClassMethods(org.myrobotlab.service.Wii.class);
		loadClassMethods(org.myrobotlab.service.WiiBot.class);
		loadClassMethods(org.myrobotlab.service.WiiDAR.class);
	}
	
	/**
	 * Helper method that recurses implementation to find all
	 * public static methods declared.
	 * @param implementation the class to analyze
	 */
	private void loadClassMethods(Class<?> implementation) {
		if (implementation == null) {
			return;
		}
		Method[] methods = implementation.getDeclaredMethods();
		if (methods == null || methods.length == 0) {
			return;
		}
		Completion completer;
		int paramLength = 0;
		int loop = 0;
		Class<?>[] params;
		TypeVariable<Method>[] generics;
		StringBuffer paramsString = new StringBuffer();
		StringBuffer genericsString = new StringBuffer();
		for (Method m: methods) {
			if (m.getName() == "main" || !Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
				continue;
			}
			paramsString.delete(0, paramsString.length());
			params = m.getParameterTypes();
			paramLength = params.length;
			if (paramLength > 0) {
				for (loop = 0; loop < paramLength; loop++) {
					if (loop > 0) {
						paramsString.append(",");
					}
					paramsString.append(params[loop].getName());
					// TODO: should grab the generics for each parameter
				}
			}
			genericsString.delete(0, genericsString.length());
			// TODO: this doesn't work - doesn't actually grab the generics for this method
			generics = m.getTypeParameters();
			paramLength = generics.length;
			if (paramLength > 0) {
				genericsString.append("<");
				for (loop = 0; loop < paramLength; loop++) {
					if (loop > 0) {
						genericsString.append(",");
					}
					genericsString.append(generics[loop].getClass().getName());
				}
				genericsString.append(">");
			}
			completer = new BasicCompletion(this,
					m.getName() + "(",
					m.getName(),
					String.format("<html><body>"
								+ "<b>%1$s %2$s.%3$s"
								+ "%5$s(%4$s)"
								+ "</b></body></html>",
							m.getReturnType().getName(),
							m.getClass().getName(),
							m.getName(),
							paramsString,
							genericsString));
			addCompletion(completer);
		}
		completer = null;
	}
}
