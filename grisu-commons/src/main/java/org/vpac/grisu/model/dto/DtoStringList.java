package org.vpac.grisu.model.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="list")
public class DtoStringList {
	
	public static DtoStringList fromStringArray(String[] array) {
		
		DtoStringList result = new DtoStringList();
		result.setStringList(Arrays.asList(array));
		
		return result;
		
	}
	
	public static DtoStringList fromSingleString(String string) {
		DtoStringList result = new DtoStringList();
		List<String> list = new LinkedList<String>();
		list.add(string);
		result.setStringList(list);
		return result;
	}
	
	public static DtoStringList fromStringList(List<String> list) {
		
		DtoStringList result = new DtoStringList();
		result.setStringList(list);
		
		return result;
		
	}
	
	public static DtoStringList fromStringColletion(Collection<String> list) {
		
		DtoStringList result = new DtoStringList();
		result.setStringList(new LinkedList(list));
		
		return result;
		
	}
	
	private List<String> stringList = new LinkedList<String>();

	@XmlElement(name="listElement")
	public List<String> getStringList() {
		return stringList;
	}

	public void setStringList(List<String> stringList) {
		this.stringList = stringList;
	}
	
	public String[] asArray() {
		if ( this.stringList != null ) {
			return this.stringList.toArray(new String[]{});
		} else {
			return null;
		}
	}
	
	
	

}