package grisu.backend.utils;

import grisu.backend.hibernate.JobDAO;
import grisu.backend.model.job.Job;
import grisu.control.JobConstants;
import grisu.control.exceptions.NoSuchJobException;

import java.util.List;

/**
 * The JobNameManager takes care that a user does not submit a job with the same
 * name twice. Before you create a Job, take the jobname you want to give your
 * job and pass it to the {@link #getJobname(String, String, int)} method. This
 * will suggest a jobname according to the auto-naming scheme you have chosen.
 * 
 * @author Markus Binsteiner
 * 
 */
public final class JobNameManager {

	public static final String ITERATOR_SEPERATOR = "_";

	private static JobDAO jobdao = new JobDAO();

	/**
	 * Checks whether the same jobname is already in the db and suggests a new
	 * one according to the auto-naming scheme you have chosen.
	 * 
	 * @param dn
	 *            the dn of the user
	 * @param jobname
	 *            the suggested name of the job
	 * @param createJobNameMethod
	 *            the auto-naming scheme (have a look at
	 *            {@link Job#ALWAYS_INCREMENT_JOB_NAME},
	 *            {@link Job#ALWAYS_TIMESTAMP_JOB_NAME},
	 *            {@link Job#ONLY_INCREMENT_JOB_NAME_IF_JOB_EXISTS_WITH_SAME_NAME}
	 *            and
	 *            {@link Job#ONLY_TIMESTAMP_JOB_NAME_IF_JOB_EXISTS_WITH_SAME_NAME}
	 *            .
	 * @return the new (suggested) jobname
	 */
	public static String getJobname(final String dn, String proposedJobname,
			final int createJobNameMethod) {

		proposedJobname = proposedJobname.replaceAll("\\s|;|'|\"|,|\\$|\\?|#",
				"_");

		switch (createJobNameMethod) {

		case JobConstants.ALWAYS_INCREMENT_JOB_NAME:
			List<Job> jobs = null;
			try {
				// gets all jobnames that start with "jobname"
				jobs = jobdao.getSimilarJobNamesByDN(dn, proposedJobname);
			} catch (final NoSuchJobException e) {
				// means we only have to return the jobname +0
				return proposedJobname + "_0";
			}
			return proposedJobname + "_"
			+ (highestJobnameNumber(jobs, proposedJobname) + 1);

		case JobConstants.DONT_ACCEPT_NEW_JOB_WITH_EXISTING_JOBNAME:

			if ((proposedJobname == null) || "".equals(proposedJobname)
					|| "null".equals(proposedJobname)) {
				throw new RuntimeException(
						"Could not create job: no valid jobname specified.");
			}

			Job job = null;

			if (createJobNameMethod == JobConstants.DONT_ACCEPT_NEW_JOB_WITH_EXISTING_JOBNAME) {
				try {
					job = jobdao.findJobByDN(dn, proposedJobname);
					throw new RuntimeException(
							"Could not create job: job with the same jobname already exists.");
				} catch (final NoSuchJobException e) {
					// that's actually good. No job with this jobname exists
					// jet/anymore.
					// TODO look whether there's a job directory in one of the
					// grisu-job directory with this name
					assert true;
				}
			}
			return proposedJobname;
		default:
			break;

		}

		// only JobConstants.ALWAYS_INCREMENT_JOB_NAME is implemented at the
		// moment
		return null;
	}

	/**
	 * Looks up the job with the "highest" jobname, something like jobname_01,
	 * jobname_02, jobname_09 would return 9. Applies to the auto-naming schemes
	 * {@link Job#ALWAYS_INCREMENT_JOB_NAME} and
	 * Job#ONLY_INCREMENT_JOB_NAME_IF_JOB_EXISTS_WITH_SAME_NAME}.
	 * 
	 * @param jobs
	 *            the list of jobs to be looked at
	 * @param jobname
	 *            the jobname in question
	 * @return the "highest" jobname
	 */
	private static String highestJobnameNumber(final List<Job> jobs,
			final String jobname) {

		int max = 0;

		for (final Object element : jobs) {
			int value = 0;
			try {
				final String tempName = ((Job) element).getJobname();
				value = new Integer(tempName.substring(jobname.length() + 1));
			} catch (final Exception e) {
				// e.printStackTrace();
				// this error is ok.
				assert true;
			}
			if (value > max) {
				max = value;
			}
		}
		return String.format("%03d", max);

	}

	private JobNameManager() {
	}

}
