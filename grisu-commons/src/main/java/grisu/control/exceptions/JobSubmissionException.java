package grisu.control.exceptions;

import javax.xml.ws.WebFault;

@WebFault(faultBean = "grisu.control.jaxws.exceptions.JobSubmissionException")
public class JobSubmissionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JobSubmissionException(final String message) {
		super(message);
	}

	public JobSubmissionException(final String message, final Throwable e) {
		super(message, e);
	}

}
