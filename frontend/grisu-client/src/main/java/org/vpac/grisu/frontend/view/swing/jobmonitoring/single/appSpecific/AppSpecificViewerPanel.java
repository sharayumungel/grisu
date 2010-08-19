package org.vpac.grisu.frontend.view.swing.jobmonitoring.single.appSpecific;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;

import javax.swing.JPanel;

import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.frontend.model.job.JobObject;
import org.vpac.grisu.frontend.view.swing.jobmonitoring.single.JobDetailPanel;
import org.vpac.grisu.model.FileManager;
import org.vpac.grisu.model.GrisuRegistryManager;

import au.org.arcs.jcommons.constants.Constants;

public abstract class AppSpecificViewerPanel extends JPanel implements
		JobDetailPanel, PropertyChangeListener {

	public static AppSpecificViewerPanel create(ServiceInterface si,
			JobObject job) {

		try {
			String appName = job.getJobProperty(Constants.APPLICATIONNAME_KEY);

			String className = "org.vpac.grisu.frontend.view.swing.jobmonitoring.single.appSpecific."
					+ appName;
			Class classO = Class.forName(className);

			Constructor<AppSpecificViewerPanel> constO = classO
					.getConstructor(ServiceInterface.class);

			AppSpecificViewerPanel asvp = constO.newInstance(si);

			return asvp;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private JobObject job = null;
	protected final ServiceInterface si;
	protected final FileManager fm;

	public AppSpecificViewerPanel(ServiceInterface si) {
		super();
		this.si = si;
		if (si != null) {
			this.fm = GrisuRegistryManager.getDefault(si).getFileManager();
		} else {
			this.fm = null;
		}
	}

	abstract public void initialize();

	abstract public void jobUpdated(PropertyChangeEvent evt);

	public void setJob(JobObject job) {
		if (this.job != null) {
			this.job.removePropertyChangeListener(this);
		}
		this.job = job;
		this.job.addPropertyChangeListener(this);

		try {
			initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JPanel getPanel() {
		return this;
	}

	public JobObject getJob() {
		return this.job;
	}

	public void propertyChange(PropertyChangeEvent evt) {

		System.out.println("Job property change.");
		System.out.print(evt.getPropertyName() + ": ");
		System.out.println(evt.getNewValue());

		jobUpdated(evt);

	}

}
