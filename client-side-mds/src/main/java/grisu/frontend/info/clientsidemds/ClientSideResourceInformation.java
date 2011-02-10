package grisu.frontend.info.clientsidemds;

import grisu.jcommons.interfaces.InformationManager;
import grisu.model.GrisuRegistry;
import grisu.model.info.ResourceInformation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;


public class ClientSideResourceInformation implements ResourceInformation {

	static final Logger myLogger = Logger
			.getLogger(ClientSideResourceInformation.class.getName());

	private final GrisuRegistry registry;
	private final InformationManager infoManager;

	public ClientSideResourceInformation(GrisuRegistry registry,
			InformationManager infoManager) {
		this.registry = registry;
		this.infoManager = infoManager;
	}

	public Set<String> distillSitesFromSubmissionLocations(
			Set<String> submissionLocations) {

		final Set<String> temp = new TreeSet<String>();
		for (final String subLoc : submissionLocations) {
			String site = null;
			try {
				site = getSite(subLoc);
				temp.add(site);
			} catch (final Exception e) {
				myLogger.error("Could not get site for submissionlocation: "
						+ subLoc + ", ignoring it. Error: "
						+ e.getLocalizedMessage());
			}
		}
		return temp;
	}

	public Set<String> filterSubmissionLocationsForSite(String site,
			Set<String> submissionlocations) {

		final Set<String> temp = new TreeSet<String>();
		for (final String subLoc : submissionlocations) {
			if (site.equals(getSite(subLoc))) {
				temp.add(subLoc);
			}
		}
		return temp;
	}

	public Set<String> getAllApplications() {
		return new TreeSet<String>(Arrays.asList(infoManager
				.getAllApplicationsOnGrid()));
	}

	public Set<String> getAllAvailableSites(String fqan) {
		final Set<String> temp = new TreeSet<String>();
		for (final String subLoc : getAllAvailableSubmissionLocations(fqan)) {
			temp.add(getSite(subLoc));
		}
		return temp;
	}

	public String[] getAllAvailableSubmissionLocations(String fqan) {
		return infoManager.getAllSubmissionLocationsForVO(fqan);
	}

	public String[] getAllSubmissionLocations() {
		return infoManager.getAllSubmissionLocations();
	}

	public String[] getApplicationPackageForExecutable(String executable) {
		return infoManager.getApplicationsThatProvideExecutable(executable);
	}

	public String getRecommendedStagingFileSystemForSubmissionLocation(
			String subLoc) {

		final List<String> temp = getStagingFilesystemsForSubmissionLocation(subLoc);
		if ((temp != null) && (temp.size() > 0)) {
			return temp.get(0);
		} else {
			return null;
		}
	}

	public String getSite(String urlOrSubmissionLocation) {

		return infoManager.getSiteForHostOrUrl(urlOrSubmissionLocation);

	}

	public List<String> getStagingFilesystemsForSubmissionLocation(String subLoc) {
		return Arrays.asList(infoManager
				.getStagingFileSystemForSubmissionLocation(subLoc));
	}

}