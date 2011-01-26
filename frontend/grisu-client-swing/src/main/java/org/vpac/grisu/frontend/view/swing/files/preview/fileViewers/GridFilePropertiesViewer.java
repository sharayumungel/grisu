package org.vpac.grisu.frontend.view.swing.files.preview.fileViewers;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.vpac.grisu.frontend.view.swing.files.contextMenu.GridFilePropertiesDialog;
import org.vpac.grisu.frontend.view.swing.files.preview.GridFileViewer;
import org.vpac.grisu.model.dto.GridFile;

public class GridFilePropertiesViewer extends JPanel implements GridFileViewer {
	private JEditorPane editorPane;
	private JScrollPane scrollPane;

	/**
	 * Create the panel.
	 */
	public GridFilePropertiesViewer() {
		setLayout(new BorderLayout(0, 0));
		add(getScrollPane(), BorderLayout.CENTER);
	}

	private JEditorPane getEditorPane() {
		if (editorPane == null) {
			editorPane = new JEditorPane();
			editorPane.setContentType("text/html");
			editorPane.setEditable(false);
		}
		return editorPane;
	}

	public JPanel getPanel() {
		return this;
	}

	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setViewportView(getEditorPane());
		}
		return scrollPane;
	}

	public String[] getSupportedMimeTypes() {
		return null;
	}

	public void setFile(GridFile file, File localCacheFile) {
		if (file == null) {
			return;
		}
		String text = GridFilePropertiesDialog.generateHtml(file);
		editorPane.setText(text);
		setVisible(true);
	}
}
