package org.myrobotlab.memory;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;

/**
 * @author GroG a "very" generalized memory node - potentially used to grow
 *         associations with other nodes uses the concept of attributes and
 *         free-form associations
 * 
 */
@Root
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Node.class.getCanonicalName());

	private String name;
	@Element
	public double feelingIndex = 0;
	@Element
	public double timestamp = System.currentTimeMillis();

	@ElementMap(entry = "data", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, Object> data = new HashMap<String, Object>();

	/**
	 * NodeContext is used with messaging to publish updates of nodes. A
	 * location parentPath is needed since the node is not aware of its
	 * location.
	 */
	public static class NodeContext {
		public String parentPath;
		public Node node;

		public NodeContext(String parentPath, Node node) {
			this.parentPath = parentPath;
			this.node = node;
		}
	}

	public Node(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public HashMap<String, Object> getNodes() {
		return data;
	}

	public Object put(String key, Object value) {
		return data.put(key, value);
	}

	public Node put(Node node) {
		return (Node) data.put(node.getName(), node);
	}

	/**
	 * a convienent cast method to get a node
	 * 
	 * @param path
	 * @return
	 */
	public Node getNode(String path) {
		return (Node) get(path);
	}

	/**
	 * the most important method get is the effective "search" method of memory.
	 * It has an XPath like syntax the "/" means "data of" a node, so when the
	 * path is /k1/k2/ - would mean get the hashmap of k2 /k1/k2 - means get the
	 * key value of k2 in the hashmap of k1
	 * 
	 * other examples /background /background/position/x /background/position/y
	 * /foreground /known/ball/red /known/ball/yellow /known/cup
	 * /unknown/object1 /positions/x/ <map> /positions/y/ <map> /positions/time/
	 * <map> /tracking
	 * 
	 * @param path
	 * @return
	 */
	public Object get(String path) {
		if (path == "") {
			return this;
		}
		if (path == "/") {
			return this.data;
		}

		int pos0 = path.indexOf('/');
		if (pos0 != -1) {
			int pos1 = path.indexOf('/', pos0 + 1);
			String subpath;
			String remaining;
			if (pos1 != -1) {
				subpath = path.substring(pos0 + 1, pos1);
				remaining = path.substring(pos0 + 1);
			} else {
				subpath = path.substring(pos0 + 1);
				remaining = subpath;
			}

			if (data.containsKey(subpath)) {
				Object o = data.get(subpath);
				Class<?> c = o.getClass();
				if (c == Node.class) {
					return ((Node) o).get(remaining);
				} else {
					return o;
				}
			}
		} else if (data.containsKey(path)) {
			return data.get(path);
		}

		if (path.equals(name)) {
			return this;
		}

		return null;

	}

	public int size() {
		return data.size();
	}

	public static void main(String[] args) {
		try {
			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			Serializer serializer = new Persister();

			Node root = new Node("root");
			root.put("key1", "value1");
			root.put("key2", "value2");
			Node node2 = new Node("node2");
			root.put("node2", node2);
			node2.put("subkey1", "subValue");
			node2.put("subIntKey", 5);
			Node node3 = new Node("node3");
			node3.put("subkey1", "value3");
			node2.put("node3", node3);
			Node node4 = root.getNode("root/node2/node3");
			log.info("{}", node4.get("subkey1"));

			SerializableImage img = new SerializableImage(ImageIO.read(new File("opencv.4084.jpg")), "myImage");
			node2.put("img", img);

			File cfg = new File(String.format("node.xml"));
			serializer.write(root, cfg);
			log.info("here");
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}