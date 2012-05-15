package org.myrobotlab.service.data;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.myrobotlab.service.Scheduler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SimpleJob implements Job {

    //private static org.slf4j.Logger _log = LoggerFactory.getLogger(SimpleJob.class);
	public final static Logger LOG = Logger.getLogger(SimpleJob.class.getCanonicalName());

    /**
     * <p>
     * Empty constructor for job initialization
     * </p>
     * <p>
     * Quartz requires a public empty constructor so that the
     * scheduler can instantiate the class whenever it needs.
     * </p>
     */
    public SimpleJob() {
    }

    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a
     * <code>{@link org.quartz.Trigger}</code> fires that is associated with
     * the <code>Job</code>.
     * </p>
     * 
     * @throws JobExecutionException
     *             if there is an exception while executing the job.
     */
    public void execute(JobExecutionContext context)
        throws JobExecutionException {
        LOG.info("*******************************Hello World!********************************* - " + new Date());
        /*
    	try {

	        // Say Hello to the World and display the date/time
	        LOG.info("Hello World! - " + new Date());
	    	String[] params = new String[]{"/b"};
			Process process = Runtime.getRuntime().exec("dir", params);
			LOG.info("process exit value " + process.exitValue());

    	} catch (IOException e) {
			e.printStackTrace();
		}
		*/
    }

}	

