package grisu.frontend.view.swing.files.virtual.utils;

import grisu.control.ServiceInterface;
import grisu.control.exceptions.RemoteFileSystemException;
import grisu.frontend.view.swing.files.GridFileListListener;
import grisu.frontend.view.swing.files.GridFileListPanel;
import grisu.frontend.view.swing.files.contextMenu.GridFileListPanelContextMenu;
import grisu.frontend.view.swing.files.virtual.GridFileTreeNode;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;
import grisu.model.UserEnvironmentManager;
import grisu.model.dto.GridFile;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class VirtualFileSystemTreePanel extends JPanel implements
GridFileListPanel {

	static final Logger myLogger = LoggerFactory
			.getLogger(VirtualFileSystemTreePanel.class.getName());

	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}

			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}

	private final ServiceInterface si;
	private final UserEnvironmentManager uem;
	private final FileManager fm;
	private JScrollPane scrollPane;
	private JTree tree;

	private Vector<GridFileListListener> listeners;
	private GridFileListPanelContextMenu popupMenu;

	private final boolean displayFiles = true;
	private final boolean displayHiddenFiles = false;
	private final String[] extensionsToDisplay = null;

	/**
	 * Create the panel.
	 */
	public VirtualFileSystemTreePanel(ServiceInterface si) {
		this.si = si;
		this.fm = GrisuRegistryManager.getDefault(si).getFileManager();
		this.uem = GrisuRegistryManager.getDefault(si)
				.getUserEnvironmentManager();
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getScrollPane(), "2, 2, fill, fill");

		initialize();
	}

	synchronized public void addGridFileListListener(GridFileListListener l) {
		if (listeners == null) {
			listeners = new Vector<GridFileListListener>();
		}
		listeners.addElement(l);
	}

	@Override
	public void finalize() {
		// ToolTipManager.sharedInstance().unregisterComponent(tree);
	}

	public GridFile getCurrentDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

	public JPanel getPanel() {
		return this;
	}

	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setViewportView(getTree());

		}
		return scrollPane;
	}

	public Set<GridFile> getSelectedFiles() {

		// getTree().geti
		// TODO Auto-generated method stub
		return null;
	}

	public ServiceInterface getServiceInterface() {
		return si;
	}

	// private void fileDoubleClickOccured() {
	//
	// final int selRow = table.getSelectedRow();
	// if (selRow >= 0) {
	//
	// final GridFile sel = (GridFile) fileModel.getValueAt(selRow, 0);
	//
	// if (sel.isFolder()) {
	// fireFilesSelected(null);
	// setCurrent(sel);
	// } else {
	// fireFileDoubleClicked(sel);
	// }
	//
	// }
	//
	// }

	private JTree getTree() {
		if (tree == null) {
			tree = new JTree();
			ToolTipManager.sharedInstance().registerComponent(tree);
			tree.setCellRenderer(new VirtualFileSystemBrowserTreeRenderer(si));
			tree.setRootVisible(true);
			tree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {

					if (arg0.getClickCount() == 2) {
						// fileDoubleClickOccured();
					} else if (arg0.getClickCount() == 1) {
						// fileClickOccured();
					}

				}
			});
		}
		return tree;
	}

	// private JXTreeTable getTreeTable() {
	// if (treeTable == null) {
	// // TreeTableModel m = new UserspaceFileTreeTableModel(si,
	// // "/ARCS/BeSTGRID");
	// treeTable = new JXTreeTable();
	// }
	// return treeTable;
	// }

	private void initialize() {

		final GridFile root = new GridFile(
				ServiceInterface.VIRTUAL_GRID_PROTOCOL_NAME + "://groups", -1L);
		final GridFileTreeNode rootNode = new GridFileTreeNode(fm, root);

		final DefaultTreeModel model = new DefaultTreeModel(rootNode);
		rootNode.setModel(model);

		final LazyLoadingTreeController controller = new LazyLoadingTreeController(
				getTree());

		try {
			for (final GridFile f : fm.ls(root).getChildren()) {
				rootNode.add(new GridFileTreeNode(fm, f, controller,
						displayFiles,
						displayHiddenFiles, extensionsToDisplay));
			}
		} catch (final RemoteFileSystemException e) {
			myLogger.error(e.getLocalizedMessage(), e);
			return;
		}

		getTree().setModel(model);
		getTree().addTreeWillExpandListener(controller);

	}

	public void refresh() {
		// TODO Auto-generated method stub

	}

	public void refreshFolder(String url) {
		// TODO Auto-generated method stub

	}

	synchronized public void removeGridFileListListener(GridFileListListener l) {
		if (listeners == null) {
			listeners = new Vector<GridFileListListener>();
		}
		listeners.removeElement(l);
	}

	public void setContextMenu(GridFileListPanelContextMenu menu) {
		if (this.popupMenu != null) {
			removeGridFileListListener(this.popupMenu);
		}
		this.popupMenu = menu;
		// menu.setFileListPanel(this);
		addGridFileListListener(this.popupMenu);
		addPopup(getTree(), this.popupMenu.getJPopupMenu());
	}

	public void setCurrentUrl(String url) {
		// TODO Auto-generated method stub

	}

	public void setDisplayHiddenFiles(boolean display) {
		// TODO Auto-generated method stub

	}

	public void setExtensionsToDisplay(String[] extensions) {
		// TODO Auto-generated method stub

	}

	public void setRootUrl(GridFile url) {
		// TODO Auto-generated method stub

	}
}
