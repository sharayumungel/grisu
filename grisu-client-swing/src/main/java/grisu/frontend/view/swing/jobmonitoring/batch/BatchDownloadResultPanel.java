package grisu.frontend.view.swing.jobmonitoring.batch;

import grisu.control.ServiceInterface;
import grisu.control.exceptions.RemoteFileSystemException;
import grisu.frontend.control.jobMonitoring.RunningJobManager;
import grisu.frontend.model.job.BatchJobObject;
import grisu.frontend.model.job.JobObject;
import grisu.frontend.view.swing.files.FileListListener;
import grisu.frontend.view.swing.files.FileListPanel;
import grisu.frontend.view.swing.files.FileListPanelContextMenu;
import grisu.frontend.view.swing.files.FileListPanelSimple;
import grisu.frontend.view.swing.files.FileSizeRenderer;
import grisu.frontend.view.swing.files.GlazedFileComparator;
import grisu.frontend.view.swing.files.GlazedFileRenderer;
import grisu.frontend.view.swing.files.GlazedFileTableFormat;
import grisu.frontend.view.swing.files.GlazedFilesTransferHandler;
import grisu.frontend.view.swing.files.TimestampRenderer;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;
import grisu.model.files.GlazedFile;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventTableModel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class BatchDownloadResultPanel extends JPanel implements FileListPanel,
		PropertyChangeListener {
	private JScrollPane scrollPane;
	private JXTable table;

	private Vector<FileListListener> listeners;
	private FileListPanelContextMenu popupMenu;

	private final EventTableModel<GlazedFile> fileModel;
	private final EventList<GlazedFile> currentDirectoryContent = new BasicEventList<GlazedFile>();
	private final SortedList<GlazedFile> sortedList = new SortedList<GlazedFile>(
			currentDirectoryContent, new GlazedFileComparator());
	private BatchJobObject batchJob;
	private String[] patterns = new String[] {};
	private ServiceInterface si;
	private FileManager fm;
	private RunningJobManager rjm;
	private Thread rebuildThread;

	private boolean displayTimestamp = false;

	private boolean displaySize = true;

	static final Logger myLogger = Logger.getLogger(FileListPanelSimple.class
			.getName());

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

	/**
	 * Create the panel.
	 */
	public BatchDownloadResultPanel(boolean displaySize,
			boolean displayTimestamp) {

		this.displaySize = displaySize;
		this.displayTimestamp = displayTimestamp;

		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC, }));

		fileModel = new EventTableModel<GlazedFile>(sortedList,
				new GlazedFileTableFormat());

		add(getScrollPane(), "2, 2, fill, fill");

	}

	public void addFileListListener(FileListListener l) {

		if (listeners == null) {
			listeners = new Vector<FileListListener>();
		}
		listeners.addElement(l);
	}

	public void displayHiddenFiles(boolean display) {
		// do nothing
		throw new RuntimeException(
				"Setting of displayhiddenfiles not implemented");
	}

	private void fileClickOccured() {

		fireFilesSelected(getSelectedFiles());

	}

	private void fileDoubleClickOccured() {

		final int selRow = table.getSelectedRow();
		if (selRow >= 0) {

			final GlazedFile sel = (GlazedFile) fileModel.getValueAt(selRow, 0);

			if (!sel.isFolder()) {
				fireFileDoubleClicked(sel);
			}

		}

	}

	private void fireFileDoubleClicked(final GlazedFile file) {
		// if we have no mountPointsListeners, do nothing...
		if ((listeners != null) && !listeners.isEmpty()) {

			final Thread thread = new Thread() {
				@Override
				public void run() {

					setLoading(true);
					try {
						// make a copy of the listener list in case
						// anyone adds/removes mountPointsListeners
						Vector<FileListListener> targets;
						synchronized (this) {
							targets = (Vector<FileListListener>) listeners
									.clone();
						}

						// walk through the listener list and
						// call the userInput method in each
						for (final FileListListener l : targets) {
							try {
								l.fileDoubleClicked(file);
							} catch (final Exception e1) {
								e1.printStackTrace();
							}
						}
					} finally {
						setLoading(false);
					}

				}
			};

			thread.start();

		}
	}

	private void fireFilesSelected(Set<GlazedFile> files) {

		// if we have no mountPointsListeners, do nothing...
		if ((listeners != null) && !listeners.isEmpty()) {

			setLoading(true);
			try {
				// make a copy of the listener list in case
				// anyone adds/removes mountPointsListeners
				Vector<FileListListener> targets;
				synchronized (this) {
					targets = (Vector<FileListListener>) listeners.clone();
				}

				// walk through the listener list and
				// call the userInput method in each
				for (final FileListListener l : targets) {
					try {
						l.filesSelected(files);
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			} finally {
				setLoading(false);
			}
		}

	}

	private void fireIsLoading(boolean loading) {
		// if we have no mountPointsListeners, do nothing...
		if ((listeners != null) && !listeners.isEmpty()) {

			// make a copy of the listener list in case
			// anyone adds/removes mountPointsListeners
			Vector<FileListListener> targets;
			synchronized (this) {
				targets = (Vector<FileListListener>) listeners.clone();
			}

			// walk through the listener list and
			// call the userInput method in each
			for (final FileListListener l : targets) {
				try {
					l.isLoading(loading);
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public GlazedFile getCurrentDirectory() {

		return null;
	}

	public JPanel getPanel() {
		return this;
	}

	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setViewportView(getTable());
		}
		return scrollPane;
	}

	public Set<GlazedFile> getSelectedFiles() {

		final Set<GlazedFile> selected = new TreeSet<GlazedFile>();

		for (final int r : table.getSelectedRows()) {

			if (r >= 0) {
				final GlazedFile sel = (GlazedFile) fileModel.getValueAt(r, 0);
				selected.add(sel);
			}

		}

		return selected;
	}

	private JXTable getTable() {
		if (table == null) {
			table = new JXTable(fileModel);

			table.setDragEnabled(true);
			// table.setDropMode(DropMode.ON);

			// disable sorting for now
			table.setAutoCreateRowSorter(false);
			table.setRowSorter(null);
			table.setColumnControlVisible(true);
			// table.setDefaultRenderer(GlazedFile.class, new
			// GlazedFileRenderer(
			// this));
			// table.setDefaultRenderer(Long.class, new FileSizeRenderer());

			int vColIndex = 0;
			TableColumn col = table.getColumnModel().getColumn(vColIndex);
			col.setCellRenderer(new GlazedFileRenderer());
			int width = 120;
			col.setPreferredWidth(width);
			col.setMinWidth(80);

			vColIndex = 1;
			col = table.getColumnModel().getColumn(vColIndex);
			col.setCellRenderer(new FileSizeRenderer());
			width = 60;
			col.setPreferredWidth(width);
			col.setMaxWidth(80);

			vColIndex = 2;
			col = table.getColumnModel().getColumn(vColIndex);
			col.setCellRenderer(new TimestampRenderer());
			width = 90;
			col.setPreferredWidth(width);
			col.setMaxWidth(120);

			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {

					if (arg0.getClickCount() == 2) {
						fileDoubleClickOccured();
					} else if (arg0.getClickCount() == 1) {
						fileClickOccured();
					}

				}
			});

			if (!displaySize) {
				table.getColumnExt("Size").setVisible(false);
			}

			if (!displayTimestamp) {
				final TableColumnExt colext = table
						.getColumnExt("Date modified");
				colext.setVisible(false);
			}
		}
		return table;
	}

	public void propertyChange(PropertyChangeEvent arg0) {

		if (BatchJobObject.NUMBER_OF_FINISHED_JOBS.equals(arg0
				.getPropertyName())) {
			try {
				rebuildFileList();
			} catch (final RemoteFileSystemException e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void rebuildFileList()
			throws RemoteFileSystemException {

		if ((this.si == null) || (this.batchJob == null)) {
			return;
		}

		if ((rebuildThread != null) && rebuildThread.isAlive()) {
			return;
		}

		rebuildThread = new Thread() {
			@Override
			public void run() {

				// currentDirectoryContent.getReadWriteLock().writeLock().lock();
				//
				// currentDirectoryContent.clear();
				// currentDirectoryContent.getReadWriteLock().writeLock().unlock();

				final List<GlazedFile> files;
				try {

					for (final JobObject job : batchJob.getJobs()) {

						if (!job.isFinished(false)) {
							continue;
						}

						final Set<String> urlsToCheck = new HashSet<String>();

						final List<String> urls = job.listJobDirectory(0);

						final String path = batchJob.pathToInputFiles();

						for (final String child : urls) {
							final String temp = FileManager.getFilename(child);
							for (final String pattern : patterns) {
								if (temp.indexOf(pattern) >= 0) {
									final GlazedFile gf = fm
											.createGlazedFileFromUrl(
													child,
													GlazedFile.Type.FILETYPE_FILE);
									if (!currentDirectoryContent.contains(gf)) {
										currentDirectoryContent
												.getReadWriteLock().writeLock()
												.lock();
										currentDirectoryContent.add(gf);
										currentDirectoryContent
												.getReadWriteLock().writeLock()
												.unlock();
									}
									break;
								}
							}

						}
					}

				} catch (final RemoteFileSystemException e) {
					e.printStackTrace();
				}

			}
		};
		rebuildThread.start();
		myLogger.debug("Rebuilding file list started...");
	}

	public void refresh() {

		try {
			rebuildFileList();
		} catch (final RemoteFileSystemException e) {
			e.printStackTrace();
		}

	}

	public void removeFileListListener(FileListListener l) {

		if (listeners == null) {
			listeners = new Vector<FileListListener>();
		}
		listeners.removeElement(l);

	}

	public void setBatchJob(BatchJobObject batchJob) {

		if (this.batchJob != null) {
			this.batchJob.removePropertyChangeListener(this);
		}
		this.batchJob = batchJob;
		this.batchJob.addPropertyChangeListener(this);
		try {
			rebuildFileList();
		} catch (final RemoteFileSystemException e) {
			e.printStackTrace();
		}
	}

	public void setBatchJobAndPatterns(BatchJobObject batchJob,
			String[] patterns) {
		if (this.batchJob != null) {
			this.batchJob.removePropertyChangeListener(this);
		}
		this.batchJob = batchJob;
		this.batchJob.addPropertyChangeListener(this);
		this.patterns = patterns;

		try {
			rebuildFileList();
		} catch (final RemoteFileSystemException e) {
			e.printStackTrace();
		}

	}

	public void setContextMenu(FileListPanelContextMenu menu) {

		if (this.popupMenu != null) {
			removeFileListListener(this.popupMenu);
		}
		this.popupMenu = menu;
		menu.setFileListPanel(this);
		addFileListListener(this.popupMenu);
		addPopup(table, this.popupMenu.getJPopupMenu());
	}

	public void setCurrentUrl(String url) {
		// not used here
	}

	public void setExtensionsToDisplay(String[] extensions) {
		// do nothing
		throw new RuntimeException(
				"Setting of extensions to display not implemented");
	}

	private void setLoading(final boolean loading) {

		SwingUtilities.invokeLater(new Thread() {

			@Override
			public void run() {

				if (!loading) {
					fireIsLoading(false);
				}

				getTable().setEnabled(!loading);
				getScrollPane().setEnabled(!loading);

				if (loading) {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				} else {
					setCursor(Cursor.getDefaultCursor());
				}
				if (loading) {
					fireIsLoading(true);
				}

			}
		});

	}

	public void setPatterns(String[] patterns) {
		this.patterns = patterns;

		try {
			rebuildFileList();
		} catch (final RemoteFileSystemException e) {
			e.printStackTrace();
		}
	}

	public void setRootUrl(String url) {
		// not used here
	}

	public void setServiceInterface(ServiceInterface si) {
		this.si = si;
		this.fm = GrisuRegistryManager.getDefault(si).getFileManager();
		this.rjm = RunningJobManager.getDefault(si);

		table.setTransferHandler(new GlazedFilesTransferHandler(this, si));
		final BatchResultContextMenu menu = new BatchResultContextMenu(this.si);
		setContextMenu(menu);

	}
}