package org.vpac.grisu.frontend.view.swing.files;

import java.util.Set;

import javax.swing.JPanel;

import org.vpac.grisu.model.dto.GridFile;

public interface GridFileListPanel {

	public void addFileListListener(GridFileListListener l);

	public void displayHiddenFiles(boolean display);

	public GridFile getCurrentDirectory();

	public JPanel getPanel();

	public Set<GridFile> getSelectedFiles();

	public void refresh();

	public void removeFileListListener(GridFileListListener l);

	public void setContextMenu(GridFileListPanelContextMenu menu);

	public void setCurrentUrl(String url);

	/**
	 * Sets the extensions to display.
	 * 
	 * @param extensions
	 *            the extensions or null/empty for displaying everything
	 */
	public void setExtensionsToDisplay(String[] extensions);

	public void setRootUrl(String url);

}