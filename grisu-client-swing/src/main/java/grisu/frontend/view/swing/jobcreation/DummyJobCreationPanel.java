package grisu.frontend.view.swing.jobcreation;

import grisu.control.JobConstants;
import grisu.control.ServiceInterface;
import grisu.frontend.model.events.JobStatusEvent;
import grisu.frontend.model.job.GrisuJob;
import grisu.model.GrisuRegistryManager;
import grisu.model.UserEnvironmentManager;
import grisu.model.status.ActionStatusEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventTopicSubscriber;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class DummyJobCreationPanel extends JPanel implements JobCreationPanel,
		EventTopicSubscriber {

	private JLabel lblDummyJobSubmission;
	private JButton btnSubmit;
	private JScrollPane scrollPane;
	private JTextArea textArea;

	private String currentJobname = null;

	private ServiceInterface si;
	private UserEnvironmentManager em;

	/**
	 * Create the panel.
	 */
	public DummyJobCreationPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getLblDummyJobSubmission(), "2, 2");
		add(getBtnSubmit(), "2, 5, right, default");
		add(getScrollPane(), "2, 7, fill, fill");

	}

	public void addMessage(final String message) {
		SwingUtilities.invokeLater(new Thread() {

			@Override
			public void run() {
				getStatusTextArea().append(message + "\n");
				getStatusTextArea().setCaretPosition(
						getStatusTextArea().getText().length());
			}

		});
	}

	public boolean createsBatchJob() {
		return false;
	}

	public boolean createsSingleJob() {
		return true;
	}

	private JButton getBtnSubmit() {
		if (btnSubmit == null) {
			btnSubmit = new JButton("Submit");
			btnSubmit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					submitJob();
				}
			});
		}
		return btnSubmit;
	}

	private JLabel getLblDummyJobSubmission() {
		if (lblDummyJobSubmission == null) {
			lblDummyJobSubmission = new JLabel("Dummy job submission");
		}
		return lblDummyJobSubmission;
	}

	public JPanel getPanel() {
		return this;
	}

	public String getPanelName() {
		return "Dummy";
	}

	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setViewportView(getStatusTextArea());
		}
		return scrollPane;
	}

	private JTextArea getStatusTextArea() {
		if (textArea == null) {
			textArea = new JTextArea();
		}
		return textArea;
	}

	public String getSupportedApplication() {
		return "UnixCommands";
	}

	private void lockUI(final boolean lock) {

		SwingUtilities.invokeLater(new Thread() {

			@Override
			public void run() {
				getBtnSubmit().setEnabled(!lock);
			}

		});

	}

	public void onEvent(String topic, Object data) {

		if (data instanceof JobStatusEvent) {

			final String message = "New status: "
					+ JobConstants.translateStatus(((JobStatusEvent) data)
							.getNewStatus());

			addMessage(message);
		} else if (data instanceof ActionStatusEvent) {
			final ActionStatusEvent d = ((ActionStatusEvent) data);
			addMessage(d.getPrefix() + d.getPercentFinished() + "% finished.\n");
		}

	}

	public void setServiceInterface(ServiceInterface si) {
		// System.out.println("Serviceinterface set. DN: " + si.getDN());
		this.si = si;
		this.em = GrisuRegistryManager.getDefault(si)
				.getUserEnvironmentManager();
	}

	private void submitJob() {

		new Thread() {
			@Override
			public void run() {
				try {

					lockUI(true);

					if (StringUtils.isNotBlank(currentJobname)) {
						EventBus.unsubscribe(currentJobname, this);
					}

					final GrisuJob job = new GrisuJob(si);
					job.setTimestampJobname("helloworldJob");

					currentJobname = job.getJobname();
					EventBus.subscribe(currentJobname,
							DummyJobCreationPanel.this);

					job.setApplication("UnixCommands");
					job.setCommandline("echo hello gridworld!");

					job.setWalltimeInSeconds(60);

					// this will only work if you are in the StartUp VO.
					job.createJob("/ARCS/StartUp");
					// job.createJob(em.getCurrentFqan());

					job.submitJob();

				} catch (final Exception e) {
					e.printStackTrace();
					addMessage(e.getLocalizedMessage());
				} finally {
					lockUI(false);
				}

			}
		}.start();
	}
}
