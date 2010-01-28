package org.vpac.grisu.frontend.view.swing.files;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.vpac.grisu.frontend.control.fileTransfers.FileTransfer;

public class FileTransferStatusDialog extends JDialog implements PropertyChangeListener {


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			FileTransferStatusDialog dialog = new FileTransferStatusDialog(null);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final JButton backgroundButton;

	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public FileTransferStatusDialog(final FileTransfer ft) {
		ft.addPropertyChangeListener(this);
		setBounds(100, 100, 450, 138);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));

		final FIleTransferStatusPanel fileTransferStatusPanel = new FIleTransferStatusPanel(ft);
		contentPanel.add(fileTransferStatusPanel, BorderLayout.CENTER);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		backgroundButton = new JButton("Transfer in background");
		backgroundButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				ft.removePropertyChangeListener(FileTransferStatusDialog.this);
				ft.removePropertyChangeListener(fileTransferStatusPanel);
				dispose();
			}
		});
		buttonPane.add(backgroundButton);

	}

	public void propertyChange(PropertyChangeEvent evt) {

		if ( "status".equals(evt.getPropertyName()) ) {
			FileTransfer.Status status = (FileTransfer.Status)evt.getNewValue();

			if ( status.equals(FileTransfer.Status.FAILED) || status.equals(FileTransfer.Status.FINISHED) ) {
				backgroundButton.setText("OK");
			}
		}

	}

}