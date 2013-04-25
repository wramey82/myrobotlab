package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.slf4j.Logger;


public class VideoStreamer extends Service implements MemoryChangeListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VideoStreamer.class.getCanonicalName());

	public Memory memory = new Memory();
	
	public VideoStreamer(String n) {
		super(n, VideoStreamer.class.getCanonicalName());	
		memory.addMemoryChangeListener(this);
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}

	@Override
	// callback from memory tree - becomes a broadcast
	public void onPut(String parentPath, Node node) {
		invoke("putNode", parentPath, node);
	}
	
	// TODO - broadcast onAdd event - this will sync gui
	public Node.NodeContext putNode(String parentPath, Node node) {
		return new Node.NodeContext(parentPath, node);
	}

	public void publish(String path, Node node) {
		invoke("publishNode", new Node.NodeContext(path, node));
	}

	public Node.NodeContext publishNode(Node.NodeContext nodeContext) {
		return nodeContext;
	}

	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		VideoStreamer template = new VideoStreamer("template");
		template.startService();

		Memory m = template.memory;
			
		
		m.put("", new Node("k1"));
		m.put("/k1", new Node("k2")); // TODO - check last '/' - if exists remove it ...
		m.put("/k1/k2", new Node("k3"));
		
		log.info("/k1/k2");
		
		m.toXMLFile("m1.xml");
		
		Runtime.createAndStart("gui", "GUIService");

		m.crawlAndPublish();
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
