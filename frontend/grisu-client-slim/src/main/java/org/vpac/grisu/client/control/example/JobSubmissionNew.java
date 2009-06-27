package org.vpac.grisu.client.control.example;

import org.vpac.grisu.client.control.login.LoginParams;
import org.vpac.grisu.client.control.login.ServiceInterfaceFactory;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.js.model.JobSubmissionObjectImpl;

public class JobSubmissionNew {
	
	public static void main(String[] args) throws Exception {
		
		String username = args[0];
		char[] password = args[1].toCharArray();

		LoginParams loginParams = new LoginParams(
		// "http://localhost:8080/grisu-ws/services/grisu",
				// "https://ngportaldev.vpac.org/grisu-ws/services/grisu",
				"Local", username, password);

		ServiceInterface si = null;
		si = ServiceInterfaceFactory.createInterface(loginParams);
		
		JobSubmissionObjectImpl jso = new JobSubmissionObjectImpl();
		jso.setJobname("javaMarsssss");
		jso.setApplication("java");
		jso.setCommandline("java -version");  
		
		String jobname = si.createJob(jso.getStringJobPropertyMap(), "/ARCS/VPAC", "force-name");
		
		si.submitJob(jobname);
		
		
	}

}
