package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class LIDAR extends Service {

    private static final long serialVersionUID = 1L;
    public final static Logger log = LoggerFactory.getLogger(LIDAR.class.getCanonicalName());
    public static final String MODEL_SICK_LMS200 = "SICK LMS200";
    public String serialName;
    public transient Serial serial;
    public ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    String model;
    // states
    public static final String STATE_PRE_INITIALIZATION = "state pre initialization";
    public static final String STATE_INITIALIZATION_STAGE_1 = "state initialization stage 1";
    public static final String STATE_INITIALIZATION_STAGE_2 = "state initialization stage 2";
    public static final String STATE_INITIALIZATION_STAGE_3 = "state initialization stage 3";
    public static final String STATE_INITIALIZATION_STAGE_4 = "state initialization stage 4";
    public static final String STATE_SINGLE_SCAN = "taking a single scan";
    public static final String STATE_MODE_CHANGE = "changing mode";
    public static final String STATE_NOMINAL = "waiting on user to tell me what to do";
    public int dataMessageSize = 0;
    String state = STATE_PRE_INITIALIZATION;

    public LIDAR(String n) {
        super(n, LIDAR.class.getCanonicalName());
        reserve(String.format("%s_serial", n), "Serial", "serial port for LIDAR");
    }

    @Override
    public String getToolTip() {
        return "The LIDAR service";
    }

    public void startService() {
        super.startService();

        try {
            serial = getSerial();
            // setting callback / message route
            serial.addListener("publishByte", getName(), "byteReceived");
            serial.startService();
            if (model == null) {
                model = MODEL_SICK_LMS200;
            }

            // start LIDAR hardware initialization here
            // data coming back from the hardware will be in byteRecieved
            if (MODEL_SICK_LMS200.equals(model)) {
                serial.write(new byte[]{1, 38, 32, 43});
            }
            state = STATE_INITIALIZATION_STAGE_1;
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public void write(byte[] command) {
        // iterate through the byte array sending each one to the serial port.
        for (int i = 0; i < command.length; i++) {
            try {
                serial.write(command[i]);
            } catch (IOException e) {
                Logging.logException(e);
            }

        }
    }

    public void byteReceived(Byte b) {
        try {
            buffer.write(b);
            // so a byte was appended
            // now depending on what model it was and
            // what stage of initialization we do that funky stuff
            if (MODEL_SICK_LMS200.equals(model) && STATE_MODE_CHANGE.equals(state) && buffer.size() == 14) {
                // These modes always have 14 bytes replies
                log.info(buffer.toString());
                byte[] message = buffer.toByteArray();
                if (message[5] == 1 || message[6] == 1) {
                    log.info("Success!!!");
                }
                if (message[5] == 0 || message[6] == 0) {
                    log.error("Sorry dude, but I failed to change mode. Please try again.");
                }
                state = STATE_NOMINAL;
            }
            if (MODEL_SICK_LMS200.equals(model) && STATE_SINGLE_SCAN.equals(state) && buffer.size() == dataMessageSize) {
                log.info(buffer.toString());
                // WTF do I do with this data now?
                state = STATE_NOMINAL;
            }

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public boolean connect(String port) {
        serial = getSerial();
        boolean connect = serial.connect(port, 9600, 8, 1, 0);

        return serial.isConnected();
    }

    public Serial getSerial() {
        if (serialName == null) {
            serialName = String.format("%s_serial", getName());
        }
        serial = (Serial) Runtime.create(serialName, "Serial");
        return serial;
    }

    public void setModel(String m) {
        model = m;
    }

    public void singleScan() throws IOException {
        state = STATE_SINGLE_SCAN;
        serial.write(new char[]{0x02, 0x00, 0x02, 0x00, 0x30, 0x01, 0x31, 0x18});
    }// end singleScan

    public void setMode(int spread, float angularResolution) throws IOException {
        state = STATE_MODE_CHANGE;
        if (spread == 100) {
            if (angularResolution == 1) {
                serial.write(new char[]{0x02, 0x00, 0x05, 0x00, 0x3B, 0x64, 0x00, 0x64, 0x00, 0x1D, 0x0F});
                // Start bytes and header = 8 bytes, 202 data bytes, 1 status
                // and 2 bytes for checksum
                dataMessageSize = 213;
            } else if (angularResolution == 0.5) {
                serial.write(new char[]{0x02, 0x00, 0x05, 0x00, 0x3B, 0x64, 0x00, 0x32, 0x00, 0xb1, 0x59});
                // Start bytes and header = 8 bytes, 402 data bytes, 1 status
                // and 2 bytes for checksum
                dataMessageSize = 413;
            } else if (angularResolution == 0.25) {
                serial.write(new char[]{0x02, 0x00, 0x05, 0x00, 0x3B, 0x64, 0x00, 0x19, 0x00, 0xe7, 0x72});
                // Start bytes and header = 8 bytes, 802 data bytes, 1 status
                // and 2 bytes for checksum
                dataMessageSize = 813;
            } else {
                log.error("You've defined an unsupported Mode");
            }
        }// end if spread = 100
        if (spread == 100) {
            if (angularResolution == 1) {
                serial.write(new char[]{0x02, 0x00, 0x05, 0x00, 0x3B, 0xB4, 0x00, 0x64, 0x00, 0x97, 0x49});
                // Start bytes and header = 8 bytes, 362 data bytes, 1 status
                // and 2 bytes for checksum
                dataMessageSize = 873;
            } else if (angularResolution == 0.5) {
                serial.write(new char[]{0x02, 0x00, 0x05, 0x00, 0x3B, 0xB4, 0x00, 0x32, 0x00, 0x3B, 0x1F});
                // Start bytes and header = 8 bytes, 722 data bytes, 1 status
                // and 2 bytes for checksum
                dataMessageSize = 733;
            } else {
                log.error("You've defined an unsupported Mode");
            }
        }// end if spread = 180
    }// end of setMode

    public static void main(String[] args) {
        LoggingFactory.getInstance().configure();
        LoggingFactory.getInstance().setLevel(Level.INFO);

        try {

        LIDAR template = new LIDAR("lidar");
        template.startService();


//            LIDAR lidar01 = (LIDAR) Runtime.createAndStart("lidar01", "LIDAR");
            // creates and runs a serial service
//			lidar01.connect("dev/lidar01");
            // send a command
            // this sets the mode to a spread of 180 degrees with readings every
            // 0.5
            // degrees
//			lidar01.setMode(180, 0.5f);
            // this setMode command catches the reply from the LMS in the
            // listener
            // within the
            // LIDAR service and returns a bool stating if it was successful or
            // not.

            // an array of floats holding ranges (after the LDIAR service strips
            // and
            // parses the data.
//			lidar01.singleScan();



            Python python = new Python("python");
            python.startService();

            Runtime.createAndStart("gui", "GUIService");
            /*
             * GUIService gui = new GUIService("gui"); gui.startService();
             * gui.display();
             */

        } catch (Exception e) {
            Logging.logException(e);
        }
    }
}