package grisu.frontend.control;

import grisu.control.ServiceInterface;
import grisu.control.ServiceInterfaceCreator;
import grisu.control.exceptions.ServiceInterfaceException;

import org.apache.log4j.Logger;

/**
 * A serviceInterfaceCreator that creates a serviceinterface that uses a plain
 * java object (LocalServiceInterface). This is the most simple grisu backend
 * one can imagine. All you need is to have the grisu-core module in your
 * classpath and you need to use the string "Local" as serviceinterfaceUrl.
 * 
 * @author Markus Binsteiner
 */
public class DummyServiceInterfaceCreator implements ServiceInterfaceCreator {

	static final Logger myLogger = Logger
			.getLogger(DummyServiceInterfaceCreator.class.getName());

	static final String DEFAULT_LOCAL_URL = "Dummy";

	public final boolean canHandleUrl(final String url) {

		return DEFAULT_LOCAL_URL.equals(url);
	}

	public final ServiceInterface create(final String interfaceUrl,
			final String username, final char[] password,
			final String myProxyServer, final String myProxyPort,
			final Object[] otherOptions) throws ServiceInterfaceException {

		Class localServiceInterfaceClass = null;

		try {
			localServiceInterfaceClass = Class
					.forName("grisu.control.serviceInterfaces.DummyServiceInterface");
		} catch (final ClassNotFoundException e) {
			myLogger.warn("Could not find local service interface class.");
			throw new ServiceInterfaceException(
					"Could not find DummyServiceInterface class. Probably grisu-local-backend.jar is not in the classpath.",
					e);
		}

		ServiceInterface localServiceInterface;
		try {
			localServiceInterface = (ServiceInterface) localServiceInterfaceClass
					.newInstance();
		} catch (final Exception e) {
			throw new ServiceInterfaceException(
					"Could not create LocalServiceInterface: "
							+ e.getLocalizedMessage(), e);
		}

		return localServiceInterface;

	}

}
