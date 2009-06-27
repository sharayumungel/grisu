package org.vpac.grisu.client.control.example;

import org.vpac.grisu.client.control.login.LoginParams;
import org.vpac.grisu.client.control.login.ServiceInterfaceFactory;
import org.vpac.grisu.control.GrisuRegistry;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.ServiceInterfaceException;
import org.vpac.grisu.model.ApplicationInformation;

public class JobCreationInfo {
	
	public static void main(String[] args) throws ServiceInterfaceException {
		
		String username = args[0];
		char[] password = args[1].toCharArray();

		LoginParams loginParams = new LoginParams(
		// "http://localhost:8080/grisu-ws/services/grisu",
				// "https://ngportaldev.vpac.org/grisu-ws/services/grisu",
				"Local", username, password);

		ServiceInterface si = null;
		si = ServiceInterfaceFactory.createInterface(loginParams);
		
		
		GrisuRegistry registry = GrisuRegistry.getDefault(si);
		
		ApplicationInformation appInfo = registry.getApplicationInformationObject("java");
		for ( String version : appInfo.getAllAvailableVersionsForFqan("/ARCS/NGAdmin") ) {
			System.out.println(version);
		}
		
		
	}

}
