package org.vpac.grisu.control.serviceInterfaces;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.globus.myproxy.CredentialInfo;
import org.globus.myproxy.MyProxy;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSException;
import org.vpac.grisu.backend.hibernate.HibernateSessionFactory;
import org.vpac.grisu.backend.model.ProxyCredential;
import org.vpac.grisu.backend.utils.LocalTemplatesHelper;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.NoSuchTemplateException;
import org.vpac.grisu.control.exceptions.NoValidCredentialException;
import org.vpac.grisu.settings.MyProxyServerParams;
import org.vpac.grisu.settings.ServerPropertiesManager;
import org.vpac.grisu.settings.ServiceTemplateManagement;
import org.vpac.grisu.utils.SeveralXMLHelpers;
import org.vpac.security.light.control.CertificateFiles;
import org.vpac.security.light.control.VomsesFiles;
import org.vpac.security.light.myProxy.MyProxy_light;
import org.vpac.security.light.plainProxy.LocalProxy;
import org.w3c.dom.Document;

public class LocalServiceInterface extends AbstractServiceInterface implements
		ServiceInterface {

	private ProxyCredential credential = null;
	private String myproxy_username = null;
	private char[] passphrase = null;
	
	// for package auto-download
	public Integer getPackageVersion() {
		return 2;
	}

	@Override
	protected final ProxyCredential getCredential() {

		long oldLifetime = -1;
		try {
			if (credential != null) {
				oldLifetime = credential.getGssCredential()
						.getRemainingLifetime();
			}
		} catch (GSSException e2) {
			myLogger
					.debug("Problem getting lifetime of old certificate: " + e2);
			credential = null;
		}
		if (oldLifetime < ServerPropertiesManager
				.getMinProxyLifetimeBeforeGettingNewProxy()) {
			myLogger
					.debug("Credential reached minimum lifetime. Getting new one from myproxy. Old lifetime: "
							+ oldLifetime);
			this.credential = null;
			// user.cleanCache();
		}

		if (credential == null || !credential.isValid()) {

			if (myproxy_username == null || myproxy_username.length() == 0) {
				if (passphrase == null || passphrase.length == 0) {
					// try local proxy
					try {
						credential = new ProxyCredential(LocalProxy
								.loadGSSCredential());
					} catch (Exception e) {
						throw new NoValidCredentialException(
								"Could not load credential/no valid login data.");
					}
					if (!credential.isValid()) {
						throw new NoValidCredentialException(
								"Local proxy is not valid anymore.");
					}
				}
			} else {
				// get credential from myproxy
				String myProxyServer = MyProxyServerParams.getMyProxyServer();
				int myProxyPort = MyProxyServerParams.getMyProxyPort();

				try {
					// this is needed because of a possible round-robin myproxy
					// server
					myProxyServer = InetAddress.getByName(myProxyServer)
							.getHostAddress();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					throw new NoValidCredentialException(
							"Could not download myproxy credential: "
									+ e1.getLocalizedMessage());
				}

				try {
					credential = new ProxyCredential(MyProxy_light
							.getDelegation(myProxyServer, myProxyPort,
									myproxy_username, passphrase, 3600));
					if (getUser() != null) {
						getUser().cleanCache();
					}
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new NoValidCredentialException(
							"Could not get myproxy credential: "
									+ e.getLocalizedMessage());
				}
				if (!credential.isValid()) {
					throw new NoValidCredentialException(
							"MyProxy credential is not valid.");
				}
			}
		}

		return credential;

	}

	public final String getTemplate(final String application)
			throws NoSuchTemplateException {
		Document doc = ServiceTemplateManagement
				.getAvailableTemplate(application);

		if (doc == null) {
			throw new NoSuchTemplateException(
					"Could not find template for application: " + application
							+ ".");
		}

		return SeveralXMLHelpers.toString(doc);
	}

	public final Document getTemplate(final String application, final String version)
			throws NoSuchTemplateException {
		Document doc = ServiceTemplateManagement
				.getAvailableTemplate(application);

		if (doc == null) {
			throw new NoSuchTemplateException(
					"Could not find template for application: " + application
							+ ", version " + version);
		}

		return doc;

	}

	public final String[] listHostedApplicationTemplates() {
		return ServiceTemplateManagement.getAllAvailableApplications();
	}

	public final void login(final String username, final String password) {

		try {
			LocalTemplatesHelper.copyTemplatesAndMaybeGlobusFolder();
			VomsesFiles.copyVomses();
			CertificateFiles.copyCACerts();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			myLogger.debug(e.getLocalizedMessage());
			// throw new
			// RuntimeException("Could not initiate local backend: "+e.getLocalizedMessage());
		}

		this.myproxy_username = username;
		this.passphrase = password.toCharArray();

		try {
			// init database and make sure everything is all right
			HibernateSessionFactory.getSessionFactory();
		} catch (Throwable e) {
			throw new RuntimeException("Could not initialize database.", e);
		}

		try {
			getCredential();
		} catch (Exception e) {
//			e.printStackTrace();
			throw new NoValidCredentialException("No valid credential: "
					+ e.getLocalizedMessage());
		}
		
	}

	public final String logout() {
		Arrays.fill(passphrase, 'x');
		return null;
	}

	public final long getCredentialEndTime() {

		String myProxyServer = MyProxyServerParams.getMyProxyServer();
		int myProxyPort = MyProxyServerParams.getMyProxyPort();

		try {
			// this is needed because of a possible round-robin myproxy server
			myProxyServer = InetAddress.getByName(myProxyServer)
					.getHostAddress();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new NoValidCredentialException(
					"Could not download myproxy credential: "
							+ e1.getLocalizedMessage());
		}

		MyProxy myproxy = new MyProxy(myProxyServer, myProxyPort);
		CredentialInfo info = null;
		try {
			info = myproxy.info(getCredential().getGssCredential(),
					myproxy_username, new String(passphrase));
		} catch (MyProxyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return info.getEndTime();

	}

}
