package org.openntf.website.repositorybrowser.fs.usnsf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;
import com.ibm.commons.vfs.VFSFilter.IFilter;
import com.ibm.commons.vfs.VFSFolder;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;

/**
 * Virtual filesystem to expose the contents of an Update Site NSF as part
 * of the filesystem.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
class UpdateSiteNSFVFS extends VFS {
	public static final String VIEW_PLUGINS = "vwPlugins"; //$NON-NLS-1$
	public static final String VIEW_FEATURES = "vwFeatures"; //$NON-NLS-1$
	
	public static final int ICON_ENABLED = 202;
	
	private final String name;
	private final Database database;
	
	private final List<VFSFolder> folders;
	private final VFSFolder root;
	
	private List<VFSFile> plugins;
	private List<VFSFile> features;
	
	public UpdateSiteNSFVFS(String name, Database database) {
		this.name = name;
		this.database = database;
		this.root = new UpdateSiteNSFVFSFolder(this, name);
		this.folders = Arrays.asList(
			new UpdateSiteNSFVFSFolder(this, this.name + VFS.SEPARATOR + "features"), //$NON-NLS-1$
			new UpdateSiteNSFVFSFolder(this, this.name + VFS.SEPARATOR + "plugins") //$NON-NLS-1$
		);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doReadEntries(VFS vfs, String path, List result) {
		if(StringUtil.isEmpty(path)) {
			result.add(doCreateFolderEntry(root));
		} else if(StringUtil.equals(path, name)) {
			for(VFSFolder folder : folders) {
				result.add(doCreateFolderEntry(folder));
			}
		} else if(StringUtil.equals(path, this.name + VFS.SEPARATOR + "plugins")) { //$NON-NLS-1$
			for(VFSFile plugin : this.getPlugins()) {
				result.add(doCreateFileEntry(plugin));
			}
		} else if(StringUtil.equals(path, this.name + VFS.SEPARATOR + "features")) { //$NON-NLS-1$
			for(VFSFile feature : this.getFeatures()) {
				result.add(doCreateFileEntry(feature));
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doReadResources(String path, List result, IFilter filter) {
		if(StringUtil.isEmpty(path)) {
			result.add(root);
		} else if(StringUtil.equals(path, this.name)) {
			result.addAll(folders);
		} else if(StringUtil.equals(path, this.name + VFS.SEPARATOR + "plugins")) { //$NON-NLS-1$
			result.addAll(this.getPlugins());
		} else if(StringUtil.equals(path, this.name + VFS.SEPARATOR + "features")) { //$NON-NLS-1$
			result.addAll(this.getFeatures());
		}
	}

	@Override
	protected FileEntry doCreateFileEntry(VFSFile file) {
		UpdateSiteNSFVFSFile nsfFile = (UpdateSiteNSFVFSFile)file;
		return new UpdateSiteNSFFileEntry(this, file, nsfFile.getDocLastModified());
	}

	@Override
	protected FolderEntry doCreateFolderEntry(VFSFolder folder) {
		try {
			DateTime dt = database.getLastModified();
			try {
				return new UpdateSiteNSFFolderEntry(this, folder, dt.toJavaDate().getTime());
			} finally {
				dt.recycle();
			}
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected VFSFile doCreateVFSFile(String fileName) {
		// TODO look up the file
		throw new UnsupportedOperationException();
	}

	@Override
	protected VFSFolder doCreateVFSFolder(String folderName) {
		return new UpdateSiteNSFVFSFolder(this, folderName);
	}

	@Override
	protected boolean doIsReadonly() throws VFSException {
		return true;
	}

	@Override
	protected void doClose() {
		try {
			database.recycle();
		} catch(NotesException e) {
			// Ignore
		}
	}
	
	// *******************************************************************************
	// * Internal implementation methods
	// *******************************************************************************
	
	private static class UpdateSiteNSFFolderEntry extends VFS.FolderEntry {
		public UpdateSiteNSFFolderEntry(VFS vfs, VFSFolder folder, long lastModified) {
			super(vfs, folder, lastModified);
		}
	}
	
	private static class UpdateSiteNSFFileEntry extends VFS.FileEntry {
		public UpdateSiteNSFFileEntry(VFS vfs, VFSFile file, long lastModified) {
			super(vfs, file, lastModified);
		}
	}
	
	private synchronized List<VFSFile> getPlugins() {
		if(this.plugins == null) {
			try {
				this.plugins = new ArrayList<>();
				
				View view = database.getView(VIEW_PLUGINS);
				view.setAutoUpdate(false);
				ViewNavigator nav = view.createViewNav();
				nav.setBufferMaxEntries(400);
				ViewEntry entry = nav.getFirst();
				while(entry != null) {
					entry.setPreferJavaDates(true);
					if(entry.isDocument()) {
						List<?> columnValues = entry.getColumnValues();
						
						String name = StringUtil.toString(columnValues.get(1));
						if(StringUtil.isNotEmpty(name)) {
							String version = StringUtil.toString(columnValues.get(2));
							
							String fileName = name + "_" + version + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
							Document doc = entry.getDocument();
							this.plugins.add(new UpdateSiteNSFVFSFile(this, this.name + VFS.SEPARATOR + "plugins" + VFS.SEPARATOR + fileName, doc)); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					
					ViewEntry tempEntry = entry;
					entry = nav.getNext();
					tempEntry.recycle();
				}
			} catch(NotesException e) {
				throw new RuntimeException(e);
			}
		}
		return this.plugins;
	}
	private synchronized List<VFSFile> getFeatures() {
		if(this.features == null) {
			try {
				this.features = new ArrayList<>();
				
				View view = database.getView(VIEW_FEATURES);
				view.setAutoUpdate(false);
				ViewNavigator nav = view.createViewNav();
				nav.setBufferMaxEntries(400);
				ViewEntry entry = nav.getFirst();
				while(entry != null) {
					entry.setPreferJavaDates(true);
					if(entry.isDocument()) {
						List<?> columnValues = entry.getColumnValues();
						
						boolean enabled = (double)columnValues.get(0) == ICON_ENABLED;
						String name = StringUtil.toString(columnValues.get(4));
						if(enabled && StringUtil.isNotEmpty(name)) {
							String version = StringUtil.toString(columnValues.get(5));
							
							String fileName = name + "_" + version + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
							
							Document doc = entry.getDocument();
							this.features.add(new UpdateSiteNSFVFSFile(this, this.name + VFS.SEPARATOR + "features" + VFS.SEPARATOR + fileName, doc)); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					
					ViewEntry tempEntry = entry;
					entry = nav.getNext();
					tempEntry.recycle();
				}
			} catch(NotesException e) {
				throw new RuntimeException(e);
			}
		}
		return this.features;
	}
}
