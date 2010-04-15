package org.vpac.grisu.frontend.view.swing.jobcreation.inputPanels;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;


public class Cpus extends AbstractInputPanel {
	private JComboBox comboBox;

	private boolean userInput = true;

	public Cpus(Map<String, String>panelProperties) {

		super(panelProperties);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,},
				new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,}));
		add(getComboBox(), "2, 2, 3, 1, fill, default");
	}

	private JComboBox getComboBox() {
		if (comboBox == null) {
			comboBox = new JComboBox();
			comboBox.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {

					if ( ! userInput ) {
						return;
					}

					if ( ItemEvent.SELECTED == e.getStateChange() ) {
						Integer value = (Integer)getComboBox().getSelectedItem();
						jobObject.setCpus(value);
					}

				}
			});
		}
		return comboBox;
	}

	@Override
	protected Map<String, String> getDefaultPanelProperties() {

		Map<String, String> defaultProperties = new HashMap<String, String>();

		defaultProperties.put(TITLE, "CPUS");
		defaultProperties.put(DEFAULT_VALUE, "1");
		defaultProperties.put(PREFILLS, "1,2,4,8,16,32");

		return defaultProperties;
	}

	@Override
	protected void jobPropertyChanged(PropertyChangeEvent e) {

		userInput = false;

		if ( "cpus".equals(e.getPropertyName()) ) {
			int value = (Integer)e.getNewValue();
			getComboBox().setSelectedItem(value);
		}

		userInput = true;
	}


	@Override
	protected void preparePanel(Map<String, String> panelProperties) {

		for ( String key : panelProperties.keySet() ) {
			try {
				if ( DEFAULT_VALUE.equals(key) ) {
					jobObject.setCpus(Integer.parseInt(panelProperties.get(DEFAULT_VALUE)));
				} else if (PREFILLS.equals(key)) {
					for ( String item : panelProperties.get(PREFILLS).split(",") ) {
						getComboBox().addItem(Integer.parseInt(item));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}


}
