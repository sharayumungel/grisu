package grisu.frontend.view.swing.files;

import grisu.control.ServiceInterface;
import grisu.frontend.control.fileTransfers.FileTransaction;
import grisu.frontend.control.fileTransfers.FileTransactionManager;
import grisu.frontend.view.swing.files.virtual.GridFileTreeNode;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;
import grisu.model.dto.GridFile;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;

import org.netbeans.swing.outline.Outline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridFileTransferHandler extends TransferHandler {

	static final Logger myLogger = LoggerFactory
			.getLogger(GridFileTransferHandler.class);

	public static final String LOCAL_SET_TYPE = DataFlavor.javaJVMLocalObjectMimeType
			+ ";class=java.util.Set";
	{
		try {
			SET_DATA_FLAVOR = new DataFlavor(LOCAL_SET_TYPE);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

	}
	public static DataFlavor SET_DATA_FLAVOR;

	private final ServiceInterface si;
	private final FileTransactionManager ftm;
	private final FileManager fm;
	private final GridFileListPanel fileList;
	private final boolean enableDrops;

	public GridFileTransferHandler(GridFileListPanel fileList,
			ServiceInterface si, boolean enableDrops) {
		this.si = si;
		this.enableDrops = enableDrops;
		this.fm = GrisuRegistryManager.getDefault(si).getFileManager();
		this.ftm = FileTransactionManager.getDefault(si);
		this.fileList = fileList;
	}

	// @Override
	// public boolean canImport(JComponent c, DataFlavor[] flavors) {
	//
	// if (c instanceof Outline) {
	//
	// Outline outline = (Outline) c;
	// DropLocation loc = outline.getDropLocation();
	// outline.getdro
	// }
	//
	// return true;
	// }

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {

		if (!enableDrops) {
			return false;
		}

		// Outline outline = (Outline) (support.getComponent());

		// X.p("Loc: " + outline.get
		// JTable.DropLocation dropLocation = (JTable.DropLocation) support
		// .getDropLocation();
		// int row = dropLocation.getRow();
		// int col = dropLocation.getColumn();
		// X.p("Row: " + row + ", Col: " + col);
		return true;

	}

	@Override
	protected Transferable createTransferable(JComponent c) {

		if (c instanceof Outline) {

			final Outline table = (Outline) c;

			final Set<GridFile> selected = new TreeSet<GridFile>();

			for (final int r : table.getSelectedRows()) {

				if (r >= 0) {
					final GridFile sel = (GridFile) ((GridFileTreeNode) (table
							.getValueAt(r, 0))).getUserObject();
					if (!sel.isVirtual()) {
						selected.add(sel);
					} else {
						return null;
					}

				}

			}

			final GridFilesTransferable temp = new GridFilesTransferable(
					selected);
			// for (DataFlavor f : temp.getTransferDataFlavors()) {
			// X.p(f.getHumanPresentableName());
			// }

			return temp;
		}

		throw new RuntimeException("Container of class"
				+ c.getClass().toString() + " not supported.");
	}

	@Override
	protected void exportDone(JComponent c, Transferable data, int action) {
		// TODO ?
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY;
	}

	@Override
	public boolean importData(JComponent c, Transferable t) {
		if (canImport(c, t.getTransferDataFlavors())) {
			try {
				importGridFilesSet((Set<GridFile>) t
						.getTransferData(SET_DATA_FLAVOR));
				return true;
			} catch (final UnsupportedFlavorException ufe) {
				myLogger.error(ufe.getLocalizedMessage(), ufe);
			} catch (final IOException ioe) {
				myLogger.error(ioe.getLocalizedMessage(), ioe);
			}
		}

		return false;
	}

	protected void importGridFilesSet(Set<GridFile> gridFiles) {

		final FileTransaction ft = new FileTransaction(fm, gridFiles,
				fileList.getCurrentDirectory(), true);

		SwingUtilities.invokeLater(new Thread() {

			@Override
			public void run() {
				final JFrame frame = (JFrame) SwingUtilities.getRoot(fileList
						.getPanel());
				final FileTransactionStatusDialog ftd = new FileTransactionStatusDialog(
						frame);
				ftd.setFileTransaction(ft);
				ftd.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				ftd.setVisible(true);
				ftm.addFileTransfer(ft);
			}

		});

	}

}
