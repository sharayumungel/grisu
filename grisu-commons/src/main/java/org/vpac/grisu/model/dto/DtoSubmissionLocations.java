package org.vpac.grisu.model.dto;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A wrapper that holds a list of {@link DtoSubmissionLocationInfo} objects.
 * 
 * @author Markus Binsteiner
 *
 */
@XmlRootElement(name="submissionlocations")
@XmlAccessorType(XmlAccessType.FIELD)
public class DtoSubmissionLocations {
	
	public static DtoSubmissionLocations createSubmissionLocationsInfo(String[] submissionLocations) {
		
		DtoSubmissionLocations result = new DtoSubmissionLocations();
		
		List<DtoSubmissionLocationInfo> subLocs = new LinkedList<DtoSubmissionLocationInfo>();
		for ( String subLoc : submissionLocations ) {
			DtoSubmissionLocationInfo temp = new DtoSubmissionLocationInfo();
			temp.setSubmissionLocation(subLoc);
			subLocs.add(temp);
		}
		
		result.setAllSubmissionLocations(subLocs);
		
		return result;
		
	}

	/**
	 * The list of submission location objects.
	 */
	@XmlElement(name="submissionlocation")
	private List<DtoSubmissionLocationInfo> allSubmissionLocations = new LinkedList<DtoSubmissionLocationInfo>();

	public List<DtoSubmissionLocationInfo> getAllSubmissionLocations() {
		return allSubmissionLocations;
	}

	public void setAllSubmissionLocations(
			List<DtoSubmissionLocationInfo> allSubmissionLocations) {
		this.allSubmissionLocations = allSubmissionLocations;
	}
	
	public String[] getSubmissionLocationStrings() {
		
		String[] result = new String[allSubmissionLocations.size()];
		
		for ( int i=0; i<result.length; i++ ) {
			result[i] = getAllSubmissionLocations().get(i).getSubmissionLocation();
		}
		return result;
	}
	
	
}
