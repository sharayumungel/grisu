package org.vpac.grisu.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

/**
 * The concept of MountPoints is pretty important within grisu. A MountPoint is
 * basically a mapping of a "logical name" to an url. Much like mountpoints in a
 * unix filesystem. A logical name should contain the site where the filesystem
 * sits and the VO that has got access to this filesystem so that the user can
 * recognise which one is meant when looking at the name in a file browser.
 * 
 * @author Markus Binsteiner
 * 
 */
@Entity
@XmlRootElement(name="mountpoint")
@XmlAccessorType(XmlAccessType.NONE)
public class MountPoint implements Comparable<MountPoint> {

	static final Logger myLogger = Logger.getLogger(MountPoint.class.getName());

	private Long mountPointId = null;

	/**
	 * The dn of the user.
	 */
	@XmlElement(name="dn")
	private String dn = null;
	/**
	 * The fqan that is used to create a voms credential to access this mountpoint.
	 */
	@XmlElement(name="fqan")
	private String fqan = null;
	/**
	 * The alias of this mountpoint.
	 */
	@XmlAttribute(name="alias")
	private String alias = null;
	/**
	 * The url of the root of this mountpoint.
	 */
	@XmlAttribute(name="url")
	private String rootUrl = null;

	@XmlElement(name="automounted")
	private boolean automaticallyMounted = false;
	@XmlElement(name="disabled")
	private boolean disabled = false;

	// for hibernate
	public MountPoint() {
	}

	/**
	 * Creates a MountPoint. Sets automount property to false.
	 * 
	 * @param dn
	 *            the dn of the user
	 * @param fqan
	 *            the fqan that is used to access this filesystem
	 * @param url
	 *            the root url
	 * @param mountpoint
	 *            the name of the mountpoint
	 */
	public MountPoint(final String dn, final String fqan, final String url, final String mountpoint) {
		this.dn = dn;
		this.fqan = fqan;
		this.rootUrl = url;
		this.alias = mountpoint;
	}

	/**
	 * Creates a Mountpoint.
	 * 
	 * @param dn
	 *            the dn of the user
	 * @param fqan
	 *            the fqan that is used to access this filesystem
	 * @param url
	 *            the root url
	 * @param mountpoint
	 *            the name of the mountpoint
	 * @param automaticallyMounted
	 *            whether this mountpoint was mounted automatically using mds
	 *            information or manually by the user
	 */
	public MountPoint(final String dn, final String fqan, final String url, final String mountpoint,
			final boolean automaticallyMounted) {
		this(dn, fqan, url, mountpoint);
		this.automaticallyMounted = automaticallyMounted;
	}

	/**
	 * This is used primarily to create a "dummy" mountpoint to be able to use
	 * the {@link User#unmountFileSystem(String)} method.
	 * 
	 * @param dn
	 *            the dn of the user
	 * @param mountpoint
	 *            the name of the mountpoint
	 */
	public MountPoint(final String dn, final String mountpoint) {
		this.dn = dn;
		this.alias = mountpoint;
	}

	@Column(nullable = false)
	public String getDn() {
		return dn;
	}

	public void setDn(final String dn) {
		this.dn = dn;
	}

	/**
	 * The fqan that is used to create a voms proxy to access this mountpoint.
	 * 
	 * @return the fqan
	 */
	public String getFqan() {
		return fqan;
	}

	public void setFqan(final String fqan) {
		this.fqan = fqan;
	}

	@Column(nullable = false)
	public String getAlias() {
		return alias;
	}

	public void setAlias(final String mountpoint) {
		this.alias = mountpoint;
	}

	@Column(nullable = false)
	public String getRootUrl() {
		return rootUrl;
	}

	public void setRootUrl(final String rootUrl) {
		this.rootUrl = rootUrl;
	}

	public void setUrl(final String url) {
		this.rootUrl = url;
	}

	@Id
	@GeneratedValue
	public Long getMountPointId() {
		return mountPointId;
	}

	public void setMountPointId(final Long id) {
		this.mountPointId = id;
	}

	// public boolean equals(Object otherMountPoint) {
	// if ( ! (otherMountPoint instanceof MountPoint) )
	// return false;
	// MountPoint other = (MountPoint)otherMountPoint;
	// if ( other.dn.equals(this.dn) && other.mountpoint.equals(this.mountpoint)
	// )
	// return true;
	// else return false;
	// }

	public boolean equals(final Object otherMountPoint) {

		if (otherMountPoint instanceof MountPoint) {
			MountPoint other = (MountPoint) otherMountPoint;

			return other.getAlias().equals(this.getAlias());

			// if ( other.getDn().equals(this.getDn()) &&
			// other.getRootUrl().equals(this.getRootUrl()) ) {
			//
			// if ( other.getFqan() == null ) {
			// if ( this.getFqan() == null ) {
			// return true;
			// } else {
			// return false;
			// }
			// } else {
			// if ( this.getFqan() == null ) {
			// return false;
			// } else {
			// return other.getFqan().equals(this.getFqan());
			// }
			// }
			// } else {
			// return false;
			// }
		} else {
			return false;
		}

	}

	public int hashCode() {
		// return dn.hashCode() + mountpoint.hashCode();
		return alias.hashCode();
	}

	/**
	 * Translates a "mounted" file (on that filesystem to an absolute url like
	 * gsiftp://ngdata.vpac.org/home/san04/markus/test.txt).
	 * 
	 * @param file
	 *            the "mounted" file (e.g. /ngdata.vpac/test.txt
	 * @return the absoulte path of the file or null if the file is not in the
	 *         mounted filesystem or is not a "mounted" file (starts with
	 *         something like /home.sapac.ngadmin)
	 */
	public String replaceMountpointWithAbsoluteUrl(final String file) {

		if (file.startsWith(getAlias())) {
			return file.replaceFirst(getAlias(), getRootUrl());
		} else {
			return null;
		}
	}

	/**
	 * Translates an absolute file url to a "mounted" file url.
	 * 
	 * @param file
	 *            the absolute file
	 *            (gsiftp://ngdata.vpac.org/home/sano4/markus/test.txt)
	 * @return /ngdata.vpac.org/test.txt
	 */
	public String replaceAbsoluteRootUrlWithMountPoint(final String file) {

		if (file.startsWith(getRootUrl())) {
			return file.replaceFirst(getRootUrl(), getAlias());

		} else {
			return null;
		}
	}

	/**
	 * Checks whether the "userspace" url (/ngdata.vpac/file.txt) contains the
	 * file.
	 * 
	 * @param file
	 *            the file
	 * @return true - if it contains it; false - if not.
	 */
	public boolean isResponsibleForUserSpaceFile(final String file) {

		if (file.startsWith("gsiftp")) {
			if (file.startsWith(getRootUrl())) {
				return true;
			} else {
				return false;
			}
		}

		if (file.startsWith(getAlias())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks whether the "userspace" url (/ngdata.vpac/file.txt) contains the
	 * file.
	 * 
	 * @param file
	 *            the file
	 * @return true - if it contains it; false - if not.
	 */
	public boolean isResponsibleForAbsoluteFile(final String file) {

		if (file.startsWith(getRootUrl())) {
			return true;
		} else {
			if (file.startsWith(getRootUrl().replace(":2811", ""))) {
				// warning
				myLogger
						.warn("Found mountpoint. Didn't compare port numbers though...");
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getAlias();
	}

	/**
	 * Returns the path of the specified file relative to the root of this
	 * mountpoint.
	 * 
	 * @param url
	 *            the file
	 * @return the relative path or null if the file is not within the
	 *         filesystem of the mountpoint
	 */
	public String getRelativePathToRoot(final String url) {

		if (url.startsWith("/")) {
			if (!url.startsWith(getAlias())) {
				return null;
			} else {
				String path = url.substring(getAlias().length());
				if (path.startsWith("/")) {
					return path.substring(1);
				} else {
					return path;
				}
			}
		} else {
			if (!url.startsWith(getRootUrl())) {
				return null;
			} else {
				String path = url.substring(getRootUrl().length());
				if (path.startsWith("/")) {
					return path.substring(1);
				} else {
					return path;
				}
			}
		}

	}

	// public int compareTo(Object o) {
	// // return ((MountPoint)o).getMountpoint().compareTo(getMountpoint());
	// return getRootUrl().compareTo(((MountPoint)o).getRootUrl());
	// }

	public int compareTo(final MountPoint mp) {
		return getRootUrl().compareTo(mp.getRootUrl());
	}

	@Column(nullable = false)
	public boolean isAutomaticallyMounted() {
		return automaticallyMounted;
	}

	public void setAutomaticallyMounted(final boolean am) {
		this.automaticallyMounted = am;
	}

	@Column(nullable = false)
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(final boolean disabled) {
		this.disabled = disabled;
	}

}
