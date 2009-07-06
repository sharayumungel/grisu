package org.vpac.grisu.client.control.example;

import org.apache.commons.lang.StringUtils;
import org.vpac.grisu.client.control.login.LoginHelpers;
import org.vpac.grisu.client.model.JobObject;
import org.vpac.grisu.control.GrisuRegistry;
import org.vpac.grisu.control.JobConstants;
import org.vpac.grisu.control.ServiceInterface;

public class CreateJobAndSubmitJobAndCheckJobInDifferentStages {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		ServiceInterface si = LoginHelpers.login();
		
		JobObject createJobObject = new JobObject(si);
		
		createJobObject.setCommandline("java -version");
		createJobObject.setWalltimeInSeconds(3600*24*40);
		createJobObject.setSubmissionLocation("dque@tango-m:ng2.vpac.org");
		createJobObject.setCpus(8);
		
		
		GrisuRegistry registry = GrisuRegistry.getDefault(si);
		System.out.println(StringUtils.join(registry.getApplicationInformation("java").getAvailableSubmissionLocationsForFqan("/ARCS/NGAdmin"),"\n"));
		
		String newJobname = createJobObject.createJob("/ARCS/NGAdmin", ServiceInterface.TIMESTAMP_METHOD);
		
		JobObject submitJobObject = new JobObject(si, newJobname);
		
		System.out.println("Application: "+submitJobObject.getApplication());
		
		submitJobObject.submitJob();
		
		
		final JobObject checkJobObject = new JobObject(si, newJobname);
		
		new Thread() {
			public void run() {
				try {
					Thread.sleep(20000);
					System.out.println("Sleeping over.");
					checkJobObject.stopWaitingForJobToFinish();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
		
		boolean finished = checkJobObject.waitForJobToFinish(3);
		
		if ( ! finished ) {
			System.out.println("not finished yet.");
			checkJobObject.kill(true);
		} else {
			System.out.println("Stdout: "+checkJobObject.getStdOutContent());
			System.out.println("Stderr: "+checkJobObject.getStdErrContent());
			checkJobObject.kill(true);
		}		
		
		
	}

}