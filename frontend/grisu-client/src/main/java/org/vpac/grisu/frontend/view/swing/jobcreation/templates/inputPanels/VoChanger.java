package org.vpac.grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.vpac.grisu.control.ServiceInterface;
import org.vpac.grisu.control.exceptions.TemplateException;
import org.vpac.grisu.frontend.view.swing.DefaultFqanChangePanel;
import org.vpac.grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import org.vpac.grisu.model.job.JobSubmissionObjectImpl;

public class VoChanger extends AbstractInputPanel {

	private DefaultFqanChangePanel fqanChangePanel = null;

	public VoChanger(String templateName, PanelConfig config)
			throws TemplateException {
		super(templateName, config);

		setLayout(new BorderLayout());
	}

	public DefaultFqanChangePanel getFqanChangePanel() {

		if (fqanChangePanel == null) {
			fqanChangePanel = new DefaultFqanChangePanel();
			try {
				fqanChangePanel.setServiceInterface(getServiceInterface());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return fqanChangePanel;
	}

	@Override
	protected Map<String, String> getDefaultPanelProperties() {

		Map<String, String> defaultProperties = new HashMap<String, String>();

		return defaultProperties;

	}

	@Override
	public void setServiceInterface(ServiceInterface si) {
		super.setServiceInterface(si);
		add(getFqanChangePanel(), BorderLayout.CENTER);
	}

	@Override
	public JComboBox getJComboBox() {
		return null;
	}

	@Override
	public JTextComponent getTextComponent() {
		return null;
	}

	@Override
	protected String getValueAsString() {
		return null;
	}

	@Override
	protected void jobPropertyChanged(PropertyChangeEvent e) {

	}

	@Override
	protected void preparePanel(Map<String, String> panelProperties)
			throws TemplateException {

	}

	@Override
	void setInitialValue() throws TemplateException {

	}

	@Override
	protected void templateRefresh(JobSubmissionObjectImpl jobObject) {

	}

}