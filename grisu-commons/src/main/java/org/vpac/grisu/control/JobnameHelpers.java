package org.vpac.grisu.control;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class JobnameHelpers {

	public static String calculateTimestampedJobname(String baseJobname) {
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy_HH.mm.SSS");
		return calculateTimestampedJobname(baseJobname, format);
	}

	public static String calculateTimestampedJobname(String baseJobname, SimpleDateFormat format) {
		String newJobname = baseJobname+"_"+format.format(new Date());
		newJobname = newJobname.replace(":", "_");
		newJobname = newJobname.replace("\\", "_");
		newJobname = newJobname.replace("/", "_");
		newJobname = newJobname.replaceAll("\\s", "_");
		return newJobname;
	}

	public static String calculateUUIDJobname(String basejobname) {
		return basejobname + "_" +UUID.randomUUID().toString();
	}

}