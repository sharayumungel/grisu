package org.vpac.grisu.settings;

import java.io.File;

/**
 * This class manages the location/values of some required files/environment
 * variables.
 * 
 * @author Markus Binsteiner
 * 
 */
public final class Environment {
	
	private Environment() {
	}

	public static final String GRISU_DIRECTORY = System
			.getProperty("user.home")
			+ File.separator + ".grisu";

	public static final String TEMPLATE_DIRECTORY = GRISU_DIRECTORY
			+ File.separator + "templates";

	public static final String CACHE_DIR_NAME = "cache";

	public static final String AVAILABLE_TEMPLATES_DIRECTORY = GRISU_DIRECTORY
			+ File.separator + "templates_available";

	public static final String GLOBUS_HOME = GRISU_DIRECTORY + File.separator
			+ "globus";

	public static final String AXIS_CLIENT_CONFIG = GLOBUS_HOME
			+ File.separator + "client-config.wsdd";

	public static final String GRISU_PLUGIN_DIRECTORY = GRISU_DIRECTORY
			+ File.separator + "plugins";

	public static final File LOCAL_CACHE_ROOT = getGrisuLocalCacheRoot();

	/**
	 * For some jobs/applications it is useful to cache output files locally so
	 * they don't have to be transferred over and over again.
	 * 
	 * @return the location of the local directory where all job output files
	 *         are chached (in subdirectories named after the jobname)
	 */
	public static File getLocalJobCacheDirectory() {

		File dir = new File(getGrisuDirectory(), "jobs");
		dir.mkdirs();
		return dir;
	}

	/**
	 * This one returns the location where grisu specific config/cache files are
	 * stored. If it does not exist it gets created.
	 * 
	 * @return the location of grisu specific config/cache files
	 */
	public static File getGrisuDirectory() {

		File grisuDir = new File(GRISU_DIRECTORY);
		if (!grisuDir.exists()) {
			grisuDir.mkdirs();
		}
		return grisuDir;
	}

	/**
	 * The location where the remote filesystems are cached locally.
	 * 
	 * @return the root of the local cache
	 */
	public static File getGrisuLocalCacheRoot() {
		File root = new File(getGrisuDirectory(), CACHE_DIR_NAME);
		if (!root.exists()) {
			if (!root.mkdirs()) {
				if (!root.exists()) {
					throw new RuntimeException(
							"Could not create local cache root directory: "
									+ root.getAbsolutePath()
									+ ". Please check the permissions.");
				}
			}
		}
		return root;
	}

}
