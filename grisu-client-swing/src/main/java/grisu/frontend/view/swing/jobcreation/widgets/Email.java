package grisu.frontend.view.swing.jobcreation.widgets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class Email extends AbstractWidget {
	private JCheckBox checkBox;
	private JCheckBox checkBox_1;
	private JLabel label;
	private JTextField textField;

	public static final String EMAIL_HISTORY_KEY_GENERIC = "send_user_email";

	public Email() {
		super();
		setBorder(new TitledBorder(null, "Send email when job is...",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(33dlu;default)"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getCheckBox(), "2, 2, 3, 1");
		add(getCheckBox_1(), "6, 2");
		add(getLabel(), "2, 4, right, default");
		add(getTextField(), "4, 4, 3, 1, fill, default");

		setHistoryKey(EMAIL_HISTORY_KEY_GENERIC);
	}

	private JCheckBox getCheckBox() {
		if (checkBox == null) {
			checkBox = new JCheckBox("...started");
		}
		return checkBox;
	}

	private JCheckBox getCheckBox_1() {
		if (checkBox_1 == null) {
			checkBox_1 = new JCheckBox("...finished");
		}
		return checkBox_1;
	}

	public String getEmailAddress() {
		return getTextField().getText();
	}

	private JLabel getLabel() {
		if (label == null) {
			label = new JLabel("Email:");
		}
		return label;
	}

	private JTextField getTextField() {
		if (textField == null) {
			textField = new JTextField();
			textField.setColumns(10);
		}
		return textField;
	}

	@Override
	public String getValue() {
		String emailAddress = getEmailAddress();
		if (StringUtils.isBlank(emailAddress)) {
			emailAddress = "null";
		}
		final String value = getCheckBox().isSelected() + ","
				+ getCheckBox_1().isSelected() + "," + emailAddress;
		return value;
	}

	public boolean sendEmailWhenJobFinished() {
		return getCheckBox_1().isSelected();
	}

	public boolean sendEmailWhenJobIsStarted() {
		return getCheckBox().isSelected();
	}

	@Override
	public void setValue(String value) {

		if (StringUtils.isBlank(value)) {
			return;
		}

		try {
			final String[] string = value.split(",");
			final boolean onStart = Boolean.parseBoolean(string[0]);
			final boolean onFinish = Boolean.parseBoolean(string[1]);
			String emailAddress = string[2];
			if (emailAddress == null || "null".equals(emailAddress)) {
				emailAddress = "";
			}
			getCheckBox().setSelected(onStart);
			getCheckBox_1().setSelected(onFinish);
			getTextField().setText(emailAddress);

		} catch (final Exception e) {
			return;
		}
	}
}
