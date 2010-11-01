package org.vpac.grisu.frontend.view.swing.files;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.vpac.grisu.model.files.FileSystemItem;

public class FileSystemItemRenderer extends DefaultListCellRenderer implements
		ListCellRenderer {

	@Override
	public Component getListCellRendererComponent(JList arg0, Object arg1,
			int arg2, boolean arg3, boolean arg4) {

		if (arg1 == null) {
			return this;
		}
		final FileSystemItem item = (FileSystemItem) arg1;

		this.setText(item.getAlias());

		if (item.isDummy()) {
			this.setHorizontalAlignment(SwingConstants.CENTER);
			setEnabled(false);
		} else {
			this.setHorizontalAlignment(SwingConstants.LEFT);
			setEnabled(true);
		}

		if (arg3 && !item.isDummy()) {
			setBackground((Color) UIManager.get("Table.selectionBackground"));
		} else {
			setBackground(arg0.getBackground());
		}

		return this;

	}

}