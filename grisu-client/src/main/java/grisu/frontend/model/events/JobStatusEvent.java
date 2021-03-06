package grisu.frontend.model.events;

import grisu.control.JobConstants;
import grisu.frontend.model.job.GrisuJob;

public class JobStatusEvent {

	private final int oldStatus;
	private final int newStatus;

	private final GrisuJob job;

	public JobStatusEvent(GrisuJob job, int oldStatus, int newStatus) {
		this.job = job;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
	}

	public GrisuJob getJob() {
		return this.job;
	}

	public int getNewStatus() {
		return this.newStatus;
	}

	public int getOldStatus() {
		return this.oldStatus;
	}

	@Override
	public String toString() {
		return "Job status changed from \""
				+ JobConstants.translateStatus(oldStatus) + "\" to \""
				+ JobConstants.translateStatus(newStatus) + "\" for job "
				+ job.getJobname();
	}

}
