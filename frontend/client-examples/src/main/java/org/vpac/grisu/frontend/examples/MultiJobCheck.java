package org.vpac.grisu.frontend.examples;

import java.io.File;
import java.util.Date;

import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.frontend.control.login.LoginParams;
import org.vpac.grisu.frontend.control.login.ServiceInterfaceFactory;
import org.vpac.grisu.frontend.model.job.DoubleWalltimeJobRestarter;
import org.vpac.grisu.frontend.model.job.FailedJobRestarter;
import org.vpac.grisu.frontend.model.job.MultiPartJobObject;

public class MultiJobCheck  {

	public static void main(final String[] args) throws Exception {

		String username = args[0];
		char[] password = args[1].toCharArray();
		
		Date startDate = new Date();

		LoginParams loginParams = new LoginParams(
//				"http://localhost:8080/xfire-backend/services/grisu",
//				"https://ngportal.vpac.org/grisu-ws/soap/EnunciateServiceInterfaceService",
//				 "https://ngportal.vpac.org/grisu-ws/services/grisu",
				 "Local",
				username, password);

		final ServiceInterface si = ServiceInterfaceFactory
				.createInterface(loginParams);

		final String multiJobName = "MultiJobTest3";
		
		MultiPartJobObject newObject = new MultiPartJobObject(si, multiJobName, false);
		
		FailedJobRestarter restarter = new DoubleWalltimeJobRestarter();

		newObject.monitorProgress(15, null, true, restarter);
	
		
		newObject.downloadResults(new File("/home/markus/Desktop/multiTest"), new String[]{"stdout", "stderr"}, false, true);

		Date endDate = new Date();
		
		System.out.println("Started: "+startDate.toString());
		System.out.println("Ended: "+endDate.toString()+"\n");
		
		for ( Date date : newObject.getLogMessages(false).keySet() ) {
			System.out.println(date.toString()+": "+newObject.getLogMessages(false).get(date));
		}
		
		
		
	}

}