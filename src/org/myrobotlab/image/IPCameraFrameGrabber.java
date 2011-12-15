package org.myrobotlab.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class IPCameraFrameGrabber extends FrameGrabber {

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
		url = new URL("http://" + host + "/videostream.cgi??user=" + user
				+ "&pwd=" + password);
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
		byte[] buffer = new byte[4096];// MTU or JPG Frame Size?
		int n = -1;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// verify http header
		
		// 0xFF, 0xD8 - start memory jpeg buffer
		// 0xFF, 0xD9 - scan for end of jpeg
		// use http headers for read size ???? <optimization>>
		// http://stackoverflow.com/questions/4585527/detect-eof-for-jpg-images markers
		
		// convert memory cache to serializable image
		
		String file = "out.jpg";
		OutputStream output = new FileOutputStream(file);
		while ((n = input.read(buffer)) != -1) {
			if (n > 0) {
				baos.write(buffer);
				output.write(buffer, 0, n);
			}
		}

		// MemoryCacheImageOutputStream output = new
		// MemoryCacheImageOutputStream(new FileOutputStream (file));
		
		BufferedImage bi = ImageIO.read(new ByteArrayInputStream (baos.toByteArray()));
		
		return IplImage.createFrom(bi);
	}
	
	public BufferedImage grabBufferedImage()
	{
		return null;
	}

	@Override
	public void release() throws Exception {
	}

}
