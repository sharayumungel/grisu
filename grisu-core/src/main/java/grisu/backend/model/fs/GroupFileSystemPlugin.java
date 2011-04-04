package grisu.backend.model.fs;

import grisu.backend.model.User;
import grisu.control.ServiceInterface;
import grisu.control.exceptions.RemoteFileSystemException;
import grisu.model.FileManager;
import grisu.model.MountPoint;
import grisu.model.dto.GridFile;
import grisu.settings.ServerPropertiesManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * A plugin that lists (non-volatile) filesystems in a tree-like group
 * structure.
 * 
 * The base url for this plugin is grid://groups . The next token will be the
 * name of the VO and beneath that this plugin will populate folders with both
 * sub-vos and files (provided the VO/Sub-VO inquestion is associated with a
 * filesystem).
 * 
 * Groups and "real" files will be merged in the child files of a url.
 * 
 * Since the way the filelisting is done can be a bit slow at times, if you only
 * want to know the child files for a certain VO, seperate the VO-part of the
 * url and the path-part using //, e.g. grid://groups/nz/NeSI//folder1/folder2 .
 * This will make sure to only query all mountpoints associated with the
 * /nz/NeSI VO, but it will not list filesystems possibly associated to the /nz
 * VO for a (real) folder called /NeSI.
 * 
 * 
 * @author Markus Binsteiner
 * 
 */
public class GroupFileSystemPlugin implements VirtualFileSystemPlugin {

	static final Logger myLogger = Logger.getLogger(GroupFileSystemPlugin.class
			.getName());

	public static final String IDENTIFIER = "groups";

	private final static String BASE = (ServiceInterface.VIRTUAL_GRID_PROTOCOL_NAME
			+ "://" + IDENTIFIER);

	private final User user;

	public GroupFileSystemPlugin(User user) {
		this.user = user;
	}

	public GridFile createGridFile(final String path, int recursiveLevels)
	throws RemoteFileSystemException {

		// Thread.dumpStack();
		if (recursiveLevels > 1) {
			throw new RuntimeException(
			"Recursion levels greater than 1 not supported yet");
		}

		String rightPart = path.substring(BASE.length());
		if (rightPart.contains("//")) {
			GridFile result = null;

			// that means we everything before the // is the fqan and everything
			// after is the path

			int i = rightPart.indexOf("//");
			String fqanT = rightPart.substring(0, i);
			String restPath = rightPart.substring(i + 2);
			if (!fqanT.startsWith("/")) {
				fqanT = "/" + fqanT;
			}

			Set<String> urls = resolveUrls(restPath, fqanT);

			if (urls.size() == 0) {
				// TODO not sure what to do
				throw new RuntimeException("No real url found for virtual url.");
			} else if (urls.size() == 1) {
				result = new GridFile(urls.iterator().next(), -1L);
				result.setIsVirtual(false);
				result.setPath(path);
			} else {
				result = new GridFile(path, -1L);
				result.setIsVirtual(true);
				result.setPath(path);
			}

			if (recursiveLevels == 0) {
				return result;
			}

			Set<GridFile> childs = listGroup(fqanT, restPath);

			for (GridFile file : childs) {

				if (file.isInaccessable()) {
					result.addChild(file);
				} else {
					result.addChildren(file.getChildren());
				}

			}

			Map<String, Set<String>> temp = findDirectChildFqans(fqanT);
			Set<String> childFqans = temp.keySet();
			for (String fqan : childFqans) {

				Set<MountPoint> mps = user.getMountPoints(fqan);
				// we need to remove all volatile mountpoints first, users are
				// not interested in those
				Iterator<MountPoint> it = mps.iterator();
				while (it.hasNext()) {
					MountPoint mp = it.next();
					if (mp.isVolatile()) {
						it.remove();
					}
				}

				if (mps.size() == 1) {
					GridFile file = new GridFile(mps.iterator().next());
					file.setName(FileManager.getFilename(fqan));
					String pathNew = (path + "/" + file.getName()).replace(
							"///",
					"/").replace("//", "/")
					+ "//";
					file.setPath(pathNew);
					file.setIsVirtual(true);
					file.addSites(temp.get(fqan));
					result.addChild(file);
				} else {
					GridFile file = new GridFile((BASE + fqan).replace("///",
					"/").replace("//", "/")
					+ "//", fqan);
					String pathNew = (path + "/" + file.getName()).replace(
							"///",
					"/").replace("//", "/")
					+ "//";
					file.setPath(pathNew);
					for (MountPoint mp : mps) {
						// X.p("Add" + mp.getRootUrl());
						file.addUrl(mp.getRootUrl(),
								GridFile.FILETYPE_MOUNTPOINT_PRIORITY);
					}
					file.setIsVirtual(true);
					file.addSites(temp.get(fqan));
					result.addChild(file);
				}
			}

			return result;

		}

		int index = BASE.length();
		String importantUrlPart = path.substring(index);
		String[] tokens = StringUtils.split(importantUrlPart, '/');

		GridFile result = null;

		if (tokens.length == 0) {
			// means root of the groupfilesystem

			result = new GridFile(path, -1L);
			result.setIsVirtual(true);
			result.setPath(path);

			if (recursiveLevels == 0) {
				return result;
			}

			for (String vo : user.getFqans().values()) {
				GridFile f = new GridFile(BASE + "/" + vo, -1L);
				f.setIsVirtual(true);
				f.setPath(path + "/" + vo);

				for (MountPoint mp : user.getMountPoints("/" + vo)) {
					f.addSite(mp.getSite());
					f.addUrl(mp.getRootUrl(),
							GridFile.FILETYPE_MOUNTPOINT_PRIORITY);
				}

				result.addChild(f);
			}

		} else if (tokens.length == 1) {
			// means is root of VO so we need to list potential files on all
			// sites that support this vo
			// and also all child vos
			String parentfqan = "/" + tokens[0];
			Set<String> urls = resolveUrls(path, parentfqan);

			if (urls.size() == 1) {
				result = new GridFile(urls.iterator().next(), -1L);
				result.setIsVirtual(false);
				result.setPath(path);
			} else {
				result = new GridFile(path, -1L);
				result.setIsVirtual(true);
				result.setPath(path);
				for (String u : urls) {
					result.addUrl(u, GridFile.FILETYPE_MOUNTPOINT_PRIORITY);
				}
			}

			if (recursiveLevels == 0) {
				return result;
			}

			Map<String, Set<String>> temp = findDirectChildFqans(parentfqan);
			Set<String> childFqans = temp.keySet();

			for (String fqan : childFqans) {

				Set<MountPoint> mps = user.getMountPoints(fqan);

				// we need to remove volatile mountpoints
				Iterator<MountPoint> it = mps.iterator();
				while (it.hasNext()) {
					MountPoint mp = it.next();
					if (mp.isVolatile()) {
						it.remove();
					}
				}

				if (mps.size() == 1) {
					GridFile file = new GridFile(mps.iterator().next());
					file.setName(FileManager.getFilename(fqan));
					file.setPath(path + "/" + file.getName());
					file.setIsVirtual(true);
					file.addSites(temp.get(fqan));
					result.addChild(file);
				} else {
					GridFile file = new GridFile(BASE + fqan, fqan);
					file.setPath(path + "/" + file.getName());
					file.setIsVirtual(true);
					file.addSites(temp.get(fqan));
					for (MountPoint mp : mps) {
						file.addUrl(mp.getRootUrl(),
								GridFile.FILETYPE_MOUNTPOINT_PRIORITY);
					}
					file.addFqan(fqan);
					result.addChild(file);

				}
			}

			Set<GridFile> files = listGroup("/" + tokens[0], "");
			for (GridFile file : files) {
				if (file.isInaccessable()) {
					result.addChild(file);
				} else {
					result.addChildren(file.getChildren());
				}
			}

		} else {

			String currentUrl = BASE;
			String potentialFqan = "";

			Set<String> parentUrls = new HashSet<String>();
			Set<GridFile> children = new TreeSet<GridFile>();

			for (String token : tokens) {

				currentUrl = currentUrl + "/" + token;
				potentialFqan = potentialFqan + "/" + token;

				if (!user.getFqans().keySet().contains(potentialFqan)) {
					continue;
				}

				String rest = path.substring(currentUrl.length());

				Set<String> urls = resolveUrls(rest, potentialFqan);
				parentUrls.addAll(urls);

				if (recursiveLevels == 1) {

					Set<GridFile> files = listGroup(potentialFqan, rest);
					for (GridFile file : files) {

						if (file.isInaccessable()) {
							children.add(file);
						} else {
							children.addAll(file.getChildren());
						}
					}

				}

			}

			if (recursiveLevels == 1) {

				Map<String, Set<String>> temp = findDirectChildFqans(potentialFqan);
				Set<String> childFqans = temp.keySet();
				for (String fqan : childFqans) {
					Set<MountPoint> mps = user.getMountPoints(fqan);

					// we need to remove volatile mountpoints
					Iterator<MountPoint> it = mps.iterator();
					while (it.hasNext()) {
						MountPoint mp = it.next();
						if (mp.isVolatile()) {
							it.remove();
						}
					}
					if (mps.size() == 0) {
						continue;
					}
					if (mps.size() == 1) {
						GridFile file = new GridFile(mps.iterator().next());
						file.setName(FileManager.getFilename(fqan));
						file.setPath(path + "/" + file.getName() + "//");
						file.setIsVirtual(true);
						file.addSites(temp.get(fqan));
						children.add(file);
					} else {
						GridFile file = new GridFile(BASE + fqan, fqan);
						file.setPath(path + "/" + file.getName() + "//");
						file.setIsVirtual(true);
						file.addSites(temp.get(fqan));
						children.add(file);
					}
				}
			}

			if (parentUrls.size() == 1) {
				result = new GridFile(parentUrls.iterator().next(), -1L);
				result.setIsVirtual(false);
				result.setPath(path);
			} else {
				result = new GridFile(path, -1L);
				result.setIsVirtual(true);
				result.setPath(path);
				for (String u : parentUrls) {
					result.addUrl(u, GridFile.FILETYPE_FOLDER_PRIORITY);
				}
			}

			result.addChildren(children);
		}

		return result;

	}

	private Map<String, Set<String>> findDirectChildFqans(String parentFqan) {

		String[] tokens = parentFqan.substring(1).split("/");

		Map<String, Set<String>> result = new TreeMap<String, Set<String>>();

		for (String fqan : user.getFqans().keySet()) {

			if (!fqan.startsWith(parentFqan)) {
				continue;
			}

			String[] fqanTokens = fqan.substring(1).split("/");
			int fqanTokenLength = fqanTokens.length;

			if ((fqanTokenLength == tokens.length + 1)
					&& fqanTokens[tokens.length - 1]
					              .equals(tokens[tokens.length - 1])) {

				Set<String> sites = new TreeSet<String>();
				Set<MountPoint> mps = user.getMountPoints(fqan);
				// removing volatile mountpoints
				Iterator<MountPoint> it = mps.iterator();
				while (it.hasNext()) {
					MountPoint mp = it.next();
					if (mp.isVolatile()) {
						it.remove();
					}
				}

				if (mps.size() == 0) {
					continue;
				}

				for (MountPoint mp : mps) {
					sites.add(mp.getSite());
				}

				result.put(fqan, sites);
			}
		}
		return result;
	}

	private Set<GridFile> listGroup(String fqan, String path)
	throws RemoteFileSystemException {

		Set<MountPoint> mps = user.getMountPoints(fqan);

		// removing volatile mountpoints
		Iterator<MountPoint> it = mps.iterator();
		while (it.hasNext()) {
			MountPoint mp = it.next();
			if (mp.isVolatile()) {
				it.remove();
			}
		}

		if (mps.size() == 0) {
			return new TreeSet<GridFile>();
		}

		final Map<String, GridFile> result = Collections
		.synchronizedMap(new HashMap<String, GridFile>());


		final ExecutorService pool = Executors.newFixedThreadPool(mps.size());

		for (final MountPoint mp : mps) {

			final String urlToQuery = mp.getRootUrl() + "/" + path;

			Thread t = new Thread() {
				@Override
				public void run() {

					myLogger.debug("Groupfilesystem list group started: "
							+ mp.getAlias() + " / " + urlToQuery);

					result.put(urlToQuery, null);

					try {
						GridFile file = user.getFileSystemManager()
						.getFolderListing(urlToQuery, 1);
						file.addSite(mp.getSite());
						result.put(urlToQuery, file);

					} catch (RemoteFileSystemException rfse) {
						String msg = rfse.getLocalizedMessage();
						if (!msg.contains("not a folder")) {
							GridFile f = new GridFile(urlToQuery, false, rfse);
							f.addSite(mp.getSite());
							result.put(urlToQuery, f);
						}
					} catch (Exception ex) {
						GridFile f = new GridFile(urlToQuery, false, ex);
						f.addSite(mp.getSite());
						result.put(urlToQuery, f);
					}

					myLogger.debug("Groupfilesystem list group finished: "
							+ mp.getAlias() + " / " + urlToQuery);

				}
			};

			pool.execute(t);
		}

		pool.shutdown();


		int timeout = ServerPropertiesManager.getFileListingTimeOut();
		try {
			boolean timedOut = !pool
			.awaitTermination(timeout, TimeUnit.SECONDS);
			if (timedOut) {
				myLogger.debug("GroupfilePlugin list group timed out....");

				// filling missing files
				for (String url : result.keySet()) {
					if ( result.get(url) == null ) {
						GridFile temp = new GridFile(
								url,
								false,
								new Exception(
										"Timeout ("
										+ timeout
										+ " seconds) while trying to list children."));
						result.put(url, temp);
					}
				}
			}
		} catch (InterruptedException e) {
			throw new RemoteFileSystemException(e);
		}

		return new TreeSet<GridFile>(result.values());

	}

	private Set<String> resolveUrls(String path, String fqan) {

		Set<MountPoint> mps = user.getMountPoints(fqan);

		// remove volatile mountpoints
		Iterator<MountPoint> it = mps.iterator();
		while (it.hasNext()) {
			MountPoint mp = it.next();
			if (mp.isVolatile()) {
				it.remove();
			}
		}

		Set<String> urls = new HashSet<String>();

		for (final MountPoint mp : mps) {
			final String urlToQuery = mp.getRootUrl() + "/" + path;
			urls.add(urlToQuery);

		}

		return urls;
	}
}
