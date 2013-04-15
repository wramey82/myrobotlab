import java.io.Serializable;

public class Data implements Serializable {
	/**
	 * 
	 */
	
	public Data(String x)
	{
		this.name = x;
	}
	
	private static final long serialVersionUID = 1L;
	private String name;
	String a = null;
	String b = "hello";
	boolean c = false;
}
