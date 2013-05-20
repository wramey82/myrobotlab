package org.myrobotlab.service;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serializable;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class Mouse extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Mouse.class
			.getCanonicalName());
	private Robot robot;
	private Point mousePos;
	private Point oldMousePos;
	private Rectangle bounds;
	public final static int BUTTON1_MASK = InputEvent.BUTTON1_MASK;
	public final static int BUTTON2_MASK = InputEvent.BUTTON2_MASK;
	public final static int BUTTON3_MASK = InputEvent.BUTTON3_MASK;
	public final static int SHIFT_DOWN_MASK = KeyEvent.SHIFT_DOWN_MASK;
	public final static int CTRL_DOWN_MASK = KeyEvent.CTRL_DOWN_MASK;
	public final static int ALT_DOWN_MASK = KeyEvent.ALT_DOWN_MASK;

	public class MouseData implements Serializable {
		private static final long serialVersionUID = 1L;
		float x = 0;
		float y = 0;

		public MouseData(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	public class KeyData implements Serializable {
		private static final long serialVersionUID = 1L;
		char c = 0;
		int keyCode = 0;
		int modifier = 0;

		public KeyData(char c, int keyCode, int modifier) {
			this.c = c;
			this.keyCode = keyCode;
			this.modifier = modifier;
		}
	}

	public class MouseThread implements Runnable {
		public Thread thread = null;
		private boolean isRunning = true;

		MouseThread() {
			thread = new Thread(this, getName() + "_polling_thread");
			thread.start();
		}

		public void run() {
			try {
				while (isRunning) {
					mousePos = MouseInfo.getPointerInfo().getLocation();
					if (!mousePos.equals(oldMousePos)) {
						invoke("publishMouseX", new Float(
								((float) mousePos.x / bounds.getWidth())));
						invoke("publishMouseY", new Float(
								((float) mousePos.y / bounds.getHeight())));
						invoke("publishMouse", new MouseData((float) mousePos.x
								/ (float) bounds.getWidth(), (float) mousePos.y
								/ (float) bounds.getHeight()));

					}
					oldMousePos = mousePos;
					Thread.sleep(200);
				}
			} catch (InterruptedException e) {
				log.info("ClockThread interrupt");
				isRunning = false;
			}
		}
	}

	public Mouse(String n) {
		super(n, Mouse.class.getCanonicalName());
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		bounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int j = 0; j < gs.length; j++) {
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration[] gc = gd.getConfigurations();
			for (int i = 0; i < gc.length; i++) {
				bounds = bounds.union(gc[i].getBounds());
			}
		}
		new MouseThread();
	}

	@Override
	public String getToolTip() {
		return "Mouse service, also allow programmatic control of mouse/keyboard/screen capture";
	}

	@Override
	public void stopService() {
		super.stopService();
		robot = null;
	}

	@Override
	public void releaseService() {
		super.releaseService();
		robot = null;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Runtime.createAndStart("java", "Java");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("mouse", "Mouse");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

	// publish methods -------------------------
	public Float publishMouseX(Float value) {
		return value;
	}

	public Float publishMouseY(Float value) {
		return value;
	}

	public MouseData publishMouse(MouseData value) {
		return value;
	}

	public void moveTo(MouseData value) {
		robot.mouseMove((int) (value.x * (float) bounds.width),
				(int) (value.y * (float) bounds.height));
	}

	public void moveTo(float x1, float y1) {
		robot.mouseMove((int) ((float) x1 * (float) bounds.width),
				(int) (y1 * (float) bounds.height));
	}

	// buttons = use bit mask constants
	public void click(final int buttons) {
		new Thread(new Runnable() {
			public void run() {
				robot.mousePress(buttons);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				robot.mouseRelease(buttons);

			}
		}).start();
	}

	public void type(final char c) {
		new Thread(new Runnable() {
			public void run() {
				KeyData cc = getKeyEventFromChar(c);
				int keyCode = cc.keyCode;
				if ((cc.keyCode & Mouse.SHIFT_DOWN_MASK) == Mouse.SHIFT_DOWN_MASK) {
					robot.keyPress(KeyEvent.VK_SHIFT);
				}
				if ((cc.keyCode & Mouse.CTRL_DOWN_MASK) == Mouse.CTRL_DOWN_MASK) {
					robot.keyPress(KeyEvent.VK_CONTROL);
				}
				if ((cc.keyCode & Mouse.ALT_DOWN_MASK) == Mouse.ALT_DOWN_MASK) {
					robot.keyPress(KeyEvent.VK_ALT);
				}
				robot.keyPress(keyCode);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				robot.keyRelease(keyCode);
				if ((cc.keyCode & Mouse.SHIFT_DOWN_MASK) == Mouse.SHIFT_DOWN_MASK) {
					robot.keyRelease(KeyEvent.VK_SHIFT);
				}
				if ((cc.keyCode & Mouse.CTRL_DOWN_MASK) == Mouse.CTRL_DOWN_MASK) {
					robot.keyRelease(KeyEvent.VK_CONTROL);
				}
				if ((cc.keyCode & Mouse.ALT_DOWN_MASK) == Mouse.ALT_DOWN_MASK) {
					robot.keyRelease(KeyEvent.VK_ALT);
				}

			}
		}).start();
	}

	public void type(final String s) {
		new Thread(new Runnable() {
			public void run() {
				for (char c : s.toCharArray()) {
					KeyData cc = getKeyEventFromChar(c);
					int bb = cc.keyCode;
					int keyCode = cc.keyCode;
					if ((cc.keyCode & Mouse.SHIFT_DOWN_MASK) == Mouse.SHIFT_DOWN_MASK) {
						robot.keyPress(KeyEvent.VK_SHIFT);
					}
					if ((cc.keyCode & Mouse.CTRL_DOWN_MASK) == Mouse.CTRL_DOWN_MASK) {
						robot.keyPress(KeyEvent.VK_CONTROL);
					}
					if ((cc.keyCode & Mouse.ALT_DOWN_MASK) == Mouse.ALT_DOWN_MASK) {
						robot.keyPress(KeyEvent.VK_ALT);
					}
					robot.keyPress(bb);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					robot.keyRelease(bb);
					if ((cc.keyCode & Mouse.SHIFT_DOWN_MASK) == Mouse.SHIFT_DOWN_MASK) {
						robot.keyRelease(KeyEvent.VK_SHIFT);
					}
					if ((cc.keyCode & Mouse.CTRL_DOWN_MASK) == Mouse.CTRL_DOWN_MASK) {
						robot.keyRelease(KeyEvent.VK_CONTROL);
					}
					if ((cc.keyCode & Mouse.ALT_DOWN_MASK) == Mouse.ALT_DOWN_MASK) {
						robot.keyRelease(KeyEvent.VK_ALT);
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	// end publish methods-----------------------

	private KeyData getKeyEventFromChar(final char c) {
		int press = -1;
		boolean shift = false;
		switch (c) {
		case 'a':
			press = (KeyEvent.VK_A);
			break;
		case 'b':
			press = (KeyEvent.VK_B);
			break;
		case 'c':
			press = (KeyEvent.VK_C);
			break;
		case 'd':
			press = (KeyEvent.VK_D);
			break;
		case 'e':
			press = (KeyEvent.VK_E);
			break;
		case 'f':
			press = (KeyEvent.VK_F);
			break;
		case 'g':
			press = (KeyEvent.VK_G);
			break;
		case 'h':
			press = (KeyEvent.VK_H);
			break;
		case 'i':
			press = (KeyEvent.VK_I);
			break;
		case 'j':
			press = (KeyEvent.VK_J);
			break;
		case 'k':
			press = (KeyEvent.VK_K);
			break;
		case 'l':
			press = (KeyEvent.VK_L);
			break;
		case 'm':
			press = (KeyEvent.VK_M);
			break;
		case 'n':
			press = (KeyEvent.VK_N);
			break;
		case 'o':
			press = (KeyEvent.VK_O);
			break;
		case 'p':
			press = (KeyEvent.VK_P);
			break;
		case 'q':
			press = (KeyEvent.VK_Q);
			break;
		case 'r':
			press = (KeyEvent.VK_R);
			break;
		case 's':
			press = (KeyEvent.VK_S);
			break;
		case 't':
			press = (KeyEvent.VK_T);
			break;
		case 'u':
			press = (KeyEvent.VK_U);
			break;
		case 'v':
			press = (KeyEvent.VK_V);
			break;
		case 'w':
			press = (KeyEvent.VK_W);
			break;
		case 'x':
			press = (KeyEvent.VK_X);
			break;
		case 'y':
			press = (KeyEvent.VK_Y);
			break;
		case 'z':
			press = (KeyEvent.VK_Z);
			break;
		case 'A':
			press = (KeyEvent.VK_A);
			shift = true;
			break;
		case 'B':
			press = (KeyEvent.VK_B);
			shift = true;
			break;
		case 'C':
			press = (KeyEvent.VK_C);
			shift = true;
			break;
		case 'D':
			press = (KeyEvent.VK_D);
			shift = true;
			break;
		case 'E':
			press = (KeyEvent.VK_E);
			shift = true;
			break;
		case 'F':
			press = (KeyEvent.VK_F);
			shift = true;
			break;
		case 'G':
			press = (KeyEvent.VK_G);
			shift = true;
			break;
		case 'H':
			press = (KeyEvent.VK_H);
			shift = true;
			break;
		case 'I':
			press = (KeyEvent.VK_I);
			shift = true;
			break;
		case 'J':
			press = (KeyEvent.VK_J);
			shift = true;
			break;
		case 'K':
			press = (KeyEvent.VK_K);
			shift = true;
			break;
		case 'L':
			press = (KeyEvent.VK_L);
			shift = true;
			break;
		case 'M':
			press = (KeyEvent.VK_M);
			shift = true;
			break;
		case 'N':
			press = (KeyEvent.VK_N);
			shift = true;
			break;
		case 'O':
			press = (KeyEvent.VK_O);
			shift = true;
			break;
		case 'P':
			press = (KeyEvent.VK_P);
			shift = true;
			break;
		case 'Q':
			press = (KeyEvent.VK_Q);
			shift = true;
			break;
		case 'R':
			press = (KeyEvent.VK_R);
			shift = true;
			break;
		case 'S':
			press = (KeyEvent.VK_S);
			shift = true;
			break;
		case 'T':
			press = (KeyEvent.VK_T);
			shift = true;
			break;
		case 'U':
			press = (KeyEvent.VK_U);
			shift = true;
			break;
		case 'V':
			press = (KeyEvent.VK_V);
			shift = true;
			break;
		case 'W':
			press = (KeyEvent.VK_W);
			shift = true;
			break;
		case 'X':
			press = (KeyEvent.VK_X);
			shift = true;
			break;
		case 'Y':
			press = (KeyEvent.VK_Y);
			shift = true;
			break;
		case 'Z':
			press = (KeyEvent.VK_Z);
			shift = true;
			break;
		case '`':
			press = (KeyEvent.VK_BACK_QUOTE);
			break;
		case '0':
			press = (KeyEvent.VK_0);
			break;
		case '1':
			press = (KeyEvent.VK_1);
			break;
		case '2':
			press = (KeyEvent.VK_2);
			break;
		case '3':
			press = (KeyEvent.VK_3);
			break;
		case '4':
			press = (KeyEvent.VK_4);
			break;
		case '5':
			press = (KeyEvent.VK_5);
			break;
		case '6':
			press = (KeyEvent.VK_6);
			break;
		case '7':
			press = (KeyEvent.VK_7);
			break;
		case '8':
			press = (KeyEvent.VK_8);
			break;
		case '9':
			press = (KeyEvent.VK_9);
			break;
		case '-':
			press = (KeyEvent.VK_MINUS);
			break;
		case '=':
			press = (KeyEvent.VK_EQUALS);
			break;
		case '~':
			press = (KeyEvent.VK_BACK_QUOTE);
			shift = true;
			break;
		case '!':
			press = (KeyEvent.VK_EXCLAMATION_MARK);
			break;
		case '@':
			press = (KeyEvent.VK_AT);
			break;
		case '#':
			press = (KeyEvent.VK_NUMBER_SIGN);
			break;
		case '$':
			press = (KeyEvent.VK_DOLLAR);
			break;
		case '%':
			press = (KeyEvent.VK_5);
			shift = true;
			break;
		case '^':
			press = (KeyEvent.VK_CIRCUMFLEX);
			break;
		case '&':
			press = (KeyEvent.VK_AMPERSAND);
			break;
		case '*':
			press = (KeyEvent.VK_ASTERISK);
			break;
		case '(':
			press = (KeyEvent.VK_LEFT_PARENTHESIS);
			break;
		case ')':
			press = (KeyEvent.VK_RIGHT_PARENTHESIS);
			break;
		case '_':
			press = (KeyEvent.VK_UNDERSCORE);
			break;
		case '+':
			press = (KeyEvent.VK_PLUS);
			break;
		case '\t':
			press = (KeyEvent.VK_TAB);
			break;
		case '\n':
			press = (KeyEvent.VK_ENTER);
			break;
		case '[':
			press = (KeyEvent.VK_OPEN_BRACKET);
			break;
		case ']':
			press = (KeyEvent.VK_CLOSE_BRACKET);
			break;
		case '\\':
			press = (KeyEvent.VK_BACK_SLASH);
			break;
		case '{':
			press = (KeyEvent.VK_OPEN_BRACKET);
			shift = true;
			break;
		case '}':
			press = (KeyEvent.VK_CLOSE_BRACKET);
			shift = true;
			break;
		case '|':
			press = (KeyEvent.VK_BACK_SLASH);
			shift = true;
			break;
		case ';':
			press = (KeyEvent.VK_SEMICOLON);
			break;
		case ':':
			press = (KeyEvent.VK_COLON);
			break;
		case '\'':
			press = (KeyEvent.VK_QUOTE);
			break;
		case '"':
			press = (KeyEvent.VK_QUOTEDBL);
			break;
		case ',':
			press = (KeyEvent.VK_COMMA);
			break;
		case '<':
			press = (KeyEvent.VK_LESS);
			break;
		case '.':
			press = (KeyEvent.VK_PERIOD);
			break;
		case '>':
			press = (KeyEvent.VK_GREATER);
			break;
		case '/':
			press = (KeyEvent.VK_SLASH);
			break;
		case '?':
			press = (KeyEvent.VK_SLASH);
			shift = true;
			break;
		case ' ':
			press = (KeyEvent.VK_SPACE);
			break;
		default:
			throw new IllegalArgumentException("Cannot type character " + c);
		}
		return new KeyData(c, press, shift ? KeyEvent.SHIFT_DOWN_MASK : 0);

	}
}
