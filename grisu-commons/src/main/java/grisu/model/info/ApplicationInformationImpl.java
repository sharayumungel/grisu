package grisu.model.info;

import grisu.control.ServiceInterface;
import grisu.jcommons.constants.Constants;
import grisu.jcommons.constants.JobSubmissionProperty;
import grisu.model.GrisuRegistryManager;
import grisu.model.info.dto.DtoProperties;
import grisu.model.info.dto.Executable;
import grisu.model.info.dto.JobQueueMatch;
import grisu.model.info.dto.Package;
import grisu.model.info.dto.Queue;
import grisu.model.info.dto.Version;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import com.google.common.collect.Sets;

/**
 * Default implementation for {@link ApplicationInformation}.
 *
 * @author Markus Binsteiner
 */
public class ApplicationInformationImpl implements ApplicationInformation {

	private final ServiceInterface serviceInterface;
	private final String application;

	private final ResourceInformation resourceInfo;

	private final Map<String, Package> cachedApplicationDetailsPerSubLoc = new HashMap<String, Package>();

	private Queue[] cachedAllSubmissionLocations = null;

	private final Map<String, Set<Version>> cachedVersionsPerSubmissionLocations = new HashMap<String, Set<Version>>();
	private final Map<String, Queue[]> cachedSubmissionLocationsPerVersion = new HashMap<String, Queue[]>();
	// private Map<String, Set<String>> cachedVersionsForSubmissionLocation =
	// new HashMap<String, Set<String>>();
	private final Map<String, Set<Queue>> cachedSubmissionLocationsForUserPerFqan = new HashMap<String, Set<Queue>>();
	private final Map<String, Set<Queue>> cachedSubmissionLocationsForUserPerVersionAndFqan = new HashMap<String, Set<Queue>>();

	private final Map<String, Set<Version>> cachedVersionsForUserPerFqan = new HashMap<String, Set<Version>>();

	/**
	 * Default constructor for this class.
	 *
	 * @param serviceInterface
	 *            the serviceinterface
	 * @param app
	 *            the name of the application
	 */
	public ApplicationInformationImpl(final ServiceInterface serviceInterface,
			final String app) {
		this.serviceInterface = serviceInterface;
		this.resourceInfo = GrisuRegistryManager.getDefault(serviceInterface)
				.getResourceInformation();
		this.application = app;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.vpac.grisu.model.info.ApplicationInformation#
	 * getAllAvailableVersionsForFqan(java.lang.String)
	 */
	public final Set<Version> getAllAvailableVersionsForFqan(final String fqan) {

		synchronized (fqan) {

			if (cachedVersionsForUserPerFqan.get(fqan) == null) {
				final Set<Version> result = new TreeSet<Version>();
				for (final Queue subLoc : getAvailableSubmissionLocationsForFqan(fqan)) {
					final List<Version> temp = serviceInterface
							.getVersionsOfApplicationOnSubmissionLocation(
									getApplicationName(), subLoc.toString());
					result.addAll(temp);
				}
				cachedVersionsForUserPerFqan.put(fqan, result);
			}
		}
		return cachedVersionsForUserPerFqan.get(fqan);
	}

	// public List<Queue> getAllSubmissionLocations(
	// Map<JobSubmissionProperty, String> additionalJobProperties,
	// String fqan) {
	//
	// if (Thread.currentThread().isInterrupted()) {
	// return null;
	// }
	//
	// final Map<JobSubmissionProperty, String> basicJobProperties = new
	// HashMap<JobSubmissionProperty, String>();
	// basicJobProperties.put(JobSubmissionProperty.APPLICATIONNAME,
	// getApplicationName());
	//
	// basicJobProperties.putAll(additionalJobProperties);
	//
	// final Map<String, String> converterMap = new HashMap<String, String>();
	// for (final JobSubmissionProperty key : basicJobProperties.keySet()) {
	// if (StringUtils.isNotBlank(basicJobProperties.get(key))) {
	// converterMap.put(key.toString(), basicJobProperties.get(key));
	// }
	// }
	//
	// return getServiceInterface().findMatchingSubmissionLocationsUsingMap(
	// DtoJob.createJob(JobConstants.UNDEFINED, converterMap, null,
	// null, false), fqan, false);
	// }

	// public SortedSet<GridResource> getAllSubmissionLocationsAsGridResources(
	// Map<JobSubmissionProperty, String> additionalJobProperties,
	// String fqan) {
	//
	// if (Thread.currentThread().isInterrupted()) {
	// return null;
	// }
	//
	// final Map<JobSubmissionProperty, String> basicJobProperties = new
	// HashMap<JobSubmissionProperty, String>();
	// basicJobProperties.put(JobSubmissionProperty.APPLICATIONNAME,
	// getApplicationName());
	//
	// basicJobProperties.putAll(additionalJobProperties);
	//
	// final Map<String, String> converterMap = new HashMap<String, String>();
	// for (final JobSubmissionProperty key : basicJobProperties.keySet()) {
	// if (StringUtils.isNotBlank(basicJobProperties.get(key))) {
	// converterMap.put(key.toString(), basicJobProperties.get(key));
	// }
	// }
	//
	// return getServiceInterface().findMatchingSubmissionLocationsUsingMap(
	// DtoJob.createJob(JobConstants.UNDEFINED, converterMap, null,
	// null, false), fqan, false)
	// .wrapGridResourcesIntoInterfaceType();
	// }

	/*
	 * (non-Javadoc)
	 *
	 * @see grisu.model.info.ApplicationInformation#getApplicationDetails
	 * (java.lang.String, java.lang.String)
	 */
	public final Package getApplicationDetails(final String subLoc,
			final String version) {
		final String KEY = version + "_subLoc:" + subLoc;

		synchronized (KEY) {
			if (cachedApplicationDetailsPerSubLoc.get(KEY) == null) {
				if (Constants.GENERIC_APPLICATION_NAME.equals(application)) {
					cachedApplicationDetailsPerSubLoc.put(KEY,
							Package.GENERIC_PACKAGE);
				} else {
					final Package details = serviceInterface
							.getApplicationDetailsForVersionAndSubmissionLocation(
									application, version, subLoc);

					cachedApplicationDetailsPerSubLoc.put(KEY, details);
				}
			}
		}

		return cachedApplicationDetailsPerSubLoc.get(KEY);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see grisu.model.info.ApplicationInformation#getApplicationName()
	 */
	public final String getApplicationName() {
		return application;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.vpac.grisu.model.info.ApplicationInformation#
	 * getAvailableAllSubmissionLocations()
	 */
	public synchronized final Queue[] getAvailableAllSubmissionLocations() {

		if (cachedAllSubmissionLocations == null) {
			if (Constants.GENERIC_APPLICATION_NAME.equals(application)) {
				cachedAllSubmissionLocations = serviceInterface
						.getAllSubmissionLocations();
			} else {

				cachedAllSubmissionLocations = serviceInterface
						.getSubmissionLocationsForApplication(application);
				// cachedAllSubmissionLocations = new HashSet(
				// Arrays.asList(serviceInterface
				// .getSubmissionLocationsForApplication(
				// application)
				// .asSubmissionLocationStrings()));
			}
		}
		return cachedAllSubmissionLocations;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.vpac.grisu.model.info.ApplicationInformation#
	 * getAvailableSubmissionLocationsForFqan(java.lang.String)
	 */
	public final Set<Queue> getAvailableSubmissionLocationsForFqan(
			final String fqan) {

		synchronized (fqan) {

			if (cachedSubmissionLocationsForUserPerFqan.get(fqan) == null) {
				final Set<Queue> temp = Sets.newHashSet();
				for (final Queue subLoc : resourceInfo
						.getAllAvailableSubmissionLocations(fqan)) {
					if (Arrays.asList(getAvailableAllSubmissionLocations())
							.contains(subLoc)) {
						temp.add(subLoc);
					}
				}
				cachedSubmissionLocationsForUserPerFqan.put(fqan, temp);
			}
		}
		return cachedSubmissionLocationsForUserPerFqan.get(fqan);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.vpac.grisu.model.info.ApplicationInformation#
	 * getAvailableSubmissionLocationsForVersion(java.lang.String)
	 */
	public final Queue[] getAvailableSubmissionLocationsForVersion(
			final String version) {

		synchronized (version) {

			if (cachedSubmissionLocationsPerVersion.get(version) == null) {
				if (Constants.NO_VERSION_INDICATOR_STRING.equals(version)) {
					cachedSubmissionLocationsPerVersion.put(version,
							getAvailableAllSubmissionLocations());
				} else {

					final Queue[] temp = serviceInterface
							.getSubmissionLocationsForApplicationAndVersion(
									application, version);
					// .asSubmissionLocationStrings());
					cachedSubmissionLocationsPerVersion.put(version, temp);
				}
			}
		}
		return cachedSubmissionLocationsPerVersion.get(version);
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @seeorg.vpac.grisu.model.info.ApplicationInformation#
	// * getAvailableSubmissionLocationsForVersionAndFqan(java.lang.String,
	// * java.lang.String)
	// */
	public final Set<Queue> getAvailableSubmissionLocationsForVersionAndFqan(
			final String version, final String fqan) {

		final String KEY = version + "_" + fqan;

		synchronized (KEY) {

			if (cachedSubmissionLocationsForUserPerVersionAndFqan.get(KEY) == null) {
				final Set<Queue> temp = new HashSet<Queue>();
				for (final Queue subLoc : resourceInfo
						.getAllAvailableSubmissionLocations(fqan)) {
					if (Arrays.asList(
							getAvailableSubmissionLocationsForVersion(version))
							.contains(subLoc)) {
						temp.add(subLoc);
					}
				}
				cachedSubmissionLocationsForUserPerVersionAndFqan
				.put(KEY, temp);
			}
		}
		return cachedSubmissionLocationsForUserPerVersionAndFqan.get(KEY);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see grisu.model.info.ApplicationInformation#getAvailableVersions
	 * (java.lang.String)
	 */
	public final Set<Version> getAvailableVersions(final String subLoc) {

		final String KEY = subLoc;

		synchronized (KEY) {

			if (cachedVersionsPerSubmissionLocations.get(KEY) == null) {
				if (Constants.GENERIC_APPLICATION_NAME.equals(application)) {
					final Set<Version> temp = new HashSet<Version>();
					temp.add(Version.ANY_VERSION);
					cachedVersionsPerSubmissionLocations.put(KEY, temp);
				} else {
					final List<Version> temp = serviceInterface
							.getVersionsOfApplicationOnSubmissionLocation(
									application, subLoc);
					cachedVersionsPerSubmissionLocations.put(KEY,
							new HashSet<Version>(temp));
				}
			}
		}
		return cachedVersionsPerSubmissionLocations.get(KEY);

	}

	// public final SortedSet<GridResource> getBestSubmissionLocations(
	// final Map<JobSubmissionProperty, String> additionalJobProperties,
	// final String fqan) {
	//
	// final Map<JobSubmissionProperty, String> basicJobProperties = new
	// HashMap<JobSubmissionProperty, String>();
	// basicJobProperties.put(JobSubmissionProperty.APPLICATIONNAME,
	// getApplicationName());
	//
	// if (additionalJobProperties != null) {
	// basicJobProperties.putAll(additionalJobProperties);
	// }
	//
	// final Map<String, String> converterMap = new HashMap<String, String>();
	// for (final JobSubmissionProperty key : basicJobProperties.keySet()) {
	// converterMap.put(key.toString(), basicJobProperties.get(key));
	// }
	//
	// return getServiceInterface().findMatchingSubmissionLocationsUsingMap(
	// DtoJob.createJob(JobConstants.UNDEFINED, converterMap, null,
	// null, false), fqan, true)
	// .wrapGridResourcesIntoInterfaceType();
	// }

	/*
	 * (non-Javadoc)
	 *
	 * @see grisu.model.info.ApplicationInformation#getExecutables(java.
	 * lang.String, java.lang.String)
	 */
	public final Set<Executable> getExecutables(final String subLoc,
			final String version) {

		Package pkg = getApplicationDetails(subLoc, version);

		return pkg.getExecutables();
	}

	public final Set<Executable> getExecutablesForVo(final String fqan) {

		return getExecutablesForVo(fqan, Constants.NO_VERSION_INDICATOR_STRING);
	}

	public final Set<Executable> getExecutablesForVo(final String fqan,
			String version) {

		final ResourceInformation ri = GrisuRegistryManager.getDefault(
				serviceInterface).getResourceInformation();
		final Queue[] subLocs = ri.getAllAvailableSubmissionLocations(fqan);
		final Set<Executable> result = Collections
				.synchronizedSet(new HashSet<Executable>());
		for (final Queue subLoc : subLocs) {
			final Set<Executable> exes = getExecutables(subLoc.toString(),
					version);
			result.addAll(exes);
		}

		return result;
	}

	public List<JobQueueMatch> getMatches(
			Map<JobSubmissionProperty, String> additionalJobProperties,
			String fqan) {

		if (Thread.currentThread().isInterrupted()) {
			return null;
		}

		final Map<JobSubmissionProperty, String> basicJobProperties = new HashMap<JobSubmissionProperty, String>();
		basicJobProperties.put(JobSubmissionProperty.APPLICATIONNAME,
				getApplicationName());

		basicJobProperties.putAll(additionalJobProperties);

		final Map<String, String> converterMap = new HashMap<String, String>();
		for (final JobSubmissionProperty key : basicJobProperties.keySet()) {
			if (StringUtils.isNotBlank(basicJobProperties.get(key))) {
				converterMap.put(key.toString(), basicJobProperties.get(key));
			}
		}

		List<JobQueueMatch> qs = getServiceInterface().findMatches(
				DtoProperties.createProperties(converterMap), fqan);

		return qs;
	}

	public List<Queue> getQueues(
			Map<JobSubmissionProperty, String> additionalJobProperties,
			String fqan) {


		if (Thread.currentThread().isInterrupted()) {
			return null;
		}

		final Map<JobSubmissionProperty, String> basicJobProperties = new
				HashMap<JobSubmissionProperty, String>();
		basicJobProperties.put(JobSubmissionProperty.APPLICATIONNAME,
				getApplicationName());

		basicJobProperties.putAll(additionalJobProperties);

		final Map<String, String> converterMap = new HashMap<String, String>();
		for (final JobSubmissionProperty key : basicJobProperties.keySet()) {
			if (StringUtils.isNotBlank(basicJobProperties.get(key))) {
				converterMap.put(key.toString(), basicJobProperties.get(key));
			}
		}


		List<Queue> qs = getServiceInterface()
				.findQueues(
						DtoProperties.createProperties(converterMap), fqan);

		return qs;

	}

	public final ResourceInformation getResourceInfo() {
		return resourceInfo;
	}

	public final ServiceInterface getServiceInterface() {
		return serviceInterface;
	}

}
