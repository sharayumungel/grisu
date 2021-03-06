package grisu.frontend.view.swing.files;

import grisu.frontend.control.fileTransfers.FileTransaction;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransactionStatusDialog extends JDialog implements
		PropertyChangeListener {

	static final Logger myLogger = LoggerFactory
			.getLogger(FileTransactionStatusDialog.class.getName());

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			final FileTransactionStatusDialog dialog = new FileTransactionStatusDialog(
					null);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private JButton backgroundButton;

	private final JPanel contentPanel = new JPanel();
	private FileTransaction ft;

	final FileTransferStatusPanel fileTransferStatusPanel;

	/**
	 * Create the dialog.
	 */
	public FileTransactionStatusDialog(Frame owner) {
		super(owner);

		setBounds(100, 100, 450, 138);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));

		fileTransferStatusPanel = new FileTransferStatusPanel();
		contentPanel.add(fileTransferStatusPanel, BorderLayout.CENTER);

		final JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		buttonPane.add(getBackgroundButton());

	}

	public synchronized JButton getBackgroundButton() {
		if (backgroundButton == null) {
			backgroundButton = new JButton("Transfer in background");
			backgroundButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {

					try {
						ft.removePropertyChangeListener(FileTransactionStatusDialog.this);
						ft.removePropertyChangeListener(fileTransferStatusPanel);
						dispose();
					} catch (final Exception e1) {
						myLogger.error(e1.getLocalizedMessage(), e1);
					}
				}
			});
		}
		return backgroundButton;
	}

	public void propertyChange(PropertyChangeEvent evt) {

		if ("status".equals(evt.getPropertyName())) {
			final FileTransaction.Status status = (FileTransaction.Status) evt
					.getNewValue();

			if (status.equals(FileTransaction.Status.FAILED)
					|| status.equals(FileTransaction.Status.FINISHED)) {
				getBackgroundButton().setText("OK");

			}
		}

	}

	public void setFileTransaction(FileTransaction ft) {
		this.ft = ft;
		fileTransferStatusPanel.setFileTransaction(ft);
		ft.addPropertyChangeListener(this);
	}
}
