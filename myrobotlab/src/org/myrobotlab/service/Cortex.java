package org.myrobotlab.service;

import static org.myrobotlab.service.OpenCV.FILTER_FACE_DETECT;
import static org.myrobotlab.service.OpenCV.FILTER_PYRAMID_DOWN;

import java.io.File;
import java.util.Map;

import org.myrobotlab.cortex.Algorithm;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.myrobotlab.opencv.OpenCVData;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;

/**
 * @author GroG TODO - move Tracking in Cortex objects to step through
 *         algorithms in call-backs All peer services are accessable directly
 *         revert Tracking
 */
public class Cortex extends Service implements MemoryChangeListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Cortex.class.getCanonicalName());

	public static final String MEMORY_TYPE = "MEMORY_TYPE";
	public static final String MEMORY_OPENCV_DATA = "MEMORY_OPENCV_DATA";
	public static final String MEMORY_TIMESTAMP = "MEMORY_TIMESTAMP";

	// state info
	private String state = STATE_IDLE;
	public final static String STATE_IDLE = "idle";

	public String trackingName = "tracking";
	public String faceDetectorName = "faceDetector";
	public String mouthName = "mouth";
	public String earName = "ear";
	Algorithm algorithm;
	public String serialPort;

	transient Tracking tracking;
	transient OpenCV faceDetector;
	transient OpenCV lkOptical;
	transient Sphinx ear;
	transient Speech mouth;

	// TODO - store all config in memory too?
	private Memory memory = new Memory();

	public Cortex(String n) {
		super(n, Cortex.class.getCanonicalName());
	}

	// FIXME - remove start replacing javadoc & tooltips with annotations
	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	// FIXME FIXME FIXME - make Listening interface !!!!
	// which implments "heard" !!!!
	public void startListening(String grammar) {
		ear.attach(mouth.getName());
		// FIXME - handle Java listener - addListener(real type)
		// ear.addListener("recognized", "python", "heard", String.class);
		ear.addListener("recognized", this.getName(), "heard", String.class);
		ear.createGrammar(grammar);
		ear.startListening();

	}
		
	public void startService()
	{
		super.startService();
		memory.addMemoryChangeListener(this);

		memory.put("/", new Node("past"));
		memory.put("/", new Node("present"));
		memory.put("/", new Node("future")); // <- predictive
		memory.put("/", new Node("locations"));

		memory.put("/present", new Node("background"));
		memory.put("/present", new Node("foreground"));
		memory.put("/present", new Node("faces"));
		memory.put("/present/faces", new Node("unknown"));
		memory.put("/present/faces", new Node("known"));
		memory.put("/present", new Node("objects"));

		memory.put("/past", new Node("background"));
		memory.put("/past", new Node("foreground"));		
		
		faceDetector = (OpenCV) Runtime.createAndStart(faceDetectorName, "OpenCV");
		lkOptical = (OpenCV) Runtime.createAndStart("lkOptical", "OpenCV");
		// ear = (Sphinx) Runtime.createAndStart(earName, "Sphinx");
		// mouth = (Speech) Runtime.createAndStart("mouth", "Speech");
		tracking = (Tracking) Runtime.create(trackingName, "Tracking");
		tracking.eye = lkOptical;
		tracking.startService();
		lkOptical.addFilter("pyramidDown","PyramidDown");
		lkOptical.addFilter("lkOpticalTrack","LKOpticalTrack");
		lkOptical.publishFilterState("lkOpticalTrack");
		lkOptical.setDisplayFilter("lkOpticalTrack");
		lkOptical.capture();
		
		faceDetector.setInpurtSource(OpenCV.INPUT_SOURCE_PIPELINE);
		faceDetector.setPipeline("lkOpticalTrack.pyramidDown");// set key
		faceDetector.addFilter("faceDetect","FaceDetect");
		faceDetector.setDisplayFilter("input");
		faceDetector.setDisplayFilter("faceDetect");
		faceDetector.capture();
		
		// TODO - register data queue
		algorithm = new Algorithm(this);

		// defaults...
		tracking.setRestPosition(90, 5);
		tracking.setSerialPort("COM12");
		tracking.setServoPins(13,12);
		tracking.setCameraIndex(1);

//		tracking.setIdle();

		tracking.startVideoStream();
		subscribe("toProcess", tracking.getName(), "process", OpenCVData.class);		
		faceDetector.broadcastState();
		lkOptical.broadcastState();
		
		
	}
	

	/**
	 * FIXME - Tracking should probably not publish Memory it should Publish OpenCVData !!!!!
	 * TODO - facilities to climb up and down nodes
	 * 
	 * @param data
	 */
	public void process(OpenCVData data) {
		// TODO add Algorithm processor
		
		algorithm.process(data);
		
		/*
		Node node = new Node(Long.toString(data.getTimestamp()));
		node.put(MEMORY_OPENCV_DATA, data);
		
		memory.put("/unprocessed", node);
		*/
		/*
		if (BACKGROUND.equals(data.get(MEMORY_TYPE))) {
			memory.put("/unprocessed/background", data);
			memory.put("/locations", data);
			
			process(String.format("/unprocessed/background/%s", data.getName()), "/processed/faces");

		} else if (FOREGROUND.equals(data.get(MEMORY_TYPE))) {
			memory.put("/unprocessed/foreground", data);
		} else {
			log.info("unknown type");
			memory.put("/unprocessed/background", data);
		}
		*/

	}

	public void process(String src, String dst) {
		// for node blah blah
		Node node = memory.getNode(src);

		if (node == null) {
			log.error("could not process {} not valid node", src);
			return;
		}

		for (Map.Entry<String, ?> nodeData : node.getNodes().entrySet()) {
			String key = nodeData.getKey();
			Object object = nodeData.getValue();
			log.info("{}{}", key, object);

			// display based on type for all non-recursive memory
			Class<?> clazz = object.getClass();
			if (clazz != Node.class) {
				if (clazz == OpenCVData.class) {
					OpenCVData data = (OpenCVData) object;
					// single output - assume filter is set to last
					OpenCVData cv = faceDetector.add(data.getInputImage());
					Node pnode = new Node(node.getName());
					pnode.put(MEMORY_OPENCV_DATA, cv);
					if (cv.getBoundingBoxArray() != null)
					{
						log.info("found faces");
						memory.put(dst, pnode);
					}
					
				}
			}
		}
	}

	public void saveMemory() {
		saveMemory(null);
	}

	public void saveMemory(String infilename) {
		String filename;

		if (infilename == null) {
			filename = String.format("memory.%d.xml", System.currentTimeMillis());
		} else {
			filename = infilename;
		}

		try {
			Serializer serializer = new Persister();

			// SerializableImage img = new SerializableImage(ImageIO.read(new
			// File("opencv.4084.jpg")), "myImage");
			File xml = new File(filename);
			serializer.write(memory, xml);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void setState(String newState) {
		state = newState;
		info(state);
	}

	// ---------publish begin ----------
	// publish means update if it already exists
	public void publish(String path, Node node) {
		invoke("publishNode", new Node.NodeContext(path, node));
	}

	public Node.NodeContext publishNode(Node.NodeContext nodeContext) {
		return nodeContext;
	}

	// callback from memory tree - becomes a broadcast
	public void onPut(String parentPath, Node node) {
		invoke("putNode", parentPath, node);
	}

	// TODO - broadcast onAdd event - this will sync gui
	public Node.NodeContext putNode(String parentPath, Node node) {
		return new Node.NodeContext(parentPath, node);
	}

	public String publishStatus(String status) {
		return status;
	}

	// ---------publish end ----------

	public void videoOff() {
		tracking.stopVideoStream();
	}

	public void crawlAndPublish() {
		memory.crawlAndPublish();
	}


	public OpenCV getProcessor() {
		return faceDetector;
	}

	public Memory getMemory() {
		return memory;
	}

	public Tracking getTracking() {
		return tracking;
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Cortex cortex = (Cortex) Runtime.createAndStart("cortex", "Cortex");
		// cortex.videoOff();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		// cortex.add("root", new Node("background"));
		// cortex.add("root", new Node("foreground"));

		log.info("here");
		cortex.setState(STATE_IDLE);

	}


}
