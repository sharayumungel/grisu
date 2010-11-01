package org.vpac.grisu.frontend.view.swing.files;

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

import org.vpac.grisu.frontend.control.fileTransfers.FileTransaction;

public class FileTransactionStatusDialog extends JDialog implements
		PropertyChangeListener {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			final FileTransactionStatusDialog dialog = new FileTransactionStatusDialog(
					null, null);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private final JButton backgroundButton;

	private final JPanel contentPanel = new JPanel();
	private final FileTransaction ft;

	final FIleTransferStatusPanel fileTransferStatusPanel;

	/**
	 * Create the dialog.
	 */
	public FileTransactionStatusDialog(Frame owner, final FileTransaction ft) {
		super(owner);

		this.ft = ft;
		ft.addPropertyChangeListener(this);
		setBounds(100, 100, 450, 138);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));

		fileTransferStatusPanel = new FIleTransferStatusPanel(ft);
		contentPanel.add(fileTransferStatusPanel, BorderLayout.CENTER);

		final JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		backgroundButton = new JButton("Transfer in background");
		backgroundButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				try {
					ft.removePropertyChangeListener(FileTransactionStatusDialog.this);
					ft.removePropertyChangeListener(fileTransferStatusPanel);
					dispose();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		buttonPane.add(backgroundButton);

	}

	public void propertyChange(PropertyChangeEvent evt) {

		if ("status".equals(evt.getPropertyName())) {
			final FileTransaction.Status status = (FileTransaction.Status) evt
					.getNewValue();

			if (status.equals(FileTransaction.Status.FAILED)
					|| status.equals(FileTransaction.Status.FINISHED)) {
				backgroundButton.setText("OK");

			}
		}

	}
}