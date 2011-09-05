package org.myrobotlab.service;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_SQDIFF;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.OpenCVFilterMatchTemplate;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.OpenCV.Polygon;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class FSMTest extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(FSMTest.class.getCanonicalName());
	
	String context = null; 			// current context identifier
	String contextPerson = null; 	// person of current context
	Node contextNode = null; 		// node of current context
	IplImage template = null; // template made from segmentation
	IplImage cameraFrame = null; // image from camera
	
	// TODO - these services could be accessed via RuntimeEnvironment.service("opencv");
	// TODO - service could be bound or created in an init - bound for remote	
	OpenCV opencv = null;
	SpeechRecognition speechRecognition = null;
	Speech speech = null;
	GUIService gui = null;
	
	Random generator = new Random();
	
	HashMap <String, HashMap<String, String>> phrases = new HashMap <String, HashMap<String,String>>(); 
	
	ArrayList<Node> memory = new ArrayList<Node>(); 
	ArrayList<Polygon> polygons = null;
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
	
	public void listeningEvent()
	{
		speech.speak("i am listening");
		speech.speak("ready");
	}
	
	public void init ()
	{		
		speechRecognition = new SpeechRecognition ("sphinx");
//		speechRecognition.startService();
		speech = new Speech("speech");
		speech.startService();
		opencv = new OpenCV("opencv");
		opencv.startService();
		gui = new GUIService("gui");
		gui.startService();
		
		speechRecognition.notify("publish", name, "heard", String.class);
		speechRecognition.notify("listeningEvent", name, "listeningEvent");
		
		opencv.notify("publish", name, "publish", Polygon.class); //<--- BUG - polygon, name (only should work)
		opencv.notify("publishIplImageTemplate", name, "getImageTemplate", IplImage.class);
		opencv.notify("publishIplImage", name, "publishIplImage", IplImage.class);
				
		// start vision
		opencv.grabberType = "com.googlecode.javacv.OpenKinectFrameGrabber";
		
		//opencv.addFilter("Gray1", "Gray");
//		opencv.addFilter("PyramidDown1", "PyramidDown");
		gui.display();
		
		opencv.getDepth = true; // Need a good switch in OpenCVGUI - runtime RGB RGB+Depth Depth
		
		opencv.capture();
		
//		sleep(5000); 
				
		initPhrases();
//		context = FIND_OBJECT;
		context = IDLE;
		speech.speak("my mouth is working");
		speech.speak("my eyes are open");
		//speech.speak("ready");
//		findKinectPolygons();
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
	final static String GET_ASSOCIATIVE_WORD = "get associative word";
	final static String QUERY_OBJECT = "i do not know what it is can you tell me";
	final static String WAITING_FOR_POLYGONS = "i am waiting for polygons";
	final static String IDLE = "i am in an idle state";
	final static String FOUND_POLYGONS = "i have found polygons";
	final static String GET_CAMERA_FRAME = "i am getting an image";
	
	
	
	
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
		nounWords.put("guitar", null);
		nounWords.put("phone", null);
		nounWords.put("food", null);
		nounWords.put("ball", null);
		nounWords.put("apple", null);
		nounWords.put("orange", null);
		phrases.put(GET_ASSOCIATIVE_WORD, nounWords);
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
		
		if (data.equals("context")) // VERY HELPFUL !
		{
			speech.speak("my current context is " + context);
			return;
		}
		
		if (phrases.get(FIND_OBJECT).containsKey(data))
		{
			findKinectPolygons();
		} else if (context.equals(GET_ASSOCIATIVE_WORD) && phrases.get(GET_ASSOCIATIVE_WORD).containsKey(data)) {

			speech.speak("i will associate this with " + data);
			// Load Memory ---- BEGIN --------			
				contextNode = new Node();
				
				contextNode.word = data;

				contextNode.imageData.cvTemplate = cvCreateImage(cvSize(template.width(), template.height()), 8, 1);
				cvCopy(template, contextNode.imageData.cvTemplate, null);
				
				LOG.error("ch " + cameraFrame.nChannels());
				
				contextNode.imageData.cvCameraFrame = cvCreateImage(cvSize(cameraFrame.width(), cameraFrame.height()), 8, cameraFrame.nChannels()); // full color
				cvCopy(cameraFrame, contextNode.imageData.cvCameraFrame, null);
				
				contextNode.imageData.cvGrayFrame = cvCreateImage(cvSize(cameraFrame.width(), cameraFrame.height()), 8, 1);
				cvCvtColor(contextNode.imageData.cvCameraFrame, contextNode.imageData.cvGrayFrame, CV_BGR2GRAY);
				
				memory.add(contextNode);
			// Load Memory ---- BEGIN --------			
				
			speech.speak("i have " + memory.size() + " thing" + ((memory.size()>1)?"s":"" + " in my memory"));

			opencv.removeFilters();
			opencv.addFilter("PyramidDown1", "PyramidDown");
			
			context = FIND_OBJECT;
		} else {
			speech.speak("i do not understand. we were in context " + context + " but you said " + data);
		}
	}

	public void findKinectPolygons ()
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
		
	public void publish(ArrayList<Polygon> p) {
		LOG.error("found " + p.size() + " polygons");
		polygons = p;
		if (context.equals(WAITING_FOR_POLYGONS))
		{
			context = FOUND_POLYGONS;
			processPolygons();
		}
	}
	
	public void processPolygons()
	{
	
			opencv.removeFilters();
			opencv.addFilter("PyramidDown1", "PyramidDown");
			
			// useful data for the kinect is 632 X 480 - 8 pixels on the right edge are not good data
			// http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1
			
			opencv.getDepth = false; 

			// TODO - filter out with FindContours settings !
			// check to see if we got a new polygon
			if (polygons == null)
			{
				speech.speak("i did not see anything");
				context = IDLE;
				return;
			} else {
				LOG.error("polygons size " + polygons.size());
				for (int i = 0; i < polygons.size(); ++i)
				{
					Polygon p = polygons.get(i);
					LOG.error("p" + i + "("+p.boundingRectangle.x()+","+p.boundingRectangle.y()+")" + "("+p.boundingRectangle.width()+","+p.boundingRectangle.height()+")" +
							p.boundingRectangle.width() * p.boundingRectangle.height());
					LOG.error(p.boundingRectangle.x());
					if (p.boundingRectangle.x() <= 1 || p.boundingRectangle.x() >= 316) // TODO clean this up in the filter
					{
						// kinect is a little goofy on the edges
						polygons.remove(i);
						
					}
				}
				
				if (polygons.size() != 1) // TODO - you must deal with this at some point
				{
					speech.speak("i do not know how to deal with " + polygons.size() + " thing" + ((polygons.size() == 1)?"":"s yet"));	
					context = IDLE;
					return;
				} else {
					// process set of polygons
					boundingBox = polygons.get(0).boundingRectangle;
				}
				
			}
						
			//opencv.notify("publishFrame", name, "publish", SerializableImage.class); //<--- BUG - polygon, name (only should work)
			opencv.publishIplImage(true);
			context = GET_CAMERA_FRAME;
	}
	
	final static String FOUND_CAMERA_IMAGE = "i have found an image";
	
	public void publishIplImage(IplImage image) {
		if (context.equals(GET_CAMERA_FRAME) && image.nChannels() == 3) 
		{
			cameraFrame = image;
			opencv.publishIplImage(false);
			context = FOUND_CAMERA_IMAGE;
			makeTemplate();
		}
	}
	
	// TODO - add intermediate function
	void makeTemplate()
	{		
			// FIXME stop the notification - Problem from other entries?
			opencv.removeNotify("publishFrame", name, "publish", SerializableImage.class);
			opencv.removeFilters();
			opencv.addFilter("PyramidDown1", "PyramidDown");
			opencv.addFilter("Gray1", "Gray");
			opencv.addFilter("MatchTemplate1", "MatchTemplate");
			OpenCVFilterMatchTemplate mt = (OpenCVFilterMatchTemplate)opencv.getFilter("MatchTemplate1");
			mt.rect = boundingBox;
			mt.makeTemplate = true; 
			context = WAIT_FOR_TEMPLATE;
	}
	
	final static String WAIT_FOR_TEMPLATE = "i am waiting for a template";
	
	public void getImageTemplate(IplImage img)
	{
		if (context.equals(WAIT_FOR_TEMPLATE)) 
		{
			template = img;
			context = FOUND_TEMPLATE;
			searchMemory();
		}
	}

	final static String FOUND_TEMPLATE = "i found a template";
	
	public void searchMemory()
	{
			// 1. Create & Fill Temporary Memory
			// 2. Search long term memory
		    // 3. Set appropriate context for next state

			opencv.removeFilters();
			opencv.addFilter("PyramidDown1", "PyramidDown");

			if (memory.isEmpty())
			{
				speech.speak("my memory is a blank slate");
				String s = getPhrase(QUERY_OBJECT);
				speech.speak(s); // need input from user
				context = GET_ASSOCIATIVE_WORD; // asking for GET_ASSOCIATIVE_WORD
				return;
			} else {
				// search memory
				speech.speak("i am searching my memory for this object");
				
				double[] minVal = new double[1];
				double[] maxVal = new double[1];
				IplImage res = null;
				CvPoint minLoc = new CvPoint();
				CvPoint maxLoc = new CvPoint();
				int matchRatio = 0;
				CvPoint tempRect0 = new CvPoint();
				CvPoint tempRect1 = new CvPoint();

				
				for (int i = 0; i < memory.size(); ++i)
				{
					Node n = memory.get(i);
					res = cvCreateImage( cvSize( n.imageData.cvGrayFrame.width() - template.width() + 1, 
							n.imageData.cvGrayFrame.height() - template.height() + 1), IPL_DEPTH_32F, 1 );
					cvMatchTemplate(n.imageData.cvGrayFrame, template, res, CV_TM_SQDIFF);
					// cvNormalize( ftmp[i], ftmp[i], 1, 0, CV_MINMAX );
					cvMinMaxLoc ( res, minVal, maxVal, minLoc, maxLoc, null );
					
					tempRect0.x(minLoc.x());
					tempRect0.y(minLoc.y());
					tempRect1.x(minLoc.x() + template.width());
					tempRect1.y(minLoc.y() + template.height());

					matchRatio = (int)(minVal[0]/((tempRect1.x() - tempRect0.x()) * (tempRect1.y() - tempRect0.y())));

					if (matchRatio < 1500)
					{
						speech.speak("i believe it is a " + n.word);
						speech.speak("with match ratio of ");
						speech.speak("" + matchRatio);						
						break;
					} else {
						speech.speak("i do not know what it is");
						speech.speak("the match ratio was ");
						speech.speak("" + matchRatio);		
						String s = getPhrase(QUERY_OBJECT);
						speech.speak(s); // need input from user
						context = GET_ASSOCIATIVE_WORD; // asking for GET_ASSOCIATIVE_WORD
						return;
						
					}
				}
			}
			
			
			// stop capture - switch to Gray Image
			// setROI
			// surf
			// match Template
			
			// and search associative memory - serializable !! hello DB !
			
			//
			//busy = false;
	}
		

	// -------------- CALLBACKS BEGIN -------------------------
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
