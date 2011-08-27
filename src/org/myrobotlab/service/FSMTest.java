package org.myrobotlab.service;

import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.OpenCVFilterMatchTemplate;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.OpenCV.Polygon;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;

public class FSMTest extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(FSMTest.class.getCanonicalName());

	
	String context = null; 			// current context identifier
	String contextPerson = null; 	// person of current context
	Node contextNode = null; 		// node of current context
	
	// TODO - these services could be accessed via RuntimeEnvironment.service("opencv");
	// TODO - service could be bound or created in an init - bound for remote	
	OpenCV opencv = null;
	SpeechRecognition speechRecognition = null;
	Speech speech = null;
	GUIService gui = null;
	
	Random generator = new Random();
	
	HashMap <String, HashMap<String, String>> phrases = new HashMap <String, HashMap<String,String>>(); 
	
	ArrayList<Node> memory = new ArrayList<Node>(); 
	
	boolean busy = false; 
	CvRect boundingBox = null;
	// findObject
		// segmentation - run kinect at ramping range
		// color hue 
		// lk track
	// identifyObject
	// reportObject
	// resolveObject - 2 objects - ask incrementally - send mail 
	
	
	public FSMTest(String n) {
		super(n, FSMTest.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	
	public void init ()
	{		
		speechRecognition = new SpeechRecognition ("sphinx");
		speechRecognition.startService();
		speech = new Speech("speech");
		speech.startService();
		opencv = new OpenCV("opencv");
		opencv.startService();
		gui = new GUIService("gui");
		gui.startService();
		
		speechRecognition.notify("publish", name, "heard", String.class);
		opencv.notify("publish", name, "publish", Polygon.class); //<--- BUG - polygon, name (only should work)
				
		// start vision
		opencv.grabberType = "com.googlecode.javacv.OpenKinectFrameGrabber";
		
		//opencv.addFilter("Gray1", "Gray");
		opencv.addFilter("PyramidDown1", "PyramidDown");
		gui.display();
		
		opencv.capture();
		
		sleep(5000); 
		
		initPhrases();
		context = FIND_OBJECT;
		
		findObject();
	}
	
	/*
	 * Context States 
	 * language keys
	 * semantic phrases are stored in a structure
	 * simplified meaning can quickly be derived depending on
	 * what context key is supplied
	 * e.g.
	 * 
	 * phrases.get(FIND_OBJECT).containsKey(input) - can be a test if 
	 * other input is in BagOfPhrases
	 */

	final static String FIND_OBJECT = "what is this"; // actor
	final static String HELLO = "hello"; // response
	final static String YES = "yes"; // response
	final static String NO = "no"; // response
	final static String I_AM_NOT_SURE = "i am not sure"; // response
	final static String I_DO_NOT_UNDERSTAND = "i do not understand";
	final static String IDENTIFY_OBJECT = "identify object";
	final static String QUERY_OBJECT = "i do not know what it is can you tell me";
	final static String WAITING_FOR_POLYGONS = "i am waiting for polygons";
	final static String IDLE = "i am in an idle state";
	
	
	
	public void initPhrases()
	{
		// load recognized grammar - keep in sync with simpl.gram
		// TODO - dynamically create a simple.gram file? vs programatically change it??
		// ------------------ SIMPLE.GRAM SYNC BEGIN --------------------------
		HashMap <String, String> findObjectPhrases = new HashMap<String, String>(); 
		findObjectPhrases.put("find object", null); 
		findObjectPhrases.put("look", null); 
		findObjectPhrases.put("what is this", null); 
		findObjectPhrases.put("what do you see", null);
		findObjectPhrases.put("and this", null);
		findObjectPhrases.put("what about this", null);
		findObjectPhrases.put("do you know what this is", null);
		phrases.put(FIND_OBJECT, findObjectPhrases);

		HashMap <String, String> nounWords = new HashMap<String, String>(); 
		nounWords.put("cup", null);
		nounWords.put("measuring thingy", null);
		nounWords.put("beer", null);
		nounWords.put("phone", null);
		nounWords.put("food", null);
		nounWords.put("ball", null);
		nounWords.put("apple", null);
		nounWords.put("orange", null);
		phrases.put(IDENTIFY_OBJECT, nounWords);
		// ------------------ SIMPLE.GRAM SYNC END --------------------------

		HashMap <String, String> iDoNotKnowWhatItIsCanYouTellMePhrases = new HashMap <String, String>();

		iDoNotKnowWhatItIsCanYouTellMePhrases.put("i dont know. please tell me", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("can you please tell me what it is", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("please tell me what it is", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("what is it", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("would you tell me what that is", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("i do not recognize it. could you tell me", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("i wish i knew", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("what would you call it", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("i dont know. please tell me", null);
		iDoNotKnowWhatItIsCanYouTellMePhrases.put("i have never seen one of those before. what is it", null);
		phrases.put(QUERY_OBJECT, iDoNotKnowWhatItIsCanYouTellMePhrases);
		
		HashMap <String, String> helloPhrases = new HashMap<String, String>(); 
		helloPhrases.put("hello", null);
		helloPhrases.put("greetings", null);
		helloPhrases.put("yes hello", null);
		helloPhrases.put("hi there", null);
		helloPhrases.put("good morning", null);
		phrases.put(HELLO, helloPhrases);

		HashMap <String, String> noPhrases = new HashMap<String, String>(); 
		noPhrases.put("no", null);
		noPhrases.put("i do not think so", null);
		noPhrases.put("no way", null);
		noPhrases.put("nope", null);
		noPhrases.put("i doubt it", null);
		phrases.put(NO, noPhrases);

		HashMap <String, String> yesPhrases = new HashMap<String, String>(); 
		yesPhrases.put("yes", null);
		yesPhrases.put("i believe so", null);
		yesPhrases.put("most certainly", null);
		yesPhrases.put("yep", null);
		yesPhrases.put("affirmative", null);
		yesPhrases.put("yes of course", null);
		yesPhrases.put("yeah", null);
		phrases.put(YES, yesPhrases);

		HashMap <String, String> idlePhrases = new HashMap<String, String>(); 
		idlePhrases.put(IDLE, null);
		idlePhrases.put("i am at rest", null);
		idlePhrases.put("i have stopped", null);
		idlePhrases.put("i am ready", null);
		idlePhrases.put("i am calm and will be listening for your next command", null);
		idlePhrases.put("i am zen", null);
		idlePhrases.put("i am very still", null);
		phrases.put(IDLE, idlePhrases);
		
	}
	

	public void heard (String data)
	{
		// if (context)
		if (data.equals("stop"))
		{

			opencv.removeFilters();
			opencv.addFilter("PyramidDown1", "PyramidDown");
			
			context = IDLE;
			speech.speak(getPhrase(IDLE));
			return;
		}
		
		if (data.equals("context"))
		{
			speech.speak("my current context is " + context);
			return;
		}
		
		if (phrases.get(FIND_OBJECT).containsKey(data))
		{
			findObject();
		} else if (context.equals(IDENTIFY_OBJECT) && phrases.get(IDENTIFY_OBJECT).containsKey(data)) {
			if (contextNode == null)
			{
				speech.speak("i forgot what we were talking about, lousy short term memory");
			}

			speech.speak("i will associate this with " + data);
			contextNode.word = data;
			memory.add(contextNode);
			speech.speak("i have " + memory.size() + " thing" + ((memory.size()>1)?"s":"" + " in my memory"));
			// clean up identifyObject - removed filters if input from user
			// TODO will need to remove filters if resolved in memory too
			opencv.removeFilter("Gray1");
			opencv.removeFilter("MatchTemplate1");
			
			context = FIND_OBJECT;
		} else {
			speech.speak("i do not understand. we were in context " + context + " but you said " + data);
		}
	}

	public void findObject ()
	{
		// lock ???
		// isolate, analyze, segmentation
		// begin segmentation and analysis

		// create depth mask - KinectDepth uses inRange to segment
		opencv.getDepth = true; // the switch is not "clean" - filters need to be defensive in which formats they will accept
		opencv.addFilter("KinectDepth1", "KinectDepth");
		opencv.addFilter("FindContours1", "FindContours"); // TODO add min requirements size etc

		// clear our polygons
		polygons = null;
		// get polygons
		// sleep(3000);
		context = WAITING_FOR_POLYGONS;
	}
		
	public void findObject2()
	{
	
			opencv.removeFilters();
			opencv.addFilter("PyramidDown1", "PyramidDown");
			
			// useful data for the kinect is 632 X 480 - 8 pixels on the right edge are not good data
			// http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1
			
			opencv.getDepth = false; // the switch is not "clean" - filters need to be defensive in which formats they will accept

			// check to see if we got a new polygon
			if (polygons == null)
			{
				speech.speak("i did not see anything");
				busy = false;
				return;
			} else {
				LOG.error("polygons size " + polygons.size());
				for (int i = 0; i < polygons.size(); ++i)
				{
					Polygon p = polygons.get(i);
					LOG.error(p.boundingRectangle.x());
					if (p.boundingRectangle.x() <= 1 || p.boundingRectangle.x() >= 316) // TODO clean this up in the filter
					{
						// kinect is a little goofy on the edges
						polygons.remove(i);
						
					}
				}
				
				if (polygons.size() == 1) // TODO - you must deal with this at some point
				{
					// process set of polygons
					boundingBox = polygons.get(0).boundingRectangle;
					speech.speak("i believe i see " + polygons.size() + " thing" + ((polygons.size() == 1)?"":"s"));					
				}
			}
						

			opencv.addFilter("Gray1", "Gray");
			opencv.addFilter("MatchTemplate1", "MatchTemplate");
			OpenCVFilterMatchTemplate mt = (OpenCVFilterMatchTemplate)opencv.getFilter("MatchTemplate1");
			mt.rect = boundingBox;
			mt.makeTemplate = true; // need new context here
			sleep(200);// TODO - remove all sleeps
			mt.makeTemplate = false;
			opencv.broadcastState();
			
			// identifyObject -------- begin ----------
			
			// make a new current context node 
			contextNode = new Node();
			// assign the segmented image found
			contextNode.imageData.image = cvCreateImage(cvSize(mt.template.width(), mt.template.height()), 8, 1);
			cvCopy(mt.template, contextNode.imageData.image, null);
									
			// search associative memory -- begin -----
			if (!identifyObject(contextNode))
			{
				speech.speak(getPhrase(QUERY_OBJECT)); // need input from user
				context = IDENTIFY_OBJECT; // asking for IDENTIFY_OBJECT
			} else {
				// associate Object - new data to add to memory object
			}
			// search associative memory -- end   -----
			
			
			
			// stop capture - switch to Gray Image
			// setROI
			// surf
			// match Template
			
			// and search associative memory - serializable !! hello DB !
			
			//
			//busy = false;
	}
		
	
	public boolean identifyObject(Node n)
	{
		if (memory.size() == 0)
		{
			return false;
		}
		
		speech.speak("i am searching my memory for this object");
		
		for (int i = 0; i < memory.size(); ++i)
		{
			// big smaller?
			
		}
		return true;		
	}
	
	ArrayList<Polygon> polygons = null;
	
	public void publish(ArrayList<Polygon> p) {
		LOG.error("found " + p.size() + " polygons");
		polygons = p;
		if (context.equals(WAITING_FOR_POLYGONS))
		{
			findObject2();
		}
	}
	public void publish(CvPoint p) {
		LOG.info("got point " + p);
	}
	
	public void sleep(int mill)
	{
		try {
			Thread.sleep(mill);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// TODO - build as data structure
	public String getPhrase (String input)
	{
		HashMap<String,String> t = phrases.get(input);
		Object[] keys = phrases.get(input).keySet().toArray();
		String randomValue = (String)keys[generator.nextInt(keys.length)];
		return randomValue;
	}
		
	public static void main(String[] args) {
		
		
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		FSMTest template = new FSMTest("fsm");
		template.startService();
		template.init();
		//template.speechRecognition.stopRecording();
		//template.speechRecognition.startRecording();
				
	}


}
