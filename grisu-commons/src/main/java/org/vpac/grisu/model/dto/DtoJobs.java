package org.vpac.grisu.model.dto;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import au.org.arcs.jcommons.constants.Constants;

/**
 * A wrapper object that holds a list of {@link DtoJob} objects.
 * 
 * @author Markus Binsteiner
 *
 */
@XmlRootElement(name="jobs")
public class DtoJobs {
	
	/**
	 * The list of jobs.
	 */
	private SortedSet<DtoJob> allJobs = Collections.synchronizedSortedSet(new TreeSet<DtoJob>());

	@XmlElement(name="job")
	public SortedSet<DtoJob> getAllJobs() {
		return allJobs;
	}

	public void setAllJobs(SortedSet<DtoJob> allJobs) {
		this.allJobs = allJobs;
	}
	
	public void addJob(DtoJob job) {
		this.allJobs.add(job);
	}
	
	public DtoJob retrieveJob(String jobname) {
		for ( DtoJob job : allJobs ) {
			if ( jobname.equals(job.propertiesAsMap().get(Constants.JOBNAME_KEY)) ) {
				return job;
			}
		}
		return null;
	}
	

}
