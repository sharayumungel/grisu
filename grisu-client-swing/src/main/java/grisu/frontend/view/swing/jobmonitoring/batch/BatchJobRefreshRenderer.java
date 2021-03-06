package grisu.frontend.view.swing.jobmonitoring.batch;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import furbelow.SpinningDial;

public class BatchJobRefreshRenderer extends DefaultTableCellRenderer implements
		TableCellRenderer {

	private static SpinningDial loading = new SpinningDial(16, 16);

	private static File findFile() {
		for (final File file : new File(System.getProperty("user.home"))
				.listFiles()) {
			if (file.isFile()) {
				return file;
			}
		}
		return null;
	}

	public BatchJobRefreshRenderer() {
		setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		if (isSelected) {
			setBackground((Color) UIManager.get("Table.selectionBackground"));
		} else {
			setBackground(table.getBackground());
		}

		this.setSize(24, 24);

		final Boolean isRefreshing = (Boolean) value;
		if (isRefreshing) {
			this.setIcon(loading);
		} else {
			this.setIcon(null);
		}

		return this;
	}

}
