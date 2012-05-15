/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-08 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.myrobotlab.arduino;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.control.ArduinoGUI;

public class Compiler implements MessageConsumer {
	public final static Logger LOG = Logger.getLogger(Compiler.class.getCanonicalName());
	static final String BUGS_URL = "http://code.google.com/p/arduino/issues/list";
	static final String SUPER_BADNESS = "Compiler error, please submit this code to " + BUGS_URL;

	String sketch;
	String buildPath;
	String primaryClassName;
	boolean verbose;
	
	  /**
	   * This is *not* the "Processing" libraries path, this is the Java libraries
	   * path, as in java.library.path=BlahBlah, which identifies search paths for
	   * DLLs or JNILIBs.
	   */
	  private String libraryPath;

	  // maps imported packages to their library folder
	  static HashMap<String, File> importToLibraryTable;


  /**
   * Name of sketch, which is the name of main file
   * (without .pde or .java extension)
   */
  private String name;
	
	RunnerException exception;

	public Compiler() {
	}
	
	public void setCompilingProgress(int percent){	
	}

	ArrayList<File> importedLibraries = new ArrayList<File>();

	public ArrayList<File> getImportedLibraries() {
		return importedLibraries;
	}

	static public HashMap<String, Target> targetsTable;

	static public Target getTarget() {
		return targetsTable.get(Preferences.get("target"));
	}

	static public boolean isWindows() {
		// return PApplet.platform == PConstants.WINDOWS;
		return System.getProperty("os.name").indexOf("Windows") != -1;
	}

	static public Map<String, String> getBoardPreferences() {
		Target target = getTarget();
		if (target == null)
			return new LinkedHashMap();
		Map map = target.getBoards();
		if (map == null)
			return new LinkedHashMap();
		map = (Map) map.get(Preferences.get("board"));
		if (map == null)
			return new LinkedHashMap();
		return map;
	}

	/**
	 * true if running on linux.
	 */
	static public boolean isLinux() {
		// return PApplet.platform == PConstants.LINUX;
		return System.getProperty("os.name").indexOf("Linux") != -1;
	}

	static public String getHardwarePath() {
		return getHardwareFolder().getAbsolutePath();
	}

	static public File getHardwareFolder() {
		// calculate on the fly because it's needed by Preferences.init() to
		// find
		// the boards.txt and programmers.txt preferences files (which happens
		// before the other folders / paths get cached).
		return getContentFile("hardware");
	}

	/**
	 * returns true if Processing is running on a Mac OS X machine.
	 */
	static public boolean isMacOS() {
		// return PApplet.platform == PConstants.MACOSX;
		return System.getProperty("os.name").indexOf("Mac") != -1;
	}

	static public File getContentFile(String name) {
		String path = System.getProperty("user.dir");

		// Get a path to somewhere inside the .app folder
		if (isMacOS()) {
			// <key>javaroot</key>
			// <string>$JAVAROOT</string>
			String javaroot = System.getProperty("javaroot");
			if (javaroot != null) {
				path = javaroot;
			}
		}
		File working = new File(path);
		return new File(working, name);
	}

	static public String getAvrBasePath() {
		if (isLinux()) {
			return ""; // avr tools are installed system-wide and in the path
		} else {
			return getHardwarePath() + File.separator + "tools" + File.separator + "avr" + File.separator + "bin"
					+ File.separator;
		}
	}

	/**
	 * Compile with avr-gcc.
	 * 
	 * @param sketch
	 *            Sketch object to be compiled.
	 * @param buildPath
	 *            Where the temporary files live and will be built from.
	 * @param primaryClassName
	 *            the name of the combined sketch file w/ extension
	 * @return true if successful.
	 * @throws RunnerException
	 *             Only if there's a problem. Only then.
	 */
	public boolean compile(String sketch, String buildPath, String primaryClassName, boolean verbose)
			throws RunnerException {
		this.sketch = sketch;
		this.buildPath = buildPath;
		this.primaryClassName = primaryClassName;
		this.verbose = verbose;

		// the pms object isn't used for anything but storage
		MessageStream pms = new MessageStream(this);

		String avrBasePath = getAvrBasePath();

		Map<String, String> boardPreferences = getBoardPreferences();

		String core = boardPreferences.get("build.core");
		if (core == null) {
			RunnerException re = new RunnerException(
					"No board selected; please choose a board from the Tools > Board menu.");
			re.hideStackTrace();
			throw re;
		}
		String corePath;

		if (core.indexOf(':') == -1) {
			Target t = getTarget();
			File coreFolder = new File(new File(t.getFolder(), "cores"), core);
			corePath = coreFolder.getAbsolutePath();
		} else {
			Target t = targetsTable.get(core.substring(0, core.indexOf(':')));
			File coreFolder = new File(t.getFolder(), "cores");
			coreFolder = new File(coreFolder, core.substring(core.indexOf(':') + 1));
			corePath = coreFolder.getAbsolutePath();
		}
		// start merge ====================================

		String variant = boardPreferences.get("build.variant");
		String variantPath = null;

		if (variant != null) {
			if (variant.indexOf(':') == -1) {
				Target t = getTarget();
				File variantFolder = new File(new File(t.getFolder(), "variants"), variant);
				variantPath = variantFolder.getAbsolutePath();
			} else {
				Target t = targetsTable.get(variant.substring(0, variant.indexOf(':')));
				File variantFolder = new File(t.getFolder(), "variants");
				variantFolder = new File(variantFolder, variant.substring(variant.indexOf(':') + 1));
				variantPath = variantFolder.getAbsolutePath();
			}
		}

		// end merge ====================================
		List<File> objectFiles = new ArrayList<File>();

		// 0. include paths for core + all libraries

		setCompilingProgress(20);
		List includePaths = new ArrayList();
		includePaths.add(corePath);
		if (variantPath != null) includePaths.add(variantPath);
		for (File file : getImportedLibraries()) {
			includePaths.add(file.getPath());
		}

		// 1. compile the sketch (already in the buildPath)

		setCompilingProgress(30);
		objectFiles.addAll(compileFiles(avrBasePath, buildPath, includePaths, findFilesInPath(buildPath, "S", false),
				findFilesInPath(buildPath, "c", false), findFilesInPath(buildPath, "cpp", false), boardPreferences));

		// 2. compile the libraries, outputting .o files to:
		// <buildPath>/<library>/

		for (File libraryFolder : getImportedLibraries()) {
			File outputFolder = new File(buildPath, libraryFolder.getName());
			File utilityFolder = new File(libraryFolder, "utility");
			createFolder(outputFolder);
			// this library can use includes in its utility/ folder
			includePaths.add(utilityFolder.getAbsolutePath());
			objectFiles.addAll(compileFiles(avrBasePath, outputFolder.getAbsolutePath(), includePaths,
					findFilesInFolder(libraryFolder, "S", false), findFilesInFolder(libraryFolder, "c", false),
					findFilesInFolder(libraryFolder, "cpp", false), boardPreferences));
			outputFolder = new File(outputFolder, "utility");
			createFolder(outputFolder);
			objectFiles.addAll(compileFiles(avrBasePath, outputFolder.getAbsolutePath(), includePaths,
					findFilesInFolder(utilityFolder, "S", false), findFilesInFolder(utilityFolder, "c", false),
					findFilesInFolder(utilityFolder, "cpp", false), boardPreferences));
			// other libraries should not see this library's utility/ folder
			includePaths.remove(includePaths.size() - 1);
		}

		// 3. compile the core, outputting .o files to <buildPath> and then
		// collecting them into the core.a library file.

		includePaths.clear();
		includePaths.add(corePath); // include path for core only
		if (variantPath != null)
			includePaths.add(variantPath);
		List<File> coreObjectFiles = compileFiles(avrBasePath, buildPath, includePaths,
				findFilesInPath(corePath, "S", true), findFilesInPath(corePath, "c", true),
				findFilesInPath(corePath, "cpp", true), boardPreferences);

		String runtimeLibraryName = buildPath + File.separator + "core.a";
		List baseCommandAR = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-ar", "rcs",
				runtimeLibraryName }));
		for (File file : coreObjectFiles) {
			List commandAR = new ArrayList(baseCommandAR);
			commandAR.add(file.getAbsolutePath());
			execAsynchronously(commandAR);
		}

		// 4. link it all together into the .elf file

		List baseCommandLinker = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-gcc", "-Os",
				"-Wl,--gc-sections", "-mmcu=" + boardPreferences.get("build.mcu"), "-o",
				buildPath + File.separator + primaryClassName + ".elf" }));

		for (File file : objectFiles) {
			baseCommandLinker.add(file.getAbsolutePath());
		}

		baseCommandLinker.add(runtimeLibraryName);
		baseCommandLinker.add("-L" + buildPath);
		baseCommandLinker.add("-lm");

		execAsynchronously(baseCommandLinker);

		List baseCommandObjcopy = new ArrayList(
				Arrays.asList(new String[] { avrBasePath + "avr-objcopy", "-O", "-R", }));

		List commandObjcopy;

		// 5. extract EEPROM data (from EEMEM directive) to .eep file.
		commandObjcopy = new ArrayList(baseCommandObjcopy);
		commandObjcopy.add(2, "ihex");
		commandObjcopy.set(3, "-j");
		commandObjcopy.add(".eeprom");
		commandObjcopy.add("--set-section-flags=.eeprom=alloc,load");
		commandObjcopy.add("--no-change-warnings");
		commandObjcopy.add("--change-section-lma");
		commandObjcopy.add(".eeprom=0");
		commandObjcopy.add(buildPath + File.separator + primaryClassName + ".elf");
		commandObjcopy.add(buildPath + File.separator + primaryClassName + ".eep");
		execAsynchronously(commandObjcopy);

		// 6. build the .hex file
		commandObjcopy = new ArrayList(baseCommandObjcopy);
		commandObjcopy.add(2, "ihex");
		commandObjcopy.add(".eeprom"); // remove eeprom data
		commandObjcopy.add(buildPath + File.separator + primaryClassName + ".elf");
		commandObjcopy.add(buildPath + File.separator + primaryClassName + ".hex");
		execAsynchronously(commandObjcopy);

		return true;
	}

	
	private List<File> compileFiles(String avrBasePath, 
									String buildPath, List<File> includePaths, 
									List<File> sSources,
									List<File> cSources, List<File> cppSources, 
									Map<String, String> boardPreferences) 
		throws RunnerException {

		List<File> objectPaths = new ArrayList<File>();

		for (File file : sSources) {
			String objectPath = buildPath + File.separator + file.getName() + ".o";
			objectPaths.add(new File(objectPath));
			execAsynchronously(getCommandCompilerS(avrBasePath, includePaths, 
					file.getAbsolutePath(), 
					objectPath,
					boardPreferences));
		}

		for (File file : cSources) {
			String objectPath = buildPath + File.separator + file.getName() + ".o";
			objectPaths.add(new File(objectPath));
        execAsynchronously(getCommandCompilerC(avrBasePath, includePaths,
                                               file.getAbsolutePath(),
                                               objectPath,
					boardPreferences));
		}

		for (File file : cppSources) {
			String objectPath = buildPath + File.separator + file.getName() + ".o";
			objectPaths.add(new File(objectPath));
        execAsynchronously(getCommandCompilerCPP(avrBasePath, includePaths,
                                                 file.getAbsolutePath(),
                                                 objectPath,
                                                 boardPreferences));
		}

		return objectPaths;
	}


	boolean firstErrorFound;
	boolean secondErrorFound;

	/**
	 * Either succeeds or throws a RunnerException fit for public consumption.
	 */
	private void execAsynchronously(List commandList) throws RunnerException {

		LOG.debug(commandList.toString());

		String[] command = new String[commandList.size()];
		commandList.toArray(command);
		int result = 0;

		if (verbose || Preferences.getBoolean("build.verbose")) {
			for (int j = 0; j < command.length; j++) {
				System.out.print(command[j] + " ");
			}
			System.out.println();
		}

		firstErrorFound = false; // haven't found any errors yet
		secondErrorFound = false;

		Process process;

		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			RunnerException re = new RunnerException(e.getMessage());
			re.hideStackTrace();
			throw re;
		}

		MessageSiphon in = new MessageSiphon(process.getInputStream(), this);
		MessageSiphon err = new MessageSiphon(process.getErrorStream(), this);

		// wait for the process to finish. if interrupted
		// before waitFor returns, continue waiting
		boolean compiling = true;
		while (compiling) {
			try {
				if (in.thread != null)
					in.thread.join();
				if (err.thread != null)
					err.thread.join();
				result = process.waitFor();
				// System.out.println("result is " + result);
				compiling = false;
      } catch (InterruptedException ignored) { }
		}

		// an error was queued up by message(), barf this back to compile(),
		// which will barf it back to Editor. if you're having trouble
		// discerning the imagery, consider how cows regurgitate their food
		// to digest it, and the fact that they have five stomaches.
		//
		// System.out.println("throwing up " + exception);
    if (exception != null) { throw exception; }

		if (result > 1) {
			// a failure in the tool (e.g. unable to locate a sub-executable)
			System.err.println(command[0] + " returned " + result);
		}

		if (result != 0) {
			RunnerException re = new RunnerException("Error compiling.");
			re.hideStackTrace();
			throw re;
		}
	}

	String editor = "";

	static public String[] match(String what, String regexp) {
		Pattern p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
		Matcher m = p.matcher(what);
		if (m.find()) {
			int count = m.groupCount() + 1;
			String[] groups = new String[count];
			for (int i = 0; i < count; i++) {
				groups[i] = m.group(i);
			}
			return groups;
		}
		return null;
	}

	/**
	 * Parse a String to an int, and provide an alternate value that should be
	 * used when the number is invalid.
	 */
	static final public int parseInt(String what, int otherwise) {
		try {
			int offset = what.indexOf('.');
			if (offset == -1) {
				return Integer.parseInt(what);
			} else {
				return Integer.parseInt(what.substring(0, offset));
			}
		} catch (NumberFormatException e) {
		}
		return otherwise;
	}

	static final public int parseInt(String what) {
		return parseInt(what, 0);
	}

	/**
   * Part of the MessageConsumer interface, this is called
   * whenever a piece (usually a line) of error message is spewed
   * out from the compiler. The errors are parsed for their contents
   * and line number, which is then reported back to Editor.
	 */
	public void message(String s) {
		int i;

		// remove the build path so people only see the filename
    // can't use replaceAll() because the path may have characters in it which
		// have meaning in a regular expression.
		if (!verbose) {
			while ((i = s.indexOf(buildPath + File.separator)) != -1) {
				s = s.substring(0, i) + s.substring(i + (buildPath + File.separator).length());
			}
		}

		// look for error line, which contains file name, line number,
		// and at least the first line of the error message
		String errorFormat = "([\\w\\d_]+.\\w+):(\\d+):\\s*error:\\s*(.*)\\s*";
		String[] pieces = match(s, errorFormat);

		// if (pieces != null && exception == null) {
//      exception = sketch.placeException(pieces[3], pieces[1], PApplet.parseInt(pieces[2]) - 1);
		// if (exception != null) exception.hideStackTrace();
		// }

		if (pieces != null) {
      String error = pieces[3], msg = "";

      if (pieces[3].trim().equals("SPI.h: No such file or directory")) {
        error = "Please import the SPI library from the Sketch > Import Library menu.";
        msg = "\nAs of Arduino 0019, the Ethernet library depends on the SPI library." +
              "\nYou appear to be using it or another library that depends on the SPI library.\n\n";
			}

      if (pieces[3].trim().equals("'BYTE' was not declared in this scope")) {
        error = "The 'BYTE' keyword is no longer supported.";
        msg = "\nAs of Arduino 1.0, the 'BYTE' keyword is no longer supported." +
              "\nPlease use Serial.write() instead.\n\n";
      }
      
      if (pieces[3].trim().equals("no matching function for call to 'Server::Server(int)'")) {
        error = "The Server class has been renamed EthernetServer.";
        msg = "\nAs of Arduino 1.0, the Server class in the Ethernet library " +
              "has been renamed to EthernetServer.\n\n";
      }
      
      if (pieces[3].trim().equals("no matching function for call to 'Client::Client(byte [4], int)'")) {
        error = "The Client class has been renamed EthernetClient.";
        msg = "\nAs of Arduino 1.0, the Client class in the Ethernet library " +
              "has been renamed to EthernetClient.\n\n";
      }
      
      if (pieces[3].trim().equals("'Udp' was not declared in this scope")) {
        error = "The Udp class has been renamed EthernetUdp.";
        msg = "\nAs of Arduino 1.0, the Udp class in the Ethernet library " +
              "has been renamed to EthernetClient.\n\n";
      }
      
      if (pieces[3].trim().equals("'class TwoWire' has no member named 'send'")) {
        error = "Wire.send() has been renamed Wire.write().";
        msg = "\nAs of Arduino 1.0, the Wire.send() function was renamed " +
              "to Wire.write() for consistency with other libraries.\n\n";
      }
      
      if (pieces[3].trim().equals("'class TwoWire' has no member named 'receive'")) {
        error = "Wire.receive() has been renamed Wire.read().";
        msg = "\nAs of Arduino 1.0, the Wire.receive() function was renamed " +
              "to Wire.read() for consistency with other libraries.\n\n";
      }

      RunnerException e = placeException(error, pieces[1], parseInt(pieces[2]) - 1);

      // replace full file path with the name of the sketch tab (unless we're
      // in verbose mode, in which case don't modify the compiler output)
      if (e != null && !verbose) {
        //SketchCode code = sketch.getCode(e.getCodeIndex());
        //String fileName = code.isExtension(sketch.getDefaultExtension()) ? code.getPrettyName() : code.getFileName();
        String fileName = "C:\\mrl\\myrobotlab\\myServo.cpp";
        s = fileName + ":" + e.getCodeLine() + ": error: " + pieces[3] + msg;        
			}

			if (exception == null && e != null) {
				exception = e;
				exception.hideStackTrace();
			}
		}

		System.err.print(s);
	}

	public RunnerException placeException(String message, String dotJavaFilename, int dotJavaLine) {
		RunnerException re = new RunnerException(message + " " + dotJavaFilename + " " + dotJavaLine);
		LOG.error(message);
		return re;
	}

	public static final int REVISION = 100;

	// ///////////////////////////////////////////////////////////////////////////

	static private List getCommandCompilerS(String avrBasePath, List includePaths, String sourceName,
			String objectName, Map<String, String> boardPreferences) {
		List baseCommandCompiler = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-gcc",
				"-c", // compile, don't link
				"-g", // include debugging info (so errors include line numbers)
				"-assembler-with-cpp", "-mmcu=" + boardPreferences.get("build.mcu"),
				"-DF_CPU=" + boardPreferences.get("build.f_cpu"), "-DARDUINO=" + REVISION, }));

		for (int i = 0; i < includePaths.size(); i++) {
			baseCommandCompiler.add("-I" + (String) includePaths.get(i));
		}

		baseCommandCompiler.add(sourceName);
		baseCommandCompiler.add("-o" + objectName);

		return baseCommandCompiler;
	}

	static private List getCommandCompilerC(String avrBasePath, List includePaths, String sourceName,
			String objectName, Map<String, String> boardPreferences) {

		List baseCommandCompiler = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-gcc",
				"-c", // compile, don't link
				"-g", // include debugging info (so errors include line numbers)
				"-Os", // optimize for size
				"-w", // surpress all warnings
				"-ffunction-sections", // place each function in its own section
				"-fdata-sections", "-mmcu=" + boardPreferences.get("build.mcu"),
				"-DF_CPU=" + boardPreferences.get("build.f_cpu"), "-DARDUINO=" + REVISION, }));

		for (int i = 0; i < includePaths.size(); i++) {
			baseCommandCompiler.add("-I" + (String) includePaths.get(i));
		}

		baseCommandCompiler.add(sourceName);
		baseCommandCompiler.add("-o" + objectName);

		return baseCommandCompiler;
	}

	static private List getCommandCompilerCPP(String avrBasePath, List includePaths, String sourceName,
			String objectName, Map<String, String> boardPreferences) {

		List baseCommandCompilerCPP = new ArrayList(Arrays.asList(new String[] { avrBasePath + "avr-g++",
				"-c", // compile, don't link
				"-g", // include debugging info (so errors include line numbers)
				"-Os", // optimize for size
				"-w", // surpress all warnings
				"-fno-exceptions",
				"-ffunction-sections", // place each function in its own section
				"-fdata-sections", "-mmcu=" + boardPreferences.get("build.mcu"),
				"-DF_CPU=" + boardPreferences.get("build.f_cpu"), "-DARDUINO=" + REVISION, }));

		for (int i = 0; i < includePaths.size(); i++) {
			baseCommandCompilerCPP.add("-I" + (String) includePaths.get(i));
		}

		baseCommandCompilerCPP.add(sourceName);
		baseCommandCompilerCPP.add("-o" + objectName);

		return baseCommandCompilerCPP;
	}

	// ///////////////////////////////////////////////////////////////////////////

	static private void createFolder(File folder) throws RunnerException {
		if (folder.isDirectory())
			return;
		if (!folder.mkdirs())
			throw new RunnerException("Couldn't create: " + folder);
	}

	/**
	 * Given a folder, return a list of the header files in that folder (but not
	 * the header files in its sub-folders, as those should be included from
	 * within the header files at the top-level).
	 */
	static public String[] headerListFromIncludePath(String path) {
		FilenameFilter onlyHFiles = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".h");
			}
		};

		return (new File(path)).list(onlyHFiles);
	} 

	static public ArrayList<File> findFilesInPath(String path, String extension, boolean recurse) {
		return findFilesInFolder(new File(path), extension, recurse);
	}

	static public ArrayList<File> findFilesInFolder(File folder, String extension, boolean recurse) {
		ArrayList<File> files = new ArrayList<File>();

		if (folder.listFiles() == null)
			return files;

		for (File file : folder.listFiles()) {
			if (file.getName().startsWith("."))
				continue; // skip hidden files

			if (file.getName().endsWith("." + extension))
				files.add(file);

			if (recurse && file.isDirectory()) {
				files.addAll(findFilesInFolder(file, extension, true));
			}
		}

		return files;
	}

	protected void loadHardware(File folder) {
		if (!folder.isDirectory())
			return;

		String list[] = folder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.')
					return false;
				if (name.equals("CVS"))
					return false;
				return (new File(dir, name).isDirectory());
			}
		});
		// if a bad folder or something like that, this might come back null
		if (list == null)
			return;

		// alphabetize list, since it's not always alpha order
		// replaced hella slow bubble sort with this feller for 0093
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		for (String target : list) {
			File subfolder = new File(folder, target);
			targetsTable.put(target, new Target(target, subfolder));
		}
	}

	protected String upload(String buildPath, String suggestedClassName, boolean verbose) throws RunnerException,
			SerialException {

		Uploader uploader;

		// download the program
		//
		uploader = new AvrdudeUploader();
		boolean success = uploader.uploadUsingPreferences(buildPath, suggestedClassName, verbose);

		return success ? suggestedClassName : null;
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);

		// run the preprocessor
		String primaryClassName = "myServo.cpp";
		// String myCode = "#include <SD.h>";
		String myCode = "" + "int ledPin = 13;               " + "" + "void setup()                   " + "{"
				+ "  pinMode(ledPin, OUTPUT);     " + "}" + "" + "void loop()                    " + "{"
				+ "  digitalWrite(ledPin, HIGH);  " + "  delay(1000);                 "
				+ "  digitalWrite(ledPin, LOW);   " + "  delay(1000);                 " + "}";

		String buildPath = "./tmp/2304983209839";
		boolean verbose = true;
		
		
		
		// compile the program. errors will happen as a RunnerException
		// that will bubble up to whomever called build().
		Compiler compiler = new Compiler();
		compiler.init();
		
		try {
			
			/*** new build call ***/
			compiler.build(buildPath,true);
			
			
			if (compiler.compile(myCode, buildPath, primaryClassName, verbose)) {
				try {
					compiler.upload(buildPath, primaryClassName, verbose);
				} catch (SerialException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// size(buildPath, primaryClassName);
				// return primaryClassName;
			}
		} catch (RunnerException e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	  /**
	   * Preprocess and compile all the code for this sketch.
	   *
	   * In an advanced program, the returned class name could be different,
	   * which is why the className is set based on the return value.
	   * A compilation error will burp up a RunnerException.
	   *
	   * @return null if compilation failed, main class name if not
	   */
	  public String build(String buildPath, boolean verbose)
	    throws RunnerException {
	    
	    // run the preprocessor
		setCompilingProgress(20);
	    String primaryClassName = preprocess(buildPath);

	    // compile the program. errors will happen as a RunnerException
	    // that will bubble up to whomever called build().
	    Compiler compiler = new Compiler();
	    if (compiler.compile("FIXME", buildPath, primaryClassName, verbose)) {
	      size(buildPath, primaryClassName);
	      return primaryClassName;
	    }
	    return null;
	  }
	
	public void init() {
		targetsTable = new HashMap<String, Target>();
		loadHardware(getHardwareFolder());
		Preferences.set("upload.using", "bootloader");
		Preferences.set("target", "arduino");
		Preferences.set("board", "diecimila");

		Preferences.set("serial.port", "/dev/ttyUSB0");
		Preferences.set("serial.debug_rate", "115200");
		Preferences.set("serial.parity", "N");
		Preferences.set("serial.databits", "8");
		Preferences.set("serial.stopbits", "1");

		// GAP - configuration here
		/*
		 * Map<String,String> boardPreferences = new Map<String,String>();
		 * boardPreferences.put("build.core", "arduino");
		 * boardPreferences.put("build.mcu", "atmega168");
		 * boardPreferences.put("build.f_cpu", "16000000L");
		 * boardPreferences.put("upload.protocol", "stk500");
		 * boardPreferences.put("upload.speed", "19200");
		 */
		// Preferences.set("board", boardPreferences);

		// FIXME FIXME FIXME ! GAP - preprocessor
//		importedLibraries.add(new File("C:\\mrl\\myrobotlab\\hardware\\libraries\\Servo"));
		/*
		 * HardwareSerial.h WProgram.h wiring.h WConstants.h binary.h
		 * pins_arduino.h wiring_private.h
		 */
	}

	/**
	 * Lovingly taken from Processing !!!
	   * ( begin auto-generated from matchAll.xml )
	   * 
	   * The matchAll() function is used to apply a regular expression to a piece of text, and return a list of matching groups (elements found inside parentheses) as a two-dimensional String array. No matches will return null. If no groups are specified in the regexp, but the sequence matches, a two dimensional array is still returned, but the second dimension is only of length one.
	   * <br/> <br/>
	   * To use the function, first check to see if the result is null. If the result is null, then the sequence did not match at all. If the sequence did match, a 2D array is returned. 
	   * If there are groups (specified by sets of parentheses) in the regexp, then the contents of each will be returned in the array.  
	   * Assuming, a loop with counter variable i, element [i][0] of a regexp match returns the entire matching string, and the match groups start at element [i][1] (the first group is [i][1], the second [i][2], and so on).
	   * <br/> <br/>
	   * The syntax can be found in the reference for Java's <A HREF="http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html">Pattern</A> class. For regular expression syntax, read the <A HREF="http://java.sun.com/docs/books/tutorial/essential/regex/">Java Tutorial</A> on the topic. 
	   * <br/> <br/>
	   * 
	   * ( end auto-generated )
	   * @webref data:string_functions
	   * @param what the String to search inside
	   * @param regexp the regexp to be used for matching
	   * @see PApplet#match(String, String)
	   * @see PApplet#split(String, String)
	   * @see PApplet#splitTokens(String, String)
	   * @see PApplet#join(String[], String)
	   * @see PApplet#trim(String)
	   */
	  static public String[][] matchAll(String what, String regexp) {
	    Pattern p = matchPattern(regexp);
	    Matcher m = p.matcher(what);
	    ArrayList<String[]> results = new ArrayList<String[]>();
	    int count = m.groupCount() + 1;
	    while (m.find()) {
	      String[] groups = new String[count];
	      for (int i = 0; i < count; i++) {
	        groups[i] = m.group(i);
	      }
	      results.add(groups);
	    }
	    if (results.isEmpty()) {
	      return null;
	    }
	    String[][] matches = new String[results.size()][count];
	    for (int i = 0; i < matches.length; i++) {
	      matches[i] = (String[]) results.get(i);
	    }
	    return matches;
	  }
	  static protected HashMap<String, Pattern> matchPatterns;

	  static Pattern matchPattern(String regexp) {
	    Pattern p = null;
	    if (matchPatterns == null) {
	      matchPatterns = new HashMap<String, Pattern>();
	    } else {
	      p = matchPatterns.get(regexp);
	    }
	    if (p == null) {
	      if (matchPatterns.size() == 10) {
	        // Just clear out the match patterns here if more than 10 are being
	        // used. It's not terribly efficient, but changes that you have >10
	        // different match patterns are very slim, unless you're doing
	        // something really tricky (like custom match() methods), in which
	        // case match() won't be efficient anyway. (And you should just be
	        // using your own Java code.) The alternative is using a queue here,
	        // but that's a silly amount of work for negligible benefit.
	        matchPatterns.clear();
	      }
	      p = Pattern.compile(regexp, Pattern.MULTILINE | Pattern.DOTALL);
	      matchPatterns.put(regexp, p);
	    }
	    return p;
	  }


	  //----------------------------------------------------------------------------------
	  // FROM Sketch
	  private String classPath;
	  private SketchCode[] code;

	  public String preprocess(String buildPath) throws RunnerException {
		    return preprocess(buildPath, new PdePreprocessor());
		  }

		  public String preprocess(String buildPath, PdePreprocessor preprocessor) throws RunnerException {
		    // make sure the user didn't hide the sketch folder
		    //ensureExistence();

		    String[] codeFolderPackages = null;
		    classPath = buildPath;

		    // 1. concatenate all .pde files to the 'main' pde
		    //    store line number for starting point of each code bit

		    StringBuffer bigCode = new StringBuffer();
		    int bigCount = 0;
		    
		    code = new SketchCode[]{new SketchCode(new File("C:\\mrl\\myrobotlab\\tmp\\2304983209839\\myServo.cpp"), "cpp")};

/******************* I REALLY DON"T UNDERSTAND WHY ALL THE JAVA PDE STUFF 
 * Why can't we just compile cpp files ? 
 *  		    
		    for (SketchCode sc : code) {
		      if (sc.isExtension("ino") || sc.isExtension("pde")) {
		        sc.setPreprocOffset(bigCount);
		        bigCode.append(sc.getProgram());
		        bigCode.append('\n');
		        bigCount += sc.getLineCount();
		      }
		    }

		    // Note that the headerOffset isn't applied until compile and run, because
		    // it only applies to the code after it's been written to the .java file.
		    int headerOffset = 0;
		    //PdePreprocessor preprocessor = new PdePreprocessor();
		    try {
		      headerOffset = preprocessor.writePrefix(bigCode.toString(),
		                                              buildPath,
		                                              name,
		                                              codeFolderPackages);
		    } catch (FileNotFoundException fnfe) {
		      fnfe.printStackTrace();
		      String msg = "Build folder disappeared or could not be written";
		      throw new RunnerException(msg);
		    }

		    // 2. run preproc on that code using the sugg class name
		    //    to create a single .java file and write to buildpath

		    String primaryClassName = null;

		    try {
		      // if (i != 0) preproc will fail if a pde file is not
		      // java mode, since that's required
		      String className = preprocessor.write();

		      if (className == null) {
		        throw new RunnerException("Could not find main class");
		        // this situation might be perfectly fine,
		        // (i.e. if the file is empty)
		        //System.out.println("No class found in " + code[i].name);
		        //System.out.println("(any code in that file will be ignored)");
		        //System.out.println();

//		      } else {
//		        code[0].setPreprocName(className + ".java");
		      }

		      // store this for the compiler and the runtime
		      primaryClassName = className + ".cpp";

		    } catch (FileNotFoundException fnfe) {
		      fnfe.printStackTrace();
		      String msg = "Build folder disappeared or could not be written";
		      throw new RunnerException(msg);
		    } catch (RunnerException pe) {
		      // RunnerExceptions are caught here and re-thrown, so that they don't
		      // get lost in the more general "Exception" handler below.
		      throw pe;

		    } catch (Exception ex) {
		      // TODO better method for handling this?
		      System.err.println("Uncaught exception type:" + ex.getClass());
		      ex.printStackTrace();
		      throw new RunnerException(ex.toString());
		    }

		    // grab the imports from the code just preproc'd

		    importedLibraries = new ArrayList<File>();

		    for (String item : preprocessor.getExtraImports()) {
		      File libFolder = (File) importToLibraryTable.get(item);

		      if (libFolder != null && !importedLibraries.contains(libFolder)) {
		        importedLibraries.add(libFolder);
		        //classPath += Compiler.contentsToClassPath(libFolder);
		        libraryPath += File.pathSeparator + libFolder.getAbsolutePath();
		      }
		    }

		    // 3. then loop over the code[] and save each .java file
*/
		    
		    try {
				String className = preprocessor.write();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		    
		    for (SketchCode sc : code) {
		      if (sc.isExtension("c") || sc.isExtension("cpp") || sc.isExtension("h")) {
		        // no pre-processing services necessary for java files
		        // just write the the contents of 'program' to a .java file
		        // into the build directory. uses byte stream and reader/writer
		        // shtuff so that unicode bunk is properly handled
		        String filename = sc.getFileName(); //code[i].name + ".java";
		        try {
		          ArduinoGUI.saveFile(sc.getProgram(), new File(buildPath, filename));
		        } catch (IOException e) {
		          e.printStackTrace();
		          throw new RunnerException("Problem moving " + filename +
		                                    " to the build folder");
		        }
//		        sc.setPreprocName(filename);

		      } 		      
		      /*
		      else if (sc.isExtension("ino") || sc.isExtension("pde")) {
		        // The compiler and runner will need this to have a proper offset
		        sc.addPreprocOffset(headerOffset);
		      }
		      */
		    }
		    return primaryClassName;
		  }
	  	  	  
		  protected void size(String buildPath, String suggestedClassName)
				    throws RunnerException {
				    long size = 0;
				    String maxsizeString = getBoardPreferences().get("upload.maximum_size");
				    if (maxsizeString == null) return;
				    long maxsize = Integer.parseInt(maxsizeString);
				    Sizer sizer = new Sizer(buildPath, suggestedClassName);
				      try {
				      size = sizer.computeSize();
				      System.out.println("Binary sketch size: " + size + " bytes (of a " +
				        maxsize + " byte maximum)");      
				    } catch (RunnerException e) {
				      System.err.println("Couldn't determine program size: " + e.getMessage());
				    }

				    if (size > maxsize)
				      throw new RunnerException(
				        "Sketch too big; see http://www.arduino.cc/en/Guide/Troubleshooting#size for tips on reducing it.");
				  }

		  
}
