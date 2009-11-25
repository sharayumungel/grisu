package org.vpac.grisu.model.dto;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
@XmlRootElement(name="userproperties")
public class DtoProperties {
	
	public static DtoProperties createUserPropertiesIntegerValue(Map<String, Integer> userProperties) {
		
		DtoProperties result = new DtoProperties();
		
		List<DtoProperty> list = new LinkedList<DtoProperty>();
		for ( String key : userProperties.keySet() ) {
			DtoProperty temp = new DtoProperty();
			temp.setKey(key);
			temp.setValue(userProperties.get(key).toString());
			list.add(temp);
		}
		result.setProperties(list);
		
		return result;
	}
	
	public static DtoProperties createUserProperties(Map<String, String> userProperties) {
		
		DtoProperties result = new DtoProperties();
		
		List<DtoProperty> list = new LinkedList<DtoProperty>();
		for ( String key : userProperties.keySet() ) {
			DtoProperty temp = new DtoProperty();
			temp.setKey(key);
			temp.setValue(userProperties.get(key));
			list.add(temp);
		}
		result.setProperties(list);
		
		return result;
	}
	

	/**
	 * The list of user properties.
	 */
	private List<DtoProperty> properties = new LinkedList<DtoProperty>();
	


	@XmlElement(name="userproperty")
	public List<DtoProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<DtoProperty> properties) {
		this.properties = properties;
	}
	
	public Map<String, String> propertiesAsMap() {
		
		Map<String, String> map = new HashMap<String, String>();
		
		for ( DtoProperty prop : getProperties() ) {
			map.put(prop.getKey(), prop.getValue());
		}
		
		return map;
	}
	
	public void addUserProperty(String key, String value) {
		properties.add(new DtoProperty(key, value));
	}
	
	public String readUserProperty(String key) {
		return propertiesAsMap().get(key);
	}

	
}