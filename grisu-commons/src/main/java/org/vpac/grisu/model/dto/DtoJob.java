package org.vpac.grisu.model.dto;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.vpac.grisu.control.JobConstants;

import au.org.arcs.jcommons.constants.Constants;

/**
 * This one holds information about a job that was created (and maybe already submitted to the endpoint resource).
 * 
 * You can use this to query information like job-directory and status of the job. Have a look in the 
 * Constants class in the GlueInterface module of the Infosystems project
 * for values of keys of possible job properties.
 * 
 * @author Markus Binsteiner
 *
 */
@XmlRootElement(name="job")
public class DtoJob {
	
	public static DtoJob createJob(int status, Map<String, String> jobProperties) {
		
		DtoJob result = new DtoJob();
		
		result.setStatus(status);

		List<DtoJobProperty> list = new LinkedList<DtoJobProperty>();
		for ( String key : jobProperties.keySet() ) {
			DtoJobProperty temp = new DtoJobProperty();
			temp.setKey(key);
			temp.setValue(jobProperties.get(key));
			list.add(temp);
		}
		
		result.setProperties(list);
		return result;
	}
	

	/**
	 * The list of job properties.
	 */
	private List<DtoJobProperty> properties = new LinkedList<DtoJobProperty>();
	/**
	 * The status of the job. Be aware that, depending on how you queried for this job, this can be stale information.
	 */
	private int status;

	@XmlElement(name="jobproperty")
	public List<DtoJobProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<DtoJobProperty> properties) {
		this.properties = properties;
	}
	
	public Map<String, String> propertiesAsMap() {
		
		Map<String, String> map = new HashMap<String, String>();
		
		for ( DtoJobProperty prop : getProperties() ) {
			map.put(prop.getKey(), prop.getValue());
		}
		
		return map;
	}

	@XmlElement(name="status")
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String statusAsString() {
		return JobConstants.translateStatus(status);
	}
	
	public void addJobProperty(String key, String value) {
		properties.add(new DtoJobProperty(key, value));
	}
	
	public String readJobProperty(String key) {
		return propertiesAsMap().get(key);
	}
	
	public String jobname() {
		return propertiesAsMap().get(Constants.JOBNAME_KEY);
	}
	
}
