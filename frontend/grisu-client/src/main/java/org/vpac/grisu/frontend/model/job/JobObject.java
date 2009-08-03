package org.vpac.grisu.frontend.model.job;

import java.io.File;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.vpac.grisu.control.JobConstants;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.FileTransferException;
import org.vpac.grisu.control.exceptions.JobPropertiesException;
import org.vpac.grisu.control.exceptions.JobSubmissionException;
import org.vpac.grisu.control.exceptions.NoSuchJobException;
import org.vpac.grisu.model.FileManager;
import org.vpac.grisu.model.GrisuRegistry;
import org.vpac.grisu.model.job.JobCreatedProperty;
import org.vpac.grisu.model.job.JobSubmissionObjectImpl;
import org.vpac.grisu.utils.FileHelpers;
import org.vpac.grisu.utils.SeveralXMLHelpers;
import org.w3c.dom.Document;

import au.org.arcs.jcommons.constants.Constants;
import au.org.arcs.jcommons.constants.JobSubmissionProperty;

/**
 * A model class that hides all the complexity of creating and submitting a job.
 * 
 * It extends the {@link JobSubmissionObjectImpl} class which is used to create
 * the job using the basic {@link JobSubmissionProperty}s. It adds methods to
 * create the job on the backend, submit it and also to monitor/control it.
 * 
 * @author Markus Binsteiner
 */
public class JobObject extends JobSubmissionObjectImpl {

	static final Logger myLogger = Logger.getLogger(JobObject.class.getName());

	private final ServiceInterface serviceInterface;

	private int status = JobConstants.UNDEFINED;

	private Map<String, String> allJobProperties;

	private String jobDirectory;

	private boolean isFinished = false;

	private Thread waitThread;

	/**
	 * Use this constructor if the job is already created on the backend. It'll
	 * fetch all the basic jobproperties from the backend and it'll also get the
	 * current status of the job.
	 * 
	 * @param si
	 *            the serviceInterface
	 * @param jobname
	 *            the name of the job
	 * @throws NoSuchJobException
	 *             if there is no job with the specified name on the backend
	 *             connected to the specified serviceInterface
	 */
	public JobObject(final ServiceInterface si, final String jobname)
			throws NoSuchJobException {

		super(SeveralXMLHelpers.fromString(si.getJsdlDocument(jobname)));
		this.serviceInterface = si;
		this.setJobname(jobname);

		getStatus(true);

	}

	/**
	 * Use this constructor if you want to create a new job.
	 * 
	 * @param si
	 *            the serviceInterface
	 */
	public JobObject(final ServiceInterface si) {
		super();
		this.serviceInterface = si;
	}

	/**
	 * This constructor creates a new JobObject and initializes it with the
	 * values you provide in the jobProperties map. Have a look at the
	 * {@link JobSubmissionProperty} enum to find out which properties are
	 * supported and what the names of the keys are for them.
	 * 
	 * @param si
	 *            the serviceInterface
	 * @param jobProperties
	 *            the basic properties of the job (no. of cpus, application to
	 *            use, ...)
	 */
	public JobObject(final ServiceInterface si, final Map<String, String> jobProperties) {
		super(jobProperties);
		this.serviceInterface = si;
	}

	/**
	 * This constructor creates a new JobObject and initializes the basic job
	 * parameters using the provided jsdl document. This could be used for
	 * example if you want to use an already submitted job by calling the
	 * {@link ServiceInterface#getJsdlDocument(String)} and using its return
	 * value in this constructor.
	 * 
	 * @param si
	 *            the serviceInterface
	 * @param jsdl
	 *            the jsdl document
	 */
	public JobObject(final ServiceInterface si, final Document jsdl) {
		super(jsdl);
		this.serviceInterface = si;
	}

	/**
	 * Creates the job on the grisu backend using the "force-name" method (which
	 * means the backend will not change the jobname you specified -- if there
	 * is a job with that jobname already the backend will throw an exception).
	 * 
	 * Be aware, that once that is done, you can't change any of the basic job
	 * parameters anymore. The backend calculates all the (possibly) missing job
	 * parameters and sets values like the final submissionlocation and such.
	 * After you created a job on the backend, you can query these calculated
	 * values using the {@link ServiceInterface#getJobProperty(String, String)}
	 * method.
	 * 
	 * @param fqan
	 *            the VO to use to submit this job
	 * @return the final jobname (equals the one you specified when creating the
	 *         JobObject object).
	 * @throws JobPropertiesException
	 *             if one of the properties is invalid and the job could not be
	 *             created on the backend
	 */
	public final String createJob(final String fqan) throws JobPropertiesException {

		return createJob(fqan, Constants.FORCE_NAME_METHOD);
	}

	/**
	 * Creates the job on the grisu backend using the jobname creating method
	 * you specified. Have a look at the static Strings in
	 * {@link ServiceInterface} for a list of supported jobname creation
	 * methods.
	 * 
	 * Other than the jobname creation, this does the same as
	 * {@link #createJob(String)}.
	 * 
	 * @param fqan
	 *            the VO to use to submit this job
	 * @param jobnameCreationMethod
	 *            the name of the jobname creation method the backend should use
	 * @return the final name of the job which can be used as a handle to get
	 *         jobproperties like the status or jobdirectory
	 * @throws JobPropertiesException
	 */
	public final String createJob(final String fqan, final String jobnameCreationMethod)
			throws JobPropertiesException {

		setJobname(serviceInterface.createJobUsingJsdl(getJobDescriptionDocumentAsString(),
				fqan, jobnameCreationMethod));

		try {
			jobDirectory = serviceInterface.getJobProperty(getJobname(),
					Constants.JOBDIRECTORY_KEY);
			getStatus(true);
		} catch (NoSuchJobException e) {
			fireJobStatusChange(this.status, JobConstants.NO_SUCH_JOB);
		}

		return this.getJobname();
	}

	/**
	 * After you created the job on the backend using the
	 * {@link #createJob(String)} or {@link #createJob(String, String)} method
	 * you can tell the backend to actually submit the job to the endpoint
	 * resource. Internally, this method also does possible stage-ins from your
	 * local machine.
	 * 
	 * @throws JobSubmissionException
	 *             if the job could not be submitted
	 */
	public final void submitJob() throws JobSubmissionException {

		if (status == JobConstants.UNDEFINED) {
			throw new IllegalStateException("Job state "
					+ JobConstants.translateStatus(JobConstants.UNDEFINED)
					+ ". Can't submit job.");
		}

		if (getInputFileUrls() != null && getInputFileUrls().length > 0) {
			setStatus(JobConstants.INPUT_FILES_UPLOADING);
		}

		// stage in local files
		for (String inputFile : getInputFileUrls()) {
			if (FileManager.isLocal(inputFile)) {
				try {
					GrisuRegistry.getDefault(serviceInterface).getFileManager()
							.uploadFile(inputFile, jobDirectory);
				} catch (FileTransferException e) {
					throw new JobSubmissionException(
							"Could not stage-in file: " + inputFile, e);
				}
			}
		}

		if (getInputFileUrls() != null && getInputFileUrls().length > 0) {
			setStatus(JobConstants.INPUT_FILES_UPLOADED);
		}

		serviceInterface.submitJob(getJobname());
		getStatus(true);
	}

	private void setStatus(final int newStatus) {

		int oldstatus = this.status;
		this.status = newStatus;

		if (oldstatus != this.status) {
			fireJobStatusChange(oldstatus, this.status);
		}

	}

	/**
	 * Returns the current status of the job.
	 * 
	 * Have a look at the {@link JobConstants} class for possible values.
	 * 
	 * @param forceRefresh
	 *            whether to use the cached status (false) or force a status
	 *            refresh (true)
	 * @return the job status
	 */
	public final int getStatus(final boolean forceRefresh) {
		if (forceRefresh) {
			int oldStatus = this.status;
			this.status = serviceInterface.getJobStatus(getJobname());
			if (this.status != oldStatus) {
				fireJobStatusChange(oldStatus, this.status);
			}
		}
		return this.status;
	}

	/**
	 * Same as {@link #getStatus(boolean)}. Just auto-translates the status to a
	 * meaningful string.
	 * 
	 * @param forceRefresh
	 *            whether to use the cached status (false) or force a status
	 *            refresh (true)
	 * @return the job status string
	 */
	public final String getStatusString(final boolean forceRefresh) {
		return JobConstants.translateStatus(getStatus(forceRefresh));
	}

	/**
	 * Tells the backend to kill this job. If you specify true for the clean
	 * parameter, the job gets deleted from the backend database and the
	 * jobdirectory on the endpoint resource gets deleted as well.
	 * 
	 * @param clean
	 *            whether to clean the job
	 * @throws JobException
	 *             if the job could not be killed/cleaned
	 */
	public final void kill(final boolean clean) {

		if (getStatus(false) == JobConstants.UNDEFINED) {
			throw new IllegalStateException("Job status "
					+ JobConstants.translateStatus(JobConstants.UNDEFINED)
					+ ". Can't kill job yet.");
		}

		try {
			this.serviceInterface.kill(this.getJobname(), clean);
		} catch (Exception e) {
			throw new JobException(this, "Could not kill/clean job.", e);
		}

	}

	/**
	 * Returns a map of all known job properties.
	 * 
	 * It only makes sense to call this method if the job was already created on
	 * the backend using the {@link #createJob(String)} or
	 * {@link #createJob(String, String)} method.
	 * 
	 * @return the job properties
	 */
	public final Map<String, String> getAllJobProperties() {

		if (getStatus(false) == JobConstants.UNDEFINED) {
			throw new IllegalStateException("Job status "
					+ JobConstants.translateStatus(JobConstants.UNDEFINED)
					+ ". Can't access job properties yet.");
		}

		if (allJobProperties == null) {
			try {
				allJobProperties = serviceInterface
						.getAllJobProperties(getJobname()).propertiesAsMap();
			} catch (Exception e) {
				throw new JobException(this, "Could not get jobproperties.", e);
			}
		}
		return allJobProperties;

	}

	/**
	 * Returns the absolute url to the job directory.
	 * 
	 * It only makes sense to call this method of the job was already created on
	 * the backend.
	 * 
	 * @return the url to the job (working-) directory
	 */
	public final String getJobDirectoryUrl() {

		if (this.getStatus(false) == JobConstants.UNDEFINED) {
			throw new IllegalStateException("Job status "
					+ JobConstants.translateStatus(JobConstants.UNDEFINED)
					+ ". Can't get jobdirectory yet.");
		}

		if (jobDirectory == null) {
			String url = getAllJobProperties().get(
					JobCreatedProperty.JOBDIRECTORY.toString());
			jobDirectory = url;
		}

		return jobDirectory;
	}
	
	public final File downloadAndCacheOutputFile(String relativePathToJobDirectory) {
		
		if (getStatus(false) <= JobConstants.ACTIVE) {
			if (getStatus(true) < JobConstants.ACTIVE) {
				throw new IllegalStateException(
						"Job not started yet. No stdout file exists.");
			}
		}
		//		
		String url;
		url = getJobDirectoryUrl()+ "/"	+ relativePathToJobDirectory;

		File file = null;
		try {
			file = GrisuRegistry.getDefault(serviceInterface)
					.getFileManager().downloadFile(url);
		} catch (Exception e) {
			throw new JobException(this, "Could not download file: "+url, e);
		}

		return file;
	}

	/**
	 * This method downloads a current version of the stdout file for this job
	 * into the local grisu cache and returns the pointer to a locally cached
	 * version of it.
	 * 
	 * It only makes sense to call this method of the job was already created on
	 * the backend.
	 * 
	 * @return the locally cached stdout file
	 */
	public final File getStdOutFile() {

		File stdoutFile;
		try {
			stdoutFile = downloadAndCacheOutputFile(serviceInterface.getJobProperty(getJobname(),
				JobSubmissionProperty.STDOUT.toString()));
		} catch (Exception e) {
			throw new JobException(this, "Could not download stdout file.", e);
		}

		return stdoutFile;
	}

	/**
	 * Tells you whether the job is finished (either sucessfully or not).
	 * 
	 * @return finished: true / still running/not started: false
	 */
	public final boolean isFinished() {

		if (isFinished) {
			return true;
		}

		if (getStatus(false) <= JobConstants.JOB_CREATED) {
			throw new IllegalStateException("Job not submitted yet.");
		}

		if (getStatus(true) >= JobConstants.FINISHED_EITHER_WAY) {
			isFinished = true;
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Returns the current content of the stdout file for this job as a string.
	 * 
	 * Internally the stdout file is downloaded to the local grisu cache and
	 * read.
	 * 
	 * @return the current content of the stdout file for this job
	 */
	public final String getStdOutContent() {

		String result;
		try {
			result = FileHelpers.readFromFileWithException(getStdOutFile());
		} catch (Exception e) {
			throw new JobException(this, "Could not read stdout file.", e);
		}

		return result;

	}

	/**
	 * Returns the current content of the stderr file for this job as a string.
	 * 
	 * Internally the stderr file is downloaded to the local grisu cache and
	 * read.
	 * 
	 * @return the current content of the stderr file for this job
	 */
	public final String getStdErrContent() {

		String result;
		try {
			result = FileHelpers.readFromFileWithException(getStdErrFile());
		} catch (Exception e) {
			throw new JobException(this, "Could not read stdout file.", e);
		}

		return result;

	}

	/**
	 * This method downloads a current version of the stderr file for this job
	 * into the local grisu cache and returns the pointer to a locally cached
	 * version of it.
	 * 
	 * It only makes sense to call this method of the job was already created on
	 * the backend.
	 * 
	 * @return the locally cached stderr file
	 */
	public final File getStdErrFile() {

		File stderrFile;
		try {
			stderrFile = downloadAndCacheOutputFile(serviceInterface.getJobProperty(getJobname(),
				JobSubmissionProperty.STDERR.toString()));
		} catch (Exception e) {
			throw new JobException(this, "Could not download stderr file.", e);
		}

		return stderrFile;
	}

	/**
	 * You can use this method to wait for the job to finish (either
	 * successfully or not) on the endpoint resource.
	 * 
	 * Use the int parameter to specify the sleep interval inbetween status
	 * checks. Don't use a low number here please (except for testing) because
	 * it could possibly cause a high load for the backend.
	 * 
	 * @param checkIntervallInSeconds
	 *            the interval inbetween status checks
	 * @return whether the job is actually finished (true) or the this
	 *         wait-thread was interrupted otherwise
	 */
	public final boolean waitForJobToFinish(final int checkIntervallInSeconds) {

		if (waitThread != null) {
			if (waitThread.isAlive()) {
				try {
					waitThread.join();
					return isFinished();
				} catch (InterruptedException e) {
					myLogger.debug("Job status wait thread interrupted.");
					return isFinished();
				}
			}
		}

		createWaitThread(checkIntervallInSeconds);

		try {
			waitThread.start();
			waitThread.join();
			waitThread = null;
		} catch (InterruptedException e) {
			myLogger.debug("Job status wait thread interrupted.");
			waitThread = null;
			return isFinished();
		}

		return isFinished();
	}

	/**
	 * Interrupts the {@link #waitForJobToFinish(int)} method.
	 */
	public final void stopWaitingForJobToFinish() {

		if (waitThread == null || !waitThread.isAlive()) {
			return;
		}

		waitThread.interrupt();
		myLogger.debug("Wait thread interrupted.");

	}

	private void createWaitThread(final int checkIntervallInSeconds) {

		try {
			// just to make sure we don't create 2 or more threads. Should never
			// happen.
			waitThread.interrupt();
		} catch (Exception e) {
			myLogger.debug(e);
		}

		waitThread = new Thread() {
			public void run() {
				while (!isFinished()) {

					if (isInterrupted()) {
						return;
					}
					System.out.println("Status: " + getStatusString(false));
					try {
						Thread.sleep(checkIntervallInSeconds * 1000);
					} catch (InterruptedException e) {
						myLogger.debug("Wait thread for job " + getJobname()
								+ " interrupted.");
						return;
					}
				}
			}
		};

	}

	// event stuff
	// ========================================================
	private Vector<JobStatusChangeListener> jobStatusChangeListeners;

	private void fireJobStatusChange(final int oldStatus, final int newStatus) {

		myLogger.debug("Fire job status change event.");
		// if we have no mountPointsListeners, do nothing...
		if (jobStatusChangeListeners != null
				&& !jobStatusChangeListeners.isEmpty()) {

			// make a copy of the listener list in case
			// anyone adds/removes mountPointsListeners
			Vector<JobStatusChangeListener> valueChangedTargets;
			synchronized (this) {
				valueChangedTargets = (Vector<JobStatusChangeListener>) jobStatusChangeListeners
						.clone();
			}

			// walk through the listener list and
			// call the gridproxychanged method in each
			Enumeration<JobStatusChangeListener> e = valueChangedTargets
					.elements();
			while (e.hasMoreElements()) {
				JobStatusChangeListener valueChanged_l = e.nextElement();
				valueChanged_l.jobStatusChanged(this, oldStatus, newStatus);
			}
		}
	}

	/**
	 * Adds a jobstatus change listener.
	 * 
	 * @param l
	 *            the listener
	 */
	public final synchronized void addJobStatusChangeListener(
			final JobStatusChangeListener l) {
		if (jobStatusChangeListeners == null) {
			jobStatusChangeListeners = new Vector<JobStatusChangeListener>();
		}
		jobStatusChangeListeners.addElement(l);
	}

	/**
	 * Removes a jobstatus change listener.
	 * 
	 * @param l
	 *            the listener
	 */
	public final synchronized void removeJobStatusChangeListener(
			final JobStatusChangeListener l) {
		if (jobStatusChangeListeners == null) {
			jobStatusChangeListeners = new Vector<JobStatusChangeListener>();
		}
		jobStatusChangeListeners.removeElement(l);
	}

}
