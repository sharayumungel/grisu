package org.vpac.grisu.client.gridTests;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.vpac.grisu.control.JobConstants;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.JobPropertiesException;
import org.vpac.grisu.control.exceptions.JobSubmissionException;
import org.vpac.grisu.control.exceptions.MdsInformationException;
import org.vpac.grisu.frontend.model.job.JobObject;
import org.vpac.grisu.frontend.model.job.JobStatusChangeListener;

abstract class GridTestElement implements JobStatusChangeListener, Comparable<GridTestElement> {

	protected final ServiceInterface serviceInterface;
	protected final String application;
	protected final String version;
	protected final String submissionLocation;

	protected final String id;

	private final List<GridTestStage> testStages = new LinkedList<GridTestStage>();

	protected final JobObject jobObject;

	private GridTestStage currentStage;
	
	private boolean failed = false;
	private boolean interrupted = false;
	
	private List<Exception> exceptions = new LinkedList<Exception>();
	
	private final static String END_STAGE = "endStage";
	
	protected final GridTestController controller;
	
	
	public GridTestController getController() {
		return this.controller;
	}
	
	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public boolean wasInterrupted() {
		return interrupted;
	}
	
	private final Date startDate;
	private Date endDate = null;

	protected GridTestElement(GridTestController controller, ServiceInterface si, String version,
			String submissionLocation) throws MdsInformationException {
		this.controller = controller;
		startDate = new Date();
		endDate = new Date();
		beginNewStage("Initializing test element...");
		this.serviceInterface = si;
		this.version = version;
		this.submissionLocation = submissionLocation;
		this.application = getApplicationSupported();
		addMessage("Creating JobObject...");
		this.jobObject = createJobObject();
		this.id = UUID.randomUUID().toString();
		this.jobObject.setJobname(this.application+"_"+this.version+"_"+this.id);
		this.jobObject.setSubmissionLocation(submissionLocation);
		addMessage("JobObject created.");
		this.jobObject.addJobStatusChangeListener(this);
		currentStage.setStatus(GridTestStageStatus.FINISHED_SUCCESS);
	}
	
	public void addJobStatusChangeListener(JobStatusChangeListener jscl) {
		this.jobObject.addJobStatusChangeListener(jscl);
	}
	
	public void removeJobStatusChangeListener(JobStatusChangeListener jscl) {
		this.jobObject.removeJobStatusChangeListener(jscl);
	}
	
	public static GridTestElement createGridTestElement(GridTestController controller, String application, ServiceInterface serviceInterface, String version, String subLoc) throws MdsInformationException {
		
		GridTestElement gte = null;
		if ( "java".equals(application.toLowerCase()) ) {
			gte = new JavaGridTestElement(controller, serviceInterface, version, subLoc);
		} else if ( "unixcommands".equals(application.toLowerCase()) ) {
			gte = new UnixCommandsGridTestElement(controller, serviceInterface, version, subLoc);
		} else if ( "underworld".equals(application.toLowerCase()) ){
			gte = new UnderworldGridTestElement(controller, serviceInterface, version, subLoc);
		} else if ( "generic".equals(application.toLowerCase()) ){
			gte = new GenericGridTestElement(controller, serviceInterface, version, subLoc);
		} else if ( "pbstest".equals(application.toLowerCase()) ){
			gte = new PbsGridTestElement(controller, serviceInterface, version, subLoc);
		} else {
			throw new RuntimeException("Application \""+application+"\" not supported yet.");
		}
		
		return gte;
	}
	
	public static boolean useMds(String application) {
		
		if ( "java".equals(application) ) {
			return true;
		} else if ( "unixcommands".equals(application) ) {
			return true;
		} else if ( "underworld".equals(application) ) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public String getTestId() {
		return this.id;
	}
	
	public List<Exception> getExceptions() {
		return exceptions;
	}
	
	public String getSubmissionLocation() {
		return submissionLocation;
	}

	public int getJobStatus(boolean forceRefresh) {
		return this.jobObject.getStatus(forceRefresh);
	}

	protected void addMessage(String message) {
		if ( currentStage != null ) {
			currentStage.addMessage(message);
		}
	}

	private boolean beginNewStage(String stageName) {

		boolean lastStageSuccess = true;
		if ( currentStage != null && !currentStage.wasSuccessful() ) {
			lastStageSuccess = false;
			if ( currentStage.getPossibleException() != null ) {
				exceptions.add(currentStage.getPossibleException());
			}
		}
		
		if ( END_STAGE.equals(stageName) ) {
			addMessage("Finished test.");
			currentStage = null;
			endDate = new Date();
			return lastStageSuccess;
		}
		
		currentStage = new GridTestStage(stageName);
		testStages.add(currentStage);
		currentStage.setStatus(GridTestStageStatus.RUNNING);
		
		return lastStageSuccess;
	}

	public void finishTest() {
		
		beginNewStage(END_STAGE);
		
		// housekeeping 
		try {
			jobObject.kill(true);
		} catch (Exception e) {
			// doesn't matter
		}
	}
	
	public List<GridTestStage> getTestStages() {
		return testStages;
	}

	public void createJob(String fqan) {

		beginNewStage("Creating job on backend...");

		try {
			jobObject.createJob(fqan);
			currentStage.setStatus(GridTestStageStatus.FINISHED_SUCCESS);
		} catch (JobPropertiesException e) {
			currentStage.setPossibleException(e);
			currentStage.setStatus(GridTestStageStatus.FINISHED_ERROR);
			failed = true;
		}

	}

	public void submitJob() {
		
		if ( ! failed ) {

		if ( beginNewStage("Submitting job to backend...") ) {

		try {
			jobObject.submitJob();
			currentStage.setStatus(GridTestStageStatus.FINISHED_SUCCESS);
		} catch (JobSubmissionException e) {
			e.printStackTrace();
			currentStage.setPossibleException(e);
			currentStage.setStatus(GridTestStageStatus.FINISHED_ERROR);
			failed = true;
		}
		} else {
			currentStage.setStatus(GridTestStageStatus.NOT_EXECUTED);
		}
		}
	}

//	public void waitForJobToFinish() {
//
//		if ( beginNewStage("Waiting for job to finish...") ) {
//
//		while (this.jobObject.getStatus(true) < JobConstants.FINISHED_EITHER_WAY) {
//			if (this.jobObject.getStatus(false) == JobConstants.NO_SUCH_JOB) {
//				addMessage("Could not find job anymore. Probably a problem with the container...");
//				currentStage.setStatus(GridTestStageStatus.FINISHED_ERROR);
//				failed = true;
//				return;
//			}
//
//			try {
//				addMessage("Waiting 2 seconds before new check. Current Status: "
//						+ JobConstants.translateStatus(this.jobObject
//								.getStatus(false)));
//				Thread.sleep(2000);
//			} catch (InterruptedException e) {
//				currentStage.setPossibleException(e);
//				currentStage.setStatus(GridTestStageStatus.FINISHED_ERROR);
//				failed = true;
//				return;
//			}
//		}
//
//		addMessage("Job finished one way or another.");
//		currentStage.setStatus(GridTestStageStatus.FINISHED_SUCCESS);
//		} else {
//			currentStage.setStatus(GridTestStageStatus.NOT_EXECUTED);
//		}
//
//	}
	
	protected void setPossibleExceptionForCurrentStage(Exception e) {
		if ( currentStage != null ) {
			currentStage.setPossibleException(e);
		}
	}

	public void checkWhetherJobDidWhatItWasSupposedToDo() {
		
		if ( ! failed ) {

		if ( beginNewStage("Checking job status and output...") ) {

			boolean success = true;
			try {
				success = checkJobSuccess();
			} catch (Exception e) {
				System.out.println("Error checking job "+toString()+": "+e.getLocalizedMessage());
				this.setPossibleExceptionForCurrentStage(e);
				success = false;
			}

		if (success) {
			currentStage.setStatus(GridTestStageStatus.FINISHED_SUCCESS);
		} else {
			currentStage.setStatus(GridTestStageStatus.FINISHED_ERROR);
			failed = true;
		}
		} else {
			currentStage.setStatus(GridTestStageStatus.NOT_EXECUTED);
		}
		
		}

	}

	public void killAndClean() {
		
		if ( ! failed ) {

		// execute that anyway
		beginNewStage("Killing and cleaning job...");

		try {
			jobObject.kill(true);
		} catch (Exception e) {
			currentStage.setPossibleException(e);
			currentStage.setStatus(GridTestStageStatus.FINISHED_ERROR);
			failed = true;
		} 

		currentStage.setStatus(GridTestStageStatus.FINISHED_SUCCESS);
		}
		
	}

	public void jobStatusChanged(JobObject job, int oldStatus, int newStatus) {

		addMessage("New job status: " + JobConstants.translateStatus(newStatus));

	}

	public void printTestResults() {
		for (GridTestStage stage : getTestStages()) {
			System.out.println("Stage: " + stage.getName());
			System.out.println("Started: " + stage.getBeginDate());
			stage.printMessages();
			System.out.println("Ended: " + stage.getEndDate());
			System.out.println("Status: " + stage.getStatus());
			if (stage.getStatus().equals(GridTestStageStatus.FINISHED_ERROR)) {
				System.out.println("Error: "
						+ stage.getPossibleException().getLocalizedMessage());
			}
			System.out.println();
		}
	}
	
	public boolean failed() {
		return failed;
	}
	
	public String getResultString() {
		StringBuffer result = new StringBuffer();
		for (GridTestStage stage : getTestStages()) {
			result.append("Stage: " + stage.getName()+"\n");
			result.append("Started: " + stage.getBeginDate()+"\n");
			result.append(stage.getMessagesString()+"\n");
			result.append("Ended: " + stage.getEndDate()+"\n");
			result.append("Status: " + stage.getStatus()+"\n");
			if (stage.getStatus().equals(GridTestStageStatus.FINISHED_ERROR) && stage.getPossibleException() != null ) {
				result.append("Error: "
						+ stage.getPossibleException().getLocalizedMessage()+"\n");
				if ( stage.getPossibleException().getCause() != null ) {
					result.append("Cause: "+Utils.fromException(stage.getPossibleException().getCause()));
				}
			}
			result.append("\n");
		}
		return result.toString();
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public void interruptRunningJob() {
		try {
			jobObject.kill(false);
		} catch (Exception e) {
			// doesn't matter
		}
		this.interrupted = true;
		this.failed = true;
	}

	@Override
	public String toString() {
		return "Application: "+getApplicationSupported()+",  version: "+version+", submissionlocation: "+submissionLocation;
	}
	
	public int compareTo(GridTestElement o) {
		int testname = this.getTestName().compareTo(o.getTestName());
		if ( testname != 0 ) {
			return testname;
		} 
		int application = this.getApplicationSupported().compareTo(o.getApplicationSupported());
		if ( application != 0 ) {
			return application;
		}
		int version = this.getVersion().compareTo(o.getVersion());
		if ( version != 0 ) {
			return version;
		}
		int subLoc = this.getSubmissionLocation().compareTo(o.getSubmissionLocation());
		return subLoc;
	}
	
	abstract protected JobObject createJobObject() throws MdsInformationException;

	abstract protected String getApplicationSupported();
	
	abstract protected boolean checkJobSuccess();
	
	abstract protected boolean useMDS();
	
	abstract public String getTestName();
	
	abstract public String getTestDescription();

}
