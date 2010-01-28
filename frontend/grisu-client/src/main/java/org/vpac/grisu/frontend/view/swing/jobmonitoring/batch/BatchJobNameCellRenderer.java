package org.vpac.grisu.frontend.view.swing.jobmonitoring.batch;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.vpac.grisu.frontend.model.job.BatchJobObject;

public class BatchJobNameCellRenderer extends DefaultTableCellRenderer implements
TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		if (isSelected) {
			setBackground((Color) UIManager.get("Table.selectionBackground"));
		} else {
			setBackground(table.getBackground());
		}

		BatchJobObject bj = (BatchJobObject)value;

		if ( bj.isBeingKilled() ) {
			Component c = super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, column);
			c.setEnabled(false);

			setText(bj.getJobname()+" (being killed at the moment)");
		} else {
			Component c = super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, column);
			c.setEnabled(true);
			setText(bj.getJobname());
		}

		return this;

	}



}