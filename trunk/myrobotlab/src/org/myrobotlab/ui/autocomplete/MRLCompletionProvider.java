/**
 * @author SwedaKonsult
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
 */
package org.myrobotlab.ui.autocomplete;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.List;

import org.apache.log4j.Logger;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.myrobotlab.control.JavaCompletionProvider;
import org.myrobotlab.reflection.Locator;
import org.myrobotlab.service.GUIService;

/**
 * @author SwedaKonsult
 *
 */
public class MRLCompletionProvider extends JavaCompletionProvider {
	/**
	 * Logger for this guy.
	 */
	public final static Logger log = Logger.getLogger(GUIService.class.getCanonicalName());
	
	/**
	 * Overriding base class declaration in order to load methods
	 * that should be easy to find and use in Jython. Still calls
	 * out the base class in order to load the Java keywords.
	 */
	protected void loadCompletions() {
		super.loadCompletions();
		
		try {
			loadClasses(Locator.getClasses("org.myrobotlab.service"));
		} catch (IOException e) {
			log.error("Could not load MRLCompletions because of I/O issues.", e);
		}
	}
	
	/**
	 * Load all information we want for the UI from the class.
	 * 
	 * @param implementation
	 */
	private void loadClass(Class<?> implementation) {
		loadClassMethods(implementation);
		loadClassConstants(implementation);
	}
	
	/**
	 * Load all constants available.
	 * 
	 * @param implementation
	 */
	private void loadClassConstants(Class<?> implementation) {
		if (implementation == null) {
			return;
		}
		Field[] fields = implementation.getDeclaredFields();
		if (fields == null || fields.length == 0) {
			return;
		}
		Completion completer;
		StringBuffer paramsString = new StringBuffer();
		StringBuffer genericsString = new StringBuffer();
		for (Field f: fields) {
			if (f.getName() == "main" || !Modifier.isPublic(f.getModifiers()) || !Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			// TODO: this doesn't work - doesn't actually grab the generics for this method
			genericsString.delete(0, genericsString.length());
			completer = new BasicCompletion(this,
					String.format("%s.%s", implementation.getName(), f.getName()),
					f.getName(),
					String.format("<html><body>"
								+ "<b>%1$s %2$s.%3$s"
								+ "%5$s(%4$s)"
								+ "</b> %6$s</body></html>",
							f.getType().getName(),
							f.getDeclaringClass().getName(),
							f.getName(),
							paramsString,
							genericsString,
							buildModifiers(f.getModifiers())));
			addCompletion(completer);
		}
		completer = null;
	}
	
	/**
	 * Load everything from the classes in the list.
	 * 
	 * @param classList
	 */
	private void loadClasses(List<Class<?>> classList) {
		if (classList == null || classList.size() == 0) {
			return;
		}
		for (Class<?> c: classList) {
			loadClass(c);
		}
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
		int arrayLength = 0;
		int loop = 0;
		Class<?>[] params;
		TypeVariable<Method>[] generics;
		StringBuffer paramsString = new StringBuffer();
		StringBuffer genericsString = new StringBuffer();
		for (Method m: methods) {
			if (m.getName() == "main" || !Modifier.isPublic(m.getModifiers())) {
				continue;
			}
			paramsString.delete(0, paramsString.length());
			params = m.getParameterTypes();
			arrayLength = params.length;
			if (arrayLength > 0) {
				for (loop = 0; loop < arrayLength; loop++) {
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
			arrayLength = generics.length;
			if (arrayLength > 0) {
				genericsString.append("<");
				for (loop = 0; loop < arrayLength; loop++) {
					if (loop > 0) {
						genericsString.append(",");
					}
					genericsString.append(generics[loop].getClass().getName());
				}
				genericsString.append(">");
			}
			completer = new BasicCompletion(this,
					String.format("%s(", m.getName()),
					m.getName(),
					String.format("<html><body>"
								+ "<b>%1$s %2$s.%3$s"
								+ "%5$s(%4$s)"
								+ "</b> %6$s</body></html>",
							m.getReturnType().getName(),
							m.getDeclaringClass().getName(),
							m.getName(),
							paramsString,
							genericsString,
							buildModifiers(m.getModifiers())));
			addCompletion(completer);
		}
		completer = null;
	}

	private CharSequence buildModifiers(int modifiers) {
		StringBuffer modifiersDescription = new StringBuffer();
		if (Modifier.isStatic(modifiers)) {
			modifiersDescription.append("<li>")
				.append("static")
				.append("</li>");
		}
		if (Modifier.isSynchronized(modifiers)) {
			modifiersDescription.append("<li>")
			.append("synchronized")
			.append("</li>");
		}
		
		if (modifiersDescription.length() == 0) {
			return "";
		}
		modifiersDescription.insert(0, "<br><br><b><i>Modifiers:</i></b><ul>")
			.append("</ul>");
		return modifiersDescription;
	}
}
