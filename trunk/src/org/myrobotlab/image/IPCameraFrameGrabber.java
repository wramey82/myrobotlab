package org.myrobotlab.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class IPCameraFrameGrabber extends FrameGrabber {

	public final static Logger LOG = Logger.getLogger(IPCameraFrameGrabber.class.getCanonicalName());

	private String host;
	private String user;
	private String password;
	private URL url;
	private URLConnection connection;
	private InputStream input;

	public IPCameraFrameGrabber (String host, String user, String password)
	{
		this.host = host;
		this.user = user;
		this.password = password;
	}
	
	@Override
	public void start() throws Exception {
		url = new URL("http://" + host + "/videostream.cgi?user=" + user
				+ "&pwd=" + password);
		LOG.error(url);
		connection = url.openConnection();
		input = connection.getInputStream();
	}

	@Override
	public void stop() throws Exception {
		//connection.
		input.close();
		input = null;
		connection = null;
		url = null;				
	}

	@Override
	public void trigger() throws Exception {
	}

	@Override
	public IplImage grab() throws Exception {
		return IplImage.createFrom(grabBufferedImage());
	}
	
	public BufferedImage grabBufferedImage() throws Exception
	{
		byte[] buffer = new byte[4096];// MTU or JPG Frame Size?
		int n = -1;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
		StringBuffer sb = new StringBuffer();
		int total = 0;
		int c;
		// read http header
		while ((c = input.read()) != -1) {
			if (c > 0) {
				sb.append((char)c);
				if (c == 13)
				{
					sb.append((char)input.read());// '10'
					c = input.read();
					sb.append((char)c);
					if (c == 13)
					{
						sb.append((char)input.read());// '10'
						break; // done with header
					}
					
				}
			}
		}
		// find size of embedded jpeg in stream
		String header = sb.toString();
		int c0 = header.indexOf("Content-Length: ") + 16;
		int c1 = header.indexOf('\r',c0); 
		int contentLength = Integer.parseInt(header.substring(c0,c1));
		LOG.info("Content-Length: " + contentLength);
		// adaptive size - careful - don't want a 2G jpeg
		if (contentLength > buffer.length)
		{
			buffer = new byte[contentLength]; 
		}
				
		n = -1;
		total = 0;
		while ((n = input.read(buffer, 0, contentLength - total)) != -1) {
			total += n;
			baos.write(buffer, 0, n);
			if (total == contentLength) {
				break;
			}
		}
		
		baos.flush();
		//LOG.info("wrote " + baos.size() + "," + total);
		BufferedImage bi = ImageIO.read(new ByteArrayInputStream (baos.toByteArray()));
		return bi;	
	}

	@Override
	public void release() throws Exception {
	}

}
