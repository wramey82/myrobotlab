package org.myrobotlab.service;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_SQDIFF;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.KinectImageNode;
import org.myrobotlab.image.OpenCVFilterKinectDepthMask;
import org.myrobotlab.image.Utils;
import org.myrobotlab.memory.Node;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class FSMTest extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(FSMTest.class.getCanonicalName());
	
	String context = null; 			// current context identifier
	String contextPerson = null; 	// person of current context
	
	// TODO - these services could be accessed via RuntimeEnvironment.service("opencv");
	// TODO - service could be bound or created in an init - bound for remote	
	OpenCV opencv = null;
	SpeechRecognition speechRecognition = null;
	Speech speech = null;
	GUIService gui = null;
	
	Random generator = new Random();
	
	HashMap <String, HashMap<String, String>> phrases = new HashMap <String, HashMap<String,String>>(); 	
	HashMap<String, Node> memory = new HashMap<String, Node>();
	OpenCVFilterKinectDepthMask filter = null; // direct handle to filter

	
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
		/*
		speech.speak("ready");
		*/
	}
	
	public void init ()
	{		
		speechRecognition = new SpeechRecognition ("sphinx");
		speechRecognition.startService();
		speech = new Speech("speech");
		speech.setBackendType(Speech.BACKEND_TYPE_GOOGLE);
		speech.startService();
		speech.setLanguage("en");
		opencv = new OpenCV("opencv");
		opencv.startService();
		gui = new GUIService("gui");
		gui.startService();
		
		speechRecognition.notify("publish", name, "heard", String.class);
		speechRecognition.notify("listeningEvent", name, "listeningEvent");
		
		opencv.notify("publish", name, "publish", KinectImageNode.class); //<--- BUG - polygon, name (only should work)
		opencv.notify("publishIplImageTemplate", name, "getImageTemplate", IplImage.class);
		opencv.notify("publishIplImage", name, "publishIplImage", IplImage.class);
		
		opencv.getDepth = true;
		opencv.addFilter("KinectDepthMask1", "KinectDepthMask");
		filter = (OpenCVFilterKinectDepthMask)opencv.getFilter("KinectDepthMask1");

		// start vision
		opencv.grabberType = "com.googlecode.javacv.OpenKinectFrameGrabber";
		gui.display();
		opencv.capture();

		initPhrases();
		changeState(IDLE);
		speech.speak("my mouth is working");
		speech.speak("my eyes are open");
		//speech.speak("ready");
		//findKinectPolygons();
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

	// TODO - organize & find patterns in the states
	public final static String FIND_OBJECT = "what is this"; // actor
	public final static String HELLO = "hello"; // response
	public final static String YES = "yes"; // response
	public final static String NO = "no"; // response
	public final static String I_AM_NOT_SURE = "i am not sure"; // response
	public final static String I_DO_NOT_UNDERSTAND = "i do not understand";
	public final static String GET_ASSOCIATIVE_WORD = "get associative word";
	public final static String QUERY_OBJECT = "i do not know what it is can you tell me";
	public final static String WAITING_FOR_POLYGONS = "i am waiting for polygons";
	public final static String IDLE = "i am in an idle state";
	public final static String HAPPY = "i am happy";
	public final static String SAD = "i am bummed";
	public final static String FOUND_POLYGONS = "i have found polygons";
	public final static String GET_CAMERA_FRAME = "i am getting an image";
	public final static String WAITING_FOR_AFFIRMATION = "is that correct?";
	
	public final static String UNKNOWN = "i don't know";
	
	
	public void initPhrases()
	{
		// load recognized grammar - keep in sync with simpl.gram
		// TODO - dynamically create a simple.gram file? vs programatically change it??
		// ------------------ SIMPLE.GRAM SYNC BEGIN --------------------------
		HashMap <String, String> t = new HashMap<String, String>(); 
		t.put("find object", null); 
		t.put("look", null); 
		t.put("what is this", null); 
		t.put("what do you see", null);
		t.put("and this", null);
		t.put("what about this", null);
		t.put("do you know what this is", null);
		phrases.put(FIND_OBJECT, t);

		t = new HashMap<String, String>(); 
		t.put("cup", null);
		t.put("measuring thingy", null);
		t.put("beer", null); 
		t.put("box", null); 
		t.put("hand", null); 
		t.put("cup", null); 
		t.put("guitar", null);
		t.put("phone", null);
		t.put("food", null);
		t.put("ball", null);
		t.put("apple", null);
		t.put("orange", null);
		phrases.put(GET_ASSOCIATIVE_WORD, t);
		// ------------------ SIMPLE.GRAM SYNC END --------------------------

		
		t = new HashMap <String, String>();
		t.put("i am looking and waiting", null);
		t.put("i am trying to see an object", null);
		phrases.put(WAITING_FOR_POLYGONS, t);

		t = new HashMap <String, String>();
		t.put("i have found something", null);
		t.put("i can see some object", null);
		t.put("there is an object", null);
		t.put("i see something", null);
		phrases.put(FOUND_POLYGONS, t);
				
		t = new HashMap <String, String>();
		t.put("i dont know. please tell me", null);
		t.put("can you please tell me what it is", null);
		t.put("please tell me what it is", null);
		t.put("what is it", null);
		t.put("would you tell me what that is", null);
		t.put("i do not recognize it. could you tell me", null);
		t.put("i wish i knew", null);
		t.put("what would you call it", null);
		t.put("i dont know. please tell me", null);
		t.put("i have never seen one of those before. what is it", null);
		phrases.put(QUERY_OBJECT, t);
		
		t = new HashMap<String, String>(); 
		t.put("hello", null);
		t.put("greetings", null);
		t.put("yes hello", null);
		t.put("hi there", null);
		t.put("good morning", null);
		phrases.put(HELLO, t);

		t = new HashMap<String, String>(); 
		t.put("no", null);
		t.put("i do not think so", null);
		t.put("no way", null);
		t.put("nope", null);
		t.put("i doubt it", null);
		phrases.put(NO, t);

		t = new HashMap<String, String>(); 
		t.put("yes", null);
		t.put("i believe so", null);
		t.put("most certainly", null);
		t.put("yep", null);
		t.put("affirmative", null);
		t.put("correct", null);
		t.put("yes of course", null);
		t.put("yeah", null);
		phrases.put(YES, t);

		t = new HashMap<String, String>(); 
		t.put(IDLE, null);
		t.put("i am at rest", null);
		t.put("i have stopped", null);
		t.put("i am ready", null);
		t.put("i am calm and will be listening for your next command", null);
		t.put("i am zen", null);
		t.put("i am very still", null);
		phrases.put(IDLE, t);
		
		t = new HashMap<String, String>(); 
		t.put(WAITING_FOR_AFFIRMATION, null);
		t.put("i that right?", null);
		t.put("am i right?", null);
		phrases.put(WAITING_FOR_AFFIRMATION, t);		
		
		t = new HashMap<String, String>(); 
		t.put(HAPPY, null);		
		t.put("great", null);
		t.put("wonderful", null);
		t.put("fabulous", null);
		t.put("kickass", null);
		t.put("i am rockin", null);
		t.put("that makes me feel good", null);
		t.put("excellent", null);
		phrases.put(HAPPY, t);		

		t = new HashMap<String, String>(); 
		t.put(SAD, null);		
		t.put("great", null);
		t.put("wonderful", null);
		t.put("fabulous", null);
		t.put("kickass", null);
		t.put("i am rockin", null);
		t.put("that makes me feel good", null);
		t.put("excellent", null);
		phrases.put(SAD, t);		
		
	}	

	public void heard (String data)
	{
		if (data.equals("save"))
		{
			save();
		}
		
		if (data.equals("stop"))
		{
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
			findKinectPolygons();
		} else if (context.equals(GET_ASSOCIATIVE_WORD) && phrases.get(GET_ASSOCIATIVE_WORD).containsKey(data)) {

			speech.speak("i will associate this with " + data);
			Node n = memory.get(UNKNOWN);
			LOG.error(n.imageData.get(0).cvBoundingBox + "," + n.imageData.get(0).boundingBox);
			n = memory.remove(UNKNOWN); // TODO - work with multiple unknowns
			LOG.error(n.imageData.get(0).cvBoundingBox + "," + n.imageData.get(0).boundingBox);
			n.word = data;
			if (!memory.containsKey(n.word))
			{
				// i have learned something new
				speech.speak("i have learned something new");
				memory.put(data, n);
			} else {
				// i have bound it to something i previously new about
				speech.speak("i have catogorized it");
				Node n2 = memory.get(n.word);
				n2.imageData.add(n.imageData.get(0)); // FIXME - messy
			}
			speech.speak("i have " + memory.size() + " thing" + ((memory.size()>1)?"s":"" + " in my memory"));
			lastAssociativeWord = n.word;
			changeState(IDLE);
		} else {
			speech.speak("i do not understand. we were in context " + context + " but you said " + data);
		}
		
		if (phrases.get(YES).containsKey(data) && context.equals(WAITING_FOR_AFFIRMATION))
		{
			speech.speak(getPhrase(HAPPY));
		}

		// result of the computer incorrectly guessing and associating object
		// need to back out the change - guess only happens if there is a
		// pre-existing memory object - so the image data must be deleted and a
		// new UNKOWN object put back in
		if (phrases.get(NO).containsKey(data) && context.equals(WAITING_FOR_AFFIRMATION))
		{
			speech.speak(getPhrase(SAD));
			// remove last KinectImageData from the "contextWord"
			// moving node out of word context and into the UNKNOWN
			// changing state back to GET_ASSOCIATIVE_WORD
			Node n = memory.get(lastAssociativeWord);
			// remove last image data
			KinectImageNode kin = n.imageData.remove(n.imageData.size()-1);
			Node unknown = new Node();
			unknown.word = UNKNOWN;
			unknown.imageData.add(kin);
			memory.put(UNKNOWN, unknown);
			// try again - notify ready for correct identification
			speech.speak(getPhrase(QUERY_OBJECT));
			changeState(GET_ASSOCIATIVE_WORD);
		}
	
	}
	
	String lastAssociativeWord = null;

	public void findKinectPolygons ()
	{
		filter.publishNodes = true;
		changeState(WAITING_FOR_POLYGONS);
	}


	// FYI - This "SHOULD" not need synchronized as there is only 1 thread
	// servicing the InBox queue - remove after it is determined that it does not
	// solve the problem
	public synchronized void publish(ArrayList<KinectImageNode> p) {
		LOG.error("found " + p.size() + " contextImageDataObjects");
		filter.publishNodes = false;
		
		// replacing all with current set - in future "unknown" objects can be concatenated
		/// you could further guard by a new context
		if (context.equals(WAITING_FOR_POLYGONS))
		{
			// invoking occurs on the same thread....
			// this "should" be thread safe with the syncrhonized call
			//invoke("changeState", FOUND_POLYGONS);
			changeState(FOUND_POLYGONS);

			Node n = new Node();
			n.word = UNKNOWN;
			n.imageData = p;
			memory.put(UNKNOWN, n);
			
			// the data arrives on the InBox (from the VideoProcessor Thread)
			// the processing of the InBox message is done by the FSMTest thread
			// which invoked by the Message call processPolygons

			processPolygons();
		}
	}
		
	public void processPolygons()
	{	
		Node object = memory.get(UNKNOWN);
		invoke("", object);
				
		if (object.imageData.size() != 1)
		{
			speech.speak("i do not know how to deal with " + object.imageData.size() + " thing" + ((object.imageData.size() == 1)?"":"s yet"));
			changeState(IDLE);
			return;
		}
			
		// matchTemplate - adaptive match - non-match
		if (memory.size() == 1) // unknown objects only
		{
			//speech.speak("my memory is empty, except for the unknown");
			speech.speak(getPhrase(QUERY_OBJECT)); // need input from user
			changeState(GET_ASSOCIATIVE_WORD);
			return;
		}
		
		
		invoke("clearVideo0");
		
		// run through - find best match - TODO - many other algorithms and techniques
		Iterator<String> itr = memory.keySet().iterator();
		Node unknown = memory.get(UNKNOWN);
		LOG.error( unknown.imageData.get(0).cvBoundingBox);
		LOG.error( unknown.imageData.get(0).boundingBox);
		int bestFit = 1000;
		int fit = 0;
		String bestFitName = null;
		
		while (itr.hasNext()) {
			String n = itr.next();
			if (n.equals(UNKNOWN))
			{
				continue; // we won't compare the unknown thingy with itself
			}
			Node toSearch = memory.get(n);
			fit = match(toSearch, unknown);

			toSearch.imageData.get(0).lastGoodFitIndex = fit;
			
			if (fit < bestFit)
			{
				bestFit = fit;
				bestFitName = n;
			}
		}

		invoke("publishVideo0", memory);

		if (bestFit < 500)
		{
		// if found
		    // announce - TODO - add map "i think it might be", i'm pretty sure its a, 
			speech.speak("i think it's a " + bestFitName);
			Node n = memory.get(bestFitName);
			n.imageData.add(unknown.imageData.get(0)); // FIXME - messy
			// with a match ratio of ....
			// is that correct?
			// context = WAITING_FOR_AFFIRMATION
		} else {
		// else
			// associate word
			speech.speak("i do not know what it is");
			speech.speak(getPhrase(QUERY_OBJECT));
			changeState(GET_ASSOCIATIVE_WORD);
		}
	}
	
	IplImage result = null;
	double[] minVal = new double[1];
	double[] maxVal = new double[1];
	CvPoint minLoc = new CvPoint();
	CvPoint maxLoc = new CvPoint();
	CvPoint tempRect0 = new CvPoint();
	CvPoint tempRect1 = new CvPoint();

	int resultWidth 	= 0; 
	int resultHeight 	= 0;


	// FIXME - bury in KinectDepthMask or other OpenCV filter to 
	// get it working on the same thread only ...
	// Don't use CVObjects out of OpenCV
	int match (Node toSearch, Node unknown)
	{
		//invoke("publishVideo0", toSearch);
		//invoke("publishVideo1", unknown);

		IplImage frame    = toSearch.imageData.get(0).cvCameraFrame;
		IplImage template = unknown.imageData.get(0).cvCameraFrame;
		
		// TODO - optimization would be to set image roi on the frame
		// although it would need to check the templates size and adjust
		// if necessary
		resultWidth  = frame.width() - (int)unknown.imageData.get(0).boundingBox.getWidth() + 1;
		resultHeight = frame.height() - (int)unknown.imageData.get(0).boundingBox.getHeight() + 1;

		// TODO - dump an array of Node memory into a VideoWidget with different source names
		// TODO - when adding to memory - process all type conversions
		
	
		if (result == null || resultWidth != result.width() || resultHeight != result.height())
		{
			// result = cvCreateImage( cvSize( frame.width() - template.width() + 1, 
			//		frame.height() - template.height() + 1), IPL_DEPTH_32F, 1 );
			result = cvCreateImage( cvSize( resultWidth, resultHeight), IPL_DEPTH_32F, 1 );
		}
		CvRect rect = new CvRect();
		rect.x(unknown.imageData.get(0).boundingBox.x);
		rect.y(unknown.imageData.get(0).boundingBox.y);
		rect.width(unknown.imageData.get(0).boundingBox.width);
		rect.height(unknown.imageData.get(0).boundingBox.height);
		cvSetImageROI(template, rect); 
		//cvSetImageROI(result, cvRect(0, 0, frame.width() - template.width() + 1, frame.height() - template.height() + 1)); 
			
		cvMatchTemplate(frame, template, result, CV_TM_SQDIFF);
		
		cvResetImageROI(template);
		
		cvMinMaxLoc ( result, minVal, maxVal, minLoc, maxLoc, null );
		// publish result
		// invoke("publishMatchResult", result);
		
		tempRect0.x(minLoc.x());
		tempRect0.y(minLoc.y());
		tempRect1.x(minLoc.x() + template.width());
		tempRect1.y(minLoc.y() + template.height());

		int matchRatio = (int)(minVal[0]/((tempRect1.x() - tempRect0.x()) * (tempRect1.y() - tempRect0.y())));		


		return matchRatio;
	}
	
	/*
	 * TODO - add publishing points for image review back to the FSM Gui
	 * 
	 */
	// -------------- CALLBACKS BEGIN -------------------------
	public CvPoint publish(CvPoint p) {
		LOG.info("got point " + p);
		return p;
	}	

	public HashMap<String, Node> publishVideo0(HashMap<String, Node> memory) {
		return memory;
	}	

	// event to clear the GUI's FSMTest video
	public void clearVideo0() {
	}	
	
	public Node publishVideo0(Node o) {
		return o;
	}	
	
	public Node publishVideo1(Node o) {
		return o;
	}	

	public Node publishVideo2(Node o) {
		return o;
	}	

	public IplImage publishMatchResult(IplImage o) {
		LOG.info("publishMatchResult" + o);
		return o;
	}	
	
	public String changeState (String newState)
	{
		context = newState;
		speech.speak(getPhrase(context));
		return newState;
	}
	
	// -------------- CALLBACKS END -------------------------
	
	public String getPhrase (String input)
	{
		if (phrases.containsKey(input))
		{
			Object[] keys = phrases.get(input).keySet().toArray();
			if (keys.length == 0)
			{
				return "i can only find a single key context, which is, " + input;
			}
			String randomValue = (String)keys[generator.nextInt(keys.length)];
			return randomValue;
		} else {
			return "i would like to express what i am doing, but i can't for, " + input;
		}
	}
	
	// TODO - WebService Call to POST GET and search memory - Jibble it with REST - use new MRL.net utils
	public void save() //saveMemory
	{
		// save to file system in html format vs database
		Iterator<String> itr = memory.keySet().iterator();
		
		StringBuffer html = new StringBuffer();
		html.append("<html><head><head><body>");
		html.append("<table class=\"memoryTable\">");
		html.append("<tr><td><b>word</b></td><td><b>image</b></td></tr>\n");
		
		while (itr.hasNext()) {
			String n = itr.next();
			Node node = memory.get(n);
			html.append("<tr><td>");
			html.append(node.word);
			html.append("</td><td>");
			for (int i = 0; i < node.imageData.size(); ++i)
			{	
				KinectImageNode kin = node.imageData.get(i);
				//kin.extraDataLabel
				// TODO - write bounding box - mask & crop image - do this at node level?
				// in filter
				String word = node.word;
				new File("html/images/"+word).mkdirs();
				
				html.append("<img src=\"images/"+word+"/cropped_" + i + ".jpg\" />");
				Utils.saveBufferedImage(kin.cameraFrame.getImage(), "html/images/"+word+"/cameraFrame_" + i +".jpg");
				Utils.saveBufferedImage(kin.cropped.getImage(), "html/images/"+word+"/cropped_" + i +".jpg");
				// TODO - masked/alpha - info.txt file to parse (db at some point) - index values - reference values
				/*
				Graphics g = bi.getGraphics();
				g.setColor(Color.WHITE);
				Rectangle r = kin.boundingBox;
				g.drawRect(r.x, r.y, r.width, r.height);
				g.dispose();
				*/
			}
			html.append("</td></tr>\n");

		}
		html.append("</table>");
		html.append("</body>");
		html.append("</html>");
		
		Writer out;
		try {
			out = new OutputStreamWriter(new FileOutputStream("html/index.html"), "UTF-8");
			out.write(html.toString());
		    out.close();
		} catch (Exception e) {
			Service.logException(e);
		}

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
