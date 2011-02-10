package grisu.frontend.model.job;

import grisu.control.JobConstants;
import grisu.control.ServiceInterface;
import grisu.control.events.FileDeletedEvent;
import grisu.control.events.FolderCreatedEvent;
import grisu.control.exceptions.JobPropertiesException;
import grisu.control.exceptions.JobSubmissionException;
import grisu.control.exceptions.NoSuchJobException;
import grisu.control.exceptions.RemoteFileSystemException;
import grisu.frontend.control.clientexceptions.FileTransactionException;
import grisu.frontend.control.fileTransfers.FileTransaction;
import grisu.frontend.control.fileTransfers.FileTransactionManager;
import grisu.frontend.model.events.JobCleanedEvent;
import grisu.frontend.model.events.JobStatusEvent;
import grisu.frontend.model.events.NewJobEvent;
import grisu.jcommons.constants.Constants;
import grisu.jcommons.constants.JobSubmissionProperty;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;
import grisu.model.UserEnvironmentManager;
import grisu.model.dto.DtoJob;
import grisu.model.dto.GridFile;
import grisu.model.job.JobCreatedProperty;
import grisu.model.job.JobSubmissionObjectImpl;
import grisu.model.status.StatusObject;
import grisu.utils.FileHelpers;
import grisu.utils.SeveralXMLHelpers;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;
import org.w3c.dom.Document;


/**
 * A model class that hides all the complexity of creating and submitting a job.
 * 
 * It extends the {@link JobSubmissionObjectImpl} class which is used to create
 * the job using the basic {@link JobSubmissionProperty}s. It adds methods to
 * create the job on the backend, submit it and also to monitor/control it.
 * 
 * @author Markus Binsteiner
 */
public class JobObject extends JobSubmissionObjectImpl implements
Comparable<JobObject> {

	static final Logger myLogger = Logger.getLogger(JobObject.class.getName());

	public static JobObject createJobObject(ServiceInterface si,
			JobSubmissionObjectImpl jobsubmissionObject)
	throws JobPropertiesException {

		final JobObject job = new JobObject(si,
				jobsubmissionObject.getJobDescriptionDocument());

		job.setInputFiles(jobsubmissionObject.getInputFiles());

		return job;
	}

	private final ServiceInterface serviceInterface;

	private int status = JobConstants.UNDEFINED;

	private Map<String, String> allJobProperties;

	private String jobDirectory;

	private boolean isFinished = false;

	private Thread waitThread;

	private Map<Date, String> logMessages;

	private boolean isBeingCleaned = false;

	private boolean isArchived = false;

	private final List<String> submissionLog = Collections
	.synchronizedList(new LinkedList<String>());

	/**
	 * Use this constructor if you want to create a new job.
	 * 
	 * @param si
	 *            the serviceInterface
	 */
	public JobObject(final ServiceInterface si) {
		super();
		this.serviceInterface = si;
		addJobLogMessage("Empty job created.");
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
		addJobLogMessage("Job created from jsdl description.");
	}

	/**
	 * This can be also used to create a JobObject when the job is already
	 * created on the backend.
	 * 
	 * This one doesn't update the job status on the backend.
	 * 
	 * @param si
	 *            the serviceinterface
	 * @param job
	 *            the job
	 * @throws NoSuchJobException
	 *             if no such job exists on the backend
	 */
	public JobObject(final ServiceInterface si, final DtoJob job)
	throws NoSuchJobException {

		this(si, job, false);
	}

	public JobObject(final ServiceInterface si, final DtoJob job,
			final boolean refreshJobStatusOnBackend) throws NoSuchJobException {
		// need to use either jobname or jobdirectory to get remote job,
		// depending on whether
		// job is archived or not
		super((job.isArchived()) ? SeveralXMLHelpers.fromString(si
				.getJsdlDocument(job.jobname())) : job.propertiesAsMap());

		this.setJobname(jobname);

		this.serviceInterface = si;

		this.isArchived = job.isArchived();

		updateWithDtoJob(job);
		addJobLogMessage("Job retrieved from backend.");

		if (refreshJobStatusOnBackend) {
			getStatus(true);
		}

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
	public JobObject(final ServiceInterface si,
			final Map<String, String> jobProperties) {
		super(jobProperties);
		this.serviceInterface = si;
		addJobLogMessage("Job created from job properties.");
	}

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

		this(si, jobname, true);

	}

	/**
	 * Use this constructor if the job is already created on the backend. It'll
	 * fetch all the basic jobproperties from the backend and it'll also get the
	 * current status of the job.
	 * 
	 * @param si
	 *            the serviceInterface
	 * @param jobname
	 *            the name of the job
	 * @param refreshJobStatusOnBackend
	 *            whether to refresh the status of the job on the backend
	 * @throws NoSuchJobException
	 *             if there is no job with the specified name on the backend
	 *             connected to the specified serviceInterface
	 */
	public JobObject(final ServiceInterface si, final String jobname,
			boolean refreshJobStatusOnBackend) throws NoSuchJobException {

		super(SeveralXMLHelpers.fromString(si.getJsdlDocument(jobname)));
		this.setJobname(jobname);
		this.serviceInterface = si;

		updateWithDtoJob(serviceInterface.getJob(jobname));

		addJobLogMessage("Job retrieved from backend.");

		if (refreshJobStatusOnBackend) {
			getStatus(true);
		}

	}

	private synchronized void addJobLogMessage(String message) {
		this.submissionLog.add(message);
		pcs.firePropertyChange("submissionLog", null, getSubmissionLog());
	}

	/**
	 * Archives the job in the background, using the default archive location.
	 * 
	 * @return the url of the target archived jobdirectory
	 * @throws JobPropertiesException
	 *             if the job can't be archived
	 * @throws RemoteFileSystemException
	 *             if the job can't be archived because the target url can't be
	 *             accessed
	 */
	public final String archive() throws JobPropertiesException,
	RemoteFileSystemException {
		return archive(null, false);
	}

	/**
	 * Archives the job in the background, using a specified target location
	 * 
	 * @param target
	 *            the target url to archive the job or null to use the default
	 *            target
	 * @return the url of the target archived jobdirectory
	 * @throws JobPropertiesException
	 *             if the job can't be archived
	 * @throws RemoteFileSystemException
	 *             if the job can't be archived because the target url can't be
	 *             accessed
	 */
	public final String archive(String target) throws JobPropertiesException,
	RemoteFileSystemException {
		return archive(target, false);
	}

	/**
	 * Archives the job.
	 * 
	 * @param target
	 *            the url (of the parent dir) to archive the job to or null to
	 *            use the (previously set) default archive location
	 * @param waitForArchivingToFinish
	 *            whether to wait until archiving is finished
	 * @throws JobPropertiesException
	 *             if the job is not finished yet
	 * @throws RemoteFileSystemException
	 *             if the archive location is not specified or there is some
	 *             other kind of file related exception
	 */
	public final String archive(String target, boolean waitForArchivingToFinish)
	throws JobPropertiesException, RemoteFileSystemException {
		try {
			final String targetUrl = getServiceInterface().archiveJob(
					getJobname(), target);

			if (waitForArchivingToFinish) {
				try {
					StatusObject.waitForActionToFinish(getServiceInterface(),
							ServiceInterface.ARCHIVE_STATUS_PREFIX
							+ getJobname(), 5, true, false);

					isArchived = true;
					pcs.firePropertyChange("archived", false, true);

					final String oldUrl = jobDirectory;
					jobDirectory = targetUrl;
					pcs.firePropertyChange("jobDirectory", oldUrl, jobDirectory);

					EventBus.publish(new JobCleanedEvent(this));
					EventBus.publish(new FileDeletedEvent(oldUrl));
					EventBus.publish(new FolderCreatedEvent(jobDirectory));
				} catch (Exception e) {
					throw new JobPropertiesException("Can't archive job: "
							+ e.getLocalizedMessage());
				}

			} else {
				new Thread() {

					@Override
					public void run() {

						try {
							StatusObject.waitForActionToFinish(
									getServiceInterface(),
									ServiceInterface.ARCHIVE_STATUS_PREFIX
									+ getJobname(), 5, true, false);

							isArchived = true;
							pcs.firePropertyChange("archived", false, true);

							final String oldUrl = jobDirectory;
							jobDirectory = targetUrl;
							pcs.firePropertyChange("jobDirectory", oldUrl,
									jobDirectory);

							EventBus.publish(new JobCleanedEvent(JobObject.this));
							EventBus.publish(new FileDeletedEvent(oldUrl));
							EventBus.publish(new FolderCreatedEvent(
									jobDirectory));

						} catch (Exception e) {
							myLogger.error("Job archiving error.", e);
						}

					}
				}.start();

			}

			return targetUrl;
		} catch (NoSuchJobException e) {
			// should never happen
			myLogger.error(e);
			throw new JobPropertiesException(e.getLocalizedMessage());
		}
	}

	public int compareTo(JobObject o2) {
		return getJobname().compareTo(o2.getJobname());
	}

	/**
	 * Creates the job on the grisu backend using the "force-name" method (which
	 * means the backend will not change the jobname you specified -- if there
	 * is a job with that jobname already the backend will throw an exception).
	 * 
	 * Also, this method uses the current fqan that the
	 * {@link UserEnvironmentManager} that is used for this client holds.
	 * 
	 * Be aware, that once that is done, you can't change any of the basic job
	 * parameters anymore. The backend calculates all the (possibly) missing job
	 * parameters and sets values like the final submissionlocation and such.
	 * After you created a job on the backend, you can query these calculated
	 * values using the {@link ServiceInterface#getJobProperty(String, String)}
	 * method.
	 * 
	 * @return the final jobname (equals the one you specified when creating the
	 *         JobObject object).
	 * @throws JobPropertiesException
	 *             if one of the properties is invalid and the job could not be
	 *             created on the backend
	 */
	public final String createJob() throws JobPropertiesException {

		final String fqan = GrisuRegistryManager.getDefault(serviceInterface)
		.getUserEnvironmentManager().getCurrentFqan();

		return createJob(fqan);

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
	public final String createJob(final String fqan)
	throws JobPropertiesException {

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
	public final String createJob(final String fqan,
			final String jobnameCreationMethod) throws JobPropertiesException {

		EventBus.publish(new JobStatusEvent(this, this.status,
				JobConstants.UNDEFINED));
		addJobLogMessage("Creating job on backend...");
		if (StringUtils.isNotBlank(getJobname())) {
			EventBus.publish(this.getJobname(), new JobStatusEvent(this,
					this.status, JobConstants.UNDEFINED));
		}

		try {
			setJobname(serviceInterface.createJob(
					getJobDescriptionDocumentAsString(), fqan,
					jobnameCreationMethod));
			EventBus.publish(new JobStatusEvent(this, this.status,
					JobConstants.JOB_CREATED));
			if (StringUtils.isNotBlank(getJobname())) {
				addJobLogMessage("Job created. Jobname: " + getJobname());
				EventBus.publish(this.getJobname(), new JobStatusEvent(this,
						this.status, JobConstants.JOB_CREATED));
			}
			// populate new job properties in background
			new Thread() {
				@Override
				public void run() {
					getAllJobProperties(true);
					addJobLogMessage("Submission site is: "
							+ getJobProperty(Constants.SUBMISSION_SITE_KEY,
									false));
					addJobLogMessage("Submission queue is: "
							+ getJobProperty(Constants.QUEUE_KEY, false));
					addJobLogMessage("Job directory url is: "
							+ getJobProperty(Constants.JOBDIRECTORY_KEY, false));
				}
			}.start();

		} catch (final JobPropertiesException e) {
			addJobLogMessage("Could not create job on backend: "
					+ e.getLocalizedMessage());
			EventBus.publish(new JobStatusEvent(this, this.status,
					JobConstants.NO_SUCH_JOB));
			if (StringUtils.isNotBlank(getJobname())) {
				EventBus.publish(this.getJobname(), new JobStatusEvent(this,
						this.status, JobConstants.NO_SUCH_JOB));
			}
			throw e;
		}

		// updateJobDirectory();

		return this.getJobname();
	}

	private void createWaitThread(final int checkIntervallInSeconds) {

		try {
			// just to make sure we don't create 2 or more threads. Should never
			// happen.
			waitThread.interrupt();
		} catch (final Exception e) {
			myLogger.debug(e);
		}

		waitThread = new Thread() {
			@Override
			public void run() {

				final int oldStatus = getStatus(false);
				while (!isFinished()) {

					if (isInterrupted()) {
						return;
					}
					if (oldStatus != getStatus(false)) {
						EventBus.publish(new JobStatusEvent(JobObject.this,
								oldStatus, JobObject.this.getStatus(false)));
						if (StringUtils.isNotBlank(getJobname())) {
							EventBus.publish(
									JobObject.this.getJobname(),
									new JobStatusEvent(JobObject.this,
											oldStatus, JobObject.this
											.getStatus(false)));
						}
					}
					try {
						Thread.sleep(checkIntervallInSeconds * 1000);
					} catch (final InterruptedException e) {
						myLogger.debug("Wait thread for job " + getJobname()
								+ " interrupted.");
						return;
					}
				}
			}
		};

	}

	/**
	 * Downloads the specified file (relative path to the jobdirectory) and puts
	 * it into the local cache.
	 * 
	 * @param relativePathToJobDirectory
	 *            the path to the file to download
	 * @return the file handle to the file in the local cache dir
	 */
	public final File downloadAndCacheOutputFile(
			String relativePathToJobDirectory) {

		addJobLogMessage("Downloading and caching output file...");

		if (getStatus(false) <= JobConstants.ACTIVE) {
			if (getStatus(true) < JobConstants.ACTIVE) {
				addJobLogMessage("Can't download output: job not started yet.");
				throw new IllegalStateException(
				"Job not started yet. No stdout file exists.");
			}
		}
		//
		String url;
		url = getJobDirectoryUrl() + "/" + relativePathToJobDirectory;

		File file = null;
		try {
			file = GrisuRegistryManager.getDefault(serviceInterface)
			.getFileManager().downloadFile(url);
			addJobLogMessage("Downloaded output file: " + url);
		} catch (final Exception e) {
			addJobLogMessage("Could not download file " + url + ": "
					+ e.getLocalizedMessage());
			throw new JobException(this, "Could not download file: " + url, e);
		}

		return file;
	}

	@Override
	public boolean equals(Object other) {

		if (other instanceof JobObject) {
			final JobObject otherJob = (JobObject) other;
			return getJobname().equals(otherJob.getJobname());
		} else {
			return false;
		}

	}

	/**
	 * Returns a map of all known job properties.
	 * 
	 * It only makes sense to call this method if the job was already created on
	 * the backend using the {@link #createJob(String)} or
	 * {@link #createJob(String, String)} method.
	 * 
	 * This method doesn't refresh job properties forcefully.
	 * 
	 * @return the job properties
	 */
	public synchronized final Map<String, String> getAllJobProperties() {

		return getAllJobProperties(false);
	}

	/**
	 * Returns a map of all known job properties.
	 * 
	 * It only makes sense to call this method if the job was already created on
	 * the backend using the {@link #createJob(String)} or
	 * {@link #createJob(String, String)} method.
	 * 
	 * @param forceRefresh
	 *            whether to forcefully refresh the job properties
	 * @return the job properties
	 */
	public synchronized final Map<String, String> getAllJobProperties(
			boolean forceRefresh) {

		// if (getStatus(false) == JobConstants.UNDEFINED) {
		// throw new IllegalStateException("Job status "
		// + JobConstants.translateStatus(JobConstants.UNDEFINED)
		// + ". Can't access job properties yet.");
		// }

		if ((allJobProperties == null) || forceRefresh) {
			try {
				allJobProperties = serviceInterface.getJob(getJobname())
				.propertiesAsMap();
			} catch (final Exception e) {
				throw new JobException(this, "Could not get jobproperties.", e);
			}
		}
		return allJobProperties;

	}

	/**
	 * Returns the current content of the file for this job as a string.
	 * 
	 * Internally the file is downloaded to the local grisu cache and read.
	 * 
	 * @return the current content of the stdout file for this job
	 */
	public final String getFileContent(String relativePathToWorkingDir) {

		String result;
		try {
			result = FileHelpers
			.readFromFileWithException(downloadAndCacheOutputFile(relativePathToWorkingDir));
		} catch (final Exception e) {
			throw new JobException(this, "Could not read stdout file.", e);
		}

		return result;

	}

	/**
	 * Returns the filesize of the specified file
	 * 
	 * @param relatevePathToJobDir
	 *            the path to the file relative to the job directory.
	 * @return the filesize in bytes
	 */
	public long getFileSize(String relatevePathToJobDir) {

		final String url = getJobDirectoryUrl() + "/" + relatevePathToJobDir;

		try {
			return GrisuRegistryManager.getDefault(serviceInterface)
			.getFileManager().getFileSize(url);
		} catch (final RemoteFileSystemException e) {
			return -1;
		}

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
			final String url = getAllJobProperties(false).get(
					JobCreatedProperty.JOBDIRECTORY.toString());
			jobDirectory = url;
		}

		return jobDirectory;
	}

	/**
	 * Returns the specified job property without a refresh.
	 * 
	 * @param key
	 *            the key
	 * 
	 * @return the property
	 */
	public final String getJobProperty(String key) {

		return getAllJobProperties(false).get(key);
	}

	/**
	 * Returns the specified job property.
	 * 
	 * @param key
	 *            the key
	 * @param forceRefresh
	 *            whether to refresh the job property forcefully or not
	 * 
	 * @return the property
	 */
	public final String getJobProperty(String key, boolean forceRefresh) {

		return getAllJobProperties(forceRefresh).get(key);
	}

	/**
	 * Returns the job log without a refresh.
	 * 
	 * @return the job log
	 */
	public synchronized Map<Date, String> getLogMessages() {

		return getLogMessages(false);
	}

	/**
	 * Returns the job log
	 * 
	 * @param forceRefresh
	 *            whether to forcefully refresh the log messages
	 * @return the job log
	 */
	public synchronized Map<Date, String> getLogMessages(boolean forceRefresh) {

		if ((logMessages == null) || forceRefresh) {
			try {
				updateWithDtoJob(serviceInterface.getJob(jobname));
			} catch (final NoSuchJobException e) {
				e.printStackTrace();
			}
		}
		return logMessages;
	}

	public ServiceInterface getServiceInterface() {
		return this.serviceInterface;
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
		if (forceRefresh && !isArchived) {
			final int oldStatus = this.status;
			// addJobLogMessage("Getting new job status. Old status: "
			// + JobConstants.translateStatus(oldStatus));
			final boolean oldFinished = isFinished(false);
			this.status = serviceInterface.getJobStatus(getJobname());

			pcs.firePropertyChange("status", oldStatus, this.status);
			pcs.firePropertyChange("statusString",
					JobConstants.translateStatus(oldStatus),
					getStatusString(false));
			pcs.firePropertyChange("finished", oldFinished, isFinished(false));
			// addJobLogMessage("Status refreshed. Status is: "
			// + JobConstants.translateStatus(this.status));
			if (this.status != oldStatus) {
				EventBus.publish(new JobStatusEvent(this, oldStatus,
						this.status));
				if (StringUtils.isNotBlank(getJobname())) {
					EventBus.publish(this.getJobname(), new JobStatusEvent(
							this, oldStatus, this.status));
				}
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
		} catch (final Exception e) {
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
			stderrFile = downloadAndCacheOutputFile(serviceInterface
					.getJobProperty(getJobname(),
							JobSubmissionProperty.STDERR.toString()));
		} catch (final Exception e) {
			throw new JobException(this, "Could not download stderr file.", e);
		}

		return stderrFile;
	}

	/**
	 * Returns the size of the stderr file.
	 * 
	 * @return the size in bytes
	 */
	public long getStdErrFileSize() {

		return getFileSize(getStderr());
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
		} catch (final Exception e) {
			throw new JobException(this, "Could not read stdout file.", e);
		}

		return result;

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
			stdoutFile = downloadAndCacheOutputFile(serviceInterface
					.getJobProperty(getJobname(),
							JobSubmissionProperty.STDOUT.toString()));
		} catch (final Exception e) {
			throw new JobException(this, "Could not download stdout file.", e);
		}

		return stdoutFile;
	}

	/**
	 * Returns the size of the stdout file.
	 * 
	 * @return the size in bytes
	 */
	public long getStdOutFileSize() {

		return getFileSize(getStdout());
	}

	public List<String> getSubmissionLog() {
		return submissionLog;
	}

	@Override
	public int hashCode() {
		return 73 * getJobname().hashCode();
	}

	public boolean isArchived() {
		return isArchived;
	}

	public boolean isBeingCleaned() {
		return isBeingCleaned;
	}

	/**
	 * Returns whether a job finished successful (Exit code: 0) or not.
	 * 
	 * @param forceRefresh
	 *            whether to refresh the status of the job on the backend or not
	 * 
	 * @return true if the job is finished but has got another exit code than 0,
	 *         false otherwise
	 */
	public final boolean isFailed(final boolean forceRefresh) {

		int status = getStatus(forceRefresh);

		if ( status < JobConstants.FINISHED_EITHER_WAY ) {
			return false;
		}

		if ( status == JobConstants.DONE ) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Tells you whether the job is finished (either sucessfully or not).
	 * 
	 * @return finished: true / still running/not started: false
	 */
	public final boolean isFinished() {
		return isFinished(true);
	}

	/**
	 * Tells you whether the job is finished (either sucessfully or not).
	 * 
	 * @param refresh
	 *            whether to refresh the job on the backend or not
	 * 
	 * @return finished: true / still running/not started: false
	 */
	public final boolean isFinished(boolean refresh) {

		if (getStatus(false) >= JobConstants.FINISHED_EITHER_WAY) {
			return true;
		}

		if (!refresh) {
			return false;
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
	 * Returns whether a job finished successful (Exit code: 0) or not.
	 * 
	 * @param forceRefresh
	 *            whether to refresh the status of the job on the backend or not
	 * 
	 * @return true if the job is finished and has got an exit code of 0, false
	 *         otherwise
	 */
	public final boolean isSuccessful(final boolean forceRefresh) {

		int status = getStatus(forceRefresh);

		if ( status < JobConstants.FINISHED_EITHER_WAY ) {
			return false;
		}

		if ( status == JobConstants.DONE ) {
			return true;
		} else {
			return false;
		}
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
			if (clean) {
				isBeingCleaned = true;
				pcs.firePropertyChange("beingCleaned", false, true);

				// delete local cache for this job
				new Thread() {
					@Override
					public void run() {
						myLogger.debug("Deleting local cached dir for job "
								+ getJobname() + ": " + getJobDirectoryUrl());
						File dir = GrisuRegistryManager
						.getDefault(serviceInterface).getFileManager()
						.getLocalCacheFile(getJobDirectoryUrl());
						FileUtils.deleteQuietly(dir);
					}
				}.start();

			}
			this.serviceInterface.kill(this.getJobname(), clean);
			getStatus(true);

			if (clean) {
				EventBus.publish(new JobCleanedEvent(this));
				EventBus.publish(new FileDeletedEvent(getJobDirectoryUrl()));
			}

		} catch (final Exception e) {
			throw new JobException(this, "Could not kill/clean job.", e);
		}

	}

	/**
	 * Lists all the files that are living under this jobs jobdirectory.
	 * 
	 * Specify a recursion level of 1 if you only are interested in the
	 * jobdirectory itself. Or the appropriate level if you want to look deeper.
	 * Use a value <= -1 to find all files on all levels.
	 * 
	 * @param recursionLevel
	 *            the recursion level
	 * @return the list of files
	 * @throws RemoteFileSystemException
	 */
	public List<String> listJobDirectory(int recursionLevel)
	throws RemoteFileSystemException {

		final GridFile folder = serviceInterface.ls(getJobDirectoryUrl(),
				recursionLevel);

		return folder.listOfAllFilesUnderThisFolder();

	}

	/**
	 * Restarts the job.
	 * 
	 * Don't use that at the moment, it's not properly implemented yet.
	 * 
	 * @throws JobSubmissionException
	 * @throws JobPropertiesException
	 */
	public final void restartJob() throws JobSubmissionException,
	JobPropertiesException {

		addJobLogMessage("Restarting job...");

		try {
			serviceInterface.restartJob(getJobname(),
					getJobDescriptionDocumentAsString());
			addJobLogMessage("Job restarted.");
		} catch (final NoSuchJobException e) {
			addJobLogMessage("Job restart failed: " + e.getLocalizedMessage());
			throw new JobSubmissionException("Could not find job on backend.",
					e);
		}
		getStatus(true);

	}

	private void setStatus(final int newStatus) {

		final int oldstatus = this.status;
		this.status = newStatus;

		if (oldstatus != this.status) {
			EventBus.publish(new JobStatusEvent(this, oldstatus, this.status));
			if (StringUtils.isNotBlank(getJobname())) {
				EventBus.publish(this.getJobname(), new JobStatusEvent(this,
						oldstatus, this.status));
			}
		}

	}

	/**
	 * Stages in input files.
	 * 
	 * Normally you don't have to call this method manually, it gets called just
	 * before job submission automatically.
	 * 
	 * @throws FileTransactionException
	 *             if a file can't be staged in
	 * @throws InterruptedException
	 */
	public final void stageFiles() throws FileTransactionException,
	InterruptedException {

		addJobLogMessage("Staging in files...");

		if ((getInputFiles() != null) && (getInputFiles().size() > 0)) {
			setStatus(JobConstants.INPUT_FILES_UPLOADING);
		} else {
			return;
		}

		final Set<String> localFiles = new HashSet<String>();
		for (final String inputFile : getInputFiles().keySet()) {
			if (FileManager.isLocal(inputFile)) {
				localFiles.add(inputFile);
			}
		}

		Map<String, Set<String>> targets = new HashMap<String, Set<String>>();
		for (String f : localFiles) {
			String path = getInputFiles().get(f);
			if (targets.get(path) == null) {
				targets.put(path, new HashSet<String>());
			}
			targets.get(path).add(f);
		}

		final FileTransactionManager ftm = FileTransactionManager
		.getDefault(serviceInterface);

		for (String target : targets.keySet()) {

			final FileTransaction fileTransfer = ftm.addJobInputFileTransfer(
					targets.get(target), this, target);
			try {
				fileTransfer.join();
			} catch (final ExecutionException e) {
				addJobLogMessage("Staging failed: " + e.getLocalizedMessage());
				if (fileTransfer.getException() != null) {
					throw fileTransfer.getException();
				} else {
					throw new FileTransactionException(
							fileTransfer.getFailedSourceFile(), jobDirectory,
							"File staging failed.", null);
				}
			}
			if (!FileTransaction.Status.FINISHED.equals(fileTransfer
					.getStatus())) {
				if (fileTransfer.getException() != null) {
					throw fileTransfer.getException();
				} else {
					throw new FileTransactionException(
							fileTransfer.getFailedSourceFile(), jobDirectory,
							"File staging failed.", null);
				}
			}

		}

		addJobLogMessage("Staging of input files finished.");

		if ((getInputFiles() != null) && (getInputFiles().size() > 0)) {
			setStatus(JobConstants.INPUT_FILES_UPLOADED);
		}

	}

	/**
	 * Interrupts the {@link #waitForJobToFinish(int)} method.
	 */
	public final void stopWaitingForJobToFinish() {

		if ((waitThread == null) || !waitThread.isAlive()) {
			return;
		}

		waitThread.interrupt();
		myLogger.debug("Wait thread interrupted.");

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
	 * @throws InterruptedException
	 */
	public final void submitJob() throws JobSubmissionException,
	InterruptedException {
		submitJob(null);
	}

	/**
	 * After you created the job on the backend using the
	 * {@link #createJob(String)} or {@link #createJob(String, String)} method
	 * you can tell the backend to actually submit the job to the endpoint
	 * resource. Internally, this method also does possible stage-ins from your
	 * local machine.
	 * 
	 * @param additionalJobProperties
	 *            properties you want to store with the job (only get stored if
	 *            submission was successful)
	 * 
	 * @throws JobSubmissionException
	 *             if the job could not be submitted
	 * @throws InterruptedException
	 */
	public final void submitJob(Map<String, String> additionalJobProperties)
	throws JobSubmissionException, InterruptedException {

		addJobLogMessage("Starting job submission...");

		if (getStatus(true) == JobConstants.UNDEFINED) {
			throw new IllegalStateException("Job state "
					+ JobConstants.translateStatus(JobConstants.UNDEFINED)
					+ ". Can't submit job.");
		}

		try {
			stageFiles();
		} catch (final FileTransactionException e) {
			addJobLogMessage("Could not stage in file: "
					+ e.getLocalizedMessage());
			throw new JobSubmissionException("Could not stage in file.", e);
		}

		if (Thread.interrupted()) {
			addJobLogMessage("Job submission interrupted.");
			throw new InterruptedException(
			"Interrupted after staging in input files.");
		}

		try {
			addJobLogMessage("Submitting job to endpoint...");
			serviceInterface.submitJob(getJobname());

		} catch (final NoSuchJobException e) {
			addJobLogMessage("Submission failed: " + e.getLocalizedMessage());
			throw new JobSubmissionException("Could not find job on backend.",
					e);
		}

		if ((additionalJobProperties != null)
				&& (additionalJobProperties.size() > 0)) {
			addJobLogMessage("Setting additional job properties...");
			try {
				serviceInterface.addJobProperties(getJobname(), DtoJob
						.createJob(-1, additionalJobProperties, null, null,
								false));
			} catch (final NoSuchJobException e) {
				addJobLogMessage("Submission failed: "
						+ e.getLocalizedMessage());
				throw new JobSubmissionException(
						"Could not find job on backend.", e);
			}
		}
		allJobProperties = null;
		getStatus(true);

		EventBus.publish(new NewJobEvent(this));

		addJobLogMessage("Job submission finished successfully.");
	}

	/**
	 * Synchronizes the internally stored value for the location of the
	 * jobdirectory with the backend.
	 */
	public void updateJobDirectory() {

		try {
			final String oldUrl = jobDirectory;
			jobDirectory = serviceInterface.getJobProperty(getJobname(),
					Constants.JOBDIRECTORY_KEY);
			getStatus(true);
			pcs.firePropertyChange("jobDirectory", oldUrl, jobDirectory);
		} catch (final NoSuchJobException e) {
			EventBus.publish(new JobStatusEvent(this, this.status,
					JobConstants.NO_SUCH_JOB));
			if (StringUtils.isNotBlank(getJobname())) {
				EventBus.publish(this.getJobname(), new JobStatusEvent(this,
						this.status, JobConstants.NO_SUCH_JOB));
			}
		}

	}

	public void updateWithDtoJob(DtoJob job) {

		// if (!isArchived && !job.jobname().equals(getJobname())) {
		// throw new IllegalArgumentException(
		// "Jobname differs. Can't update job");
		// }
		allJobProperties = job.propertiesAsMap();
		status = job.getStatus();
		logMessages = job.getLogMessages().asMap();

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

		addJobLogMessage("Waiting for job to finish...");

		if (waitThread != null) {
			if (waitThread.isAlive()) {
				try {
					waitThread.join();
					return isFinished();
				} catch (final InterruptedException e) {
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
		} catch (final InterruptedException e) {
			myLogger.debug("Job status wait thread interrupted.");
			waitThread = null;
			return isFinished();
		}

		return isFinished();
	}

}