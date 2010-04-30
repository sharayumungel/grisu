package org.vpac.grisu.frontend.view.swing;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.TemplateManager;
import org.vpac.grisu.control.exceptions.NoSuchTemplateException;
import org.vpac.grisu.frontend.view.swing.jobcreation.JobCreationPanel;
import org.vpac.grisu.frontend.view.swing.jobcreation.TemplateJobCreationPanel;
import org.vpac.grisu.model.GrisuRegistryManager;

public class GrisuTemplateApp extends GrisuApplicationWindow implements
		PropertyChangeListener {

	static final Logger myLogger = Logger.getLogger(GrisuTemplateApp.class
			.getName());

	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {

					GrisuApplicationWindow appWindow = new GrisuTemplateApp();
					appWindow.setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	private final GrisuMenu menu = new GrisuMenu();
	private TemplateManager tm;

	public GrisuTemplateApp() {
		super();
		getFrame().setJMenuBar(menu);
	}

	@Override
	public boolean displayAppSpecificMonitoringItems() {
		return true;
	}

	@Override
	public boolean displayBatchJobsCreationPane() {
		return false;
	}

	@Override
	public boolean displaySingleJobsCreationPane() {
		return true;
	}

	@Override
	public JobCreationPanel[] getJobCreationPanels() {

		if (getServiceInterface() == null) {
			return new JobCreationPanel[] {};
		}

		List<JobCreationPanel> panels = new LinkedList<JobCreationPanel>();

		SortedSet<String> allMyTemplates = tm.getAllTemplateNames();

		for (String name : allMyTemplates) {
			try {
				JobCreationPanel panel = new TemplateJobCreationPanel(name, tm
						.getTemplate(name));
				if (panel == null) {
					myLogger.warn("Can't find template " + name);
					continue;
				}
				panel.setServiceInterface(getServiceInterface());
				panels.add(panel);
			} catch (NoSuchTemplateException e) {
				myLogger.warn("Can't find template " + name);
				continue;
			}
		}

		return panels.toArray(new JobCreationPanel[] {});
	}

	@Override
	public String getName() {
		return "Default Grisu client";
	}

	@Override
	public void initOptionalStuff(ServiceInterface si) {

		menu.setServiceInterface(si);
		tm = GrisuRegistryManager.getDefault(si).getTemplateManager();
		tm.addTemplateManagerListener(this);
	}

	public void propertyChange(PropertyChangeEvent evt) {

		if (getServiceInterface() == null) {
			myLogger.info("No serviceInterface. Not updateing template list.");
			return;
		}

		refreshJobCreationPanels();

	}

}