package org.vpac.grisu.frontend.view.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXFrame;
import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.frontend.control.login.LoginManager;
import org.vpac.grisu.frontend.model.events.ApplicationEventListener;
import org.vpac.grisu.frontend.view.swing.jobcreation.JobCreationPanel;
import org.vpac.grisu.frontend.view.swing.login.LoginPanel;
import org.vpac.grisu.frontend.view.swing.login.ServiceInterfaceHolder;

import com.google.common.collect.ImmutableList;

public abstract class GrisuApplicationWindow implements WindowListener,
		ServiceInterfaceHolder {

	private ServiceInterface si;

	private GrisuMainPanel mainPanel;

	private LoginPanel lp;
	protected final GrisuMenu menu;

	private JXFrame frame;

	/**
	 * Launch the application.
	 */
	public GrisuApplicationWindow() {

		LoginManager.initEnvironment();

		new ApplicationEventListener();

		final Toolkit tk = Toolkit.getDefaultToolkit();
		tk.addAWTEventListener(WindowSaver.getInstance(),
				AWTEvent.WINDOW_EVENT_MASK);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
			e.printStackTrace();
		}

		initialize();

		menu = new GrisuMenu(this.getFrame());
		getFrame().setJMenuBar(menu);

	}

	abstract public boolean displayAppSpecificMonitoringItems();

	abstract public boolean displayBatchJobsCreationPane();

	abstract public boolean displaySingleJobsCreationPane();

	private void exit() {
		try {
			System.out.println("Exiting...");

			if (si != null) {
				si.logout();
			}

		} finally {
			WindowSaver.saveSettings();
			System.exit(0);
		}
	}

	public Set<String> getApplicationsToMonitor() {

		Set<String> result = new TreeSet<String>();
		for (JobCreationPanel panel : getJobCreationPanels()) {
			result.add(panel.getSupportedApplication());
		}
		return result;
	}

	public JFrame getFrame() {
		return frame;
	}

	abstract public JobCreationPanel[] getJobCreationPanels();

	abstract public String getName();

	public ServiceInterface getServiceInterface() {
		return si;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JXFrame();
		frame.setTitle(getName());
		frame.addWindowListener(this);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().setLayout(new BorderLayout());

		final boolean singleJobs = displaySingleJobsCreationPane();
		final boolean batchJobs = displayBatchJobsCreationPane();

		final boolean displayAppSpecificMonitoringItems = displayAppSpecificMonitoringItems();

		if (displayAppSpecificMonitoringItems) {
			mainPanel = new GrisuMainPanel(singleJobs, false, true,
					getApplicationsToMonitor(), batchJobs, false, true,
					getApplicationsToMonitor(), true);
		} else {
			mainPanel = new GrisuMainPanel(singleJobs, true, false,
					getApplicationsToMonitor(), batchJobs, true, false,
					getApplicationsToMonitor(), true);
		}

		final List<ServiceInterfaceHolder> siHolders = ImmutableList
				.of((ServiceInterfaceHolder) this);
		final LoginPanel lp = new LoginPanel(mainPanel, siHolders);
		frame.getContentPane().add(lp, BorderLayout.CENTER);
	}

	abstract protected void initOptionalStuff(ServiceInterface si);

	public void refreshJobCreationPanels() {

		mainPanel.removeAlJobCreationPanelsl();
		for (final JobCreationPanel panel : getJobCreationPanels()) {
			mainPanel.addJobCreationPanel(panel);
		}

	}

	public void setServiceInterface(ServiceInterface si) {

		this.si = si;
		initOptionalStuff(si);
		refreshJobCreationPanels();

	}

	public void setServiceInterfaceExternal(ServiceInterface si) {

		if (lp == null) {
			throw new IllegalStateException("LoginPanel not initialized.");
		}

		if (si == null) {
			throw new NullPointerException("ServiceInterface can't be null");
		}
		// this.si = si;
		lp.setServiceInterface(si);

	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowClosing(WindowEvent arg0) {
		exit();
	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

}