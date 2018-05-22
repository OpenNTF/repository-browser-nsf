package org.openntf.website.repositorybrowser.fs.usnsf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openntf.website.repositorybrowser.fs.mem.MemoryVFSFolder;
import org.openntf.website.repositorybrowser.fs.mem.XMLDocumentVFSFile;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;
import com.ibm.commons.vfs.VFSFilter.IFilter;
import com.ibm.commons.xml.DOMUtil;
import com.ibm.commons.xml.XMLException;
import com.ibm.commons.vfs.VFSFolder;
import com.ibm.commons.vfs.VFSResource;

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

	private final VFSFolder root;
	private final List<VFSResource> rootResources;
	
	private List<UpdateSiteNSFVFSFile> plugins;
	private List<UpdateSiteNSFVFSFile> features;
	
	public UpdateSiteNSFVFS(String name, Database database) {
		this.name = name;
		this.database = database;
		this.root = new MemoryVFSFolder(this, name);
		try {
			this.rootResources = Arrays.asList(
				new MemoryVFSFolder(this, this.name + VFS.SEPARATOR + "features"), //$NON-NLS-1$
				new MemoryVFSFolder(this, this.name + VFS.SEPARATOR + "plugins"), //$NON-NLS-1$
				createContentXml(),
				createArtifactsXml()
			);
		} catch(XMLException | IOException | NotesException | VFSException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doReadEntries(VFS vfs, String path, List result) {
		if(StringUtil.isEmpty(path)) {
			result.add(doCreateFolderEntry(root));
		} else if(StringUtil.equals(path, name)) {
			for(VFSResource res : rootResources) {
				if(res instanceof VFSFile) {
					result.add(doCreateFileEntry((VFSFile)res));
				} else if(res instanceof VFSFolder) {
					result.add(doCreateFolderEntry((VFSFolder)res));
				}
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
			result.addAll(rootResources);
		} else if(StringUtil.equals(path, this.name + VFS.SEPARATOR + "plugins")) { //$NON-NLS-1$
			result.addAll(this.getPlugins());
		} else if(StringUtil.equals(path, this.name + VFS.SEPARATOR + "features")) { //$NON-NLS-1$
			result.addAll(this.getFeatures());
		}
	}

	@Override
	protected FileEntry doCreateFileEntry(VFSFile file) {
		if(file instanceof UpdateSiteNSFVFSFile) {
			UpdateSiteNSFVFSFile nsfFile = (UpdateSiteNSFVFSFile)file;
			return new UpdateSiteNSFFileEntry(this, file, nsfFile.getDocLastModified());
		} else {
			return new UpdateSiteNSFFileEntry(this, file, this.getDatabaseLastModified());
		}
	}

	@Override
	protected FolderEntry doCreateFolderEntry(VFSFolder folder) {
		return new UpdateSiteNSFFolderEntry(this, folder, this.getDatabaseLastModified());
	}

	@Override
	protected VFSFile doCreateVFSFile(String fileName) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected VFSFolder doCreateVFSFolder(String folderName) {
		return new MemoryVFSFolder(this, folderName);
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
	
	private long getDatabaseLastModified() {
		try {
			DateTime dt = database.getLastModified();
			try {
				return dt.toJavaDate().getTime();
			} finally {
				dt.recycle();
			}
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}
	
	private synchronized List<UpdateSiteNSFVFSFile> getPlugins() {
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
							this.plugins.add(new UpdateSiteNSFVFSFile(this, this.name + VFS.SEPARATOR + "plugins" + VFS.SEPARATOR + fileName, name, version, doc)); //$NON-NLS-1$
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
	private synchronized List<UpdateSiteNSFVFSFile> getFeatures() {
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
							this.features.add(new UpdateSiteNSFVFSFile(this, this.name + VFS.SEPARATOR + "features" + VFS.SEPARATOR + fileName, name, version, doc)); //$NON-NLS-1$
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
	
	private VFSFile createArtifactsXml() throws XMLException, IOException, DOMException, NotesException, VFSException {
		org.w3c.dom.Document doc = DOMUtil.createDocument();
		
		{
			ProcessingInstruction proc = doc.createProcessingInstruction("artifactRepository", "version='1.1.0'"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(proc);
		}
		
		Element repository = DOMUtil.createElement(doc, "repository"); //$NON-NLS-1$
		repository.setAttribute("name", database.getTitle() + " Artifacts"); //$NON-NLS-1$ //$NON-NLS-2$
		repository.setAttribute("type", "org.eclipse.equinox.p2.artifact.repository.simpleRepository"); //$NON-NLS-1$ //$NON-NLS-2$
		repository.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
		
		{
			Element properties = DOMUtil.createElement(doc, repository, "properties"); //$NON-NLS-1$
			properties.setAttribute("size", "2"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Element timestamp = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			timestamp.setAttribute("name", "p2.timestamp"); //$NON-NLS-1$ //$NON-NLS-2$
			timestamp.setAttribute("value", StringUtil.toString(this.getDatabaseLastModified())); //$NON-NLS-1$
			
			Element compressed = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			compressed.setAttribute("name", "p2.compressed"); //$NON-NLS-1$ //$NON-NLS-2$
			compressed.setAttribute("value", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		{
			Element mappings = DOMUtil.createElement(doc, repository, "mappings"); //$NON-NLS-1$
			mappings.setAttribute("size", "3"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Element rule1 = DOMUtil.createElement(doc, mappings, "rule"); //$NON-NLS-1$
			rule1.setAttribute("filter", "(& (classifier=osgi.bundle))"); //$NON-NLS-1$ //$NON-NLS-2$
			rule1.setAttribute("output", "${repoUrl}/plugins/${id}_${version}.jar"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Element rule2 = DOMUtil.createElement(doc, mappings, "rule"); //$NON-NLS-1$
			rule2.setAttribute("filter", "(& (classifier=binary))"); //$NON-NLS-1$ //$NON-NLS-2$
			rule2.setAttribute("output", "${repoUrl}/binary/${id}_${version}"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Element rule3 = DOMUtil.createElement(doc, mappings, "rule"); //$NON-NLS-1$
			rule3.setAttribute("filter", "(& (classifier=org.eclipse.update.feature))"); //$NON-NLS-1$ //$NON-NLS-2$
			rule3.setAttribute("output", "${repoUrl}/features/${id}_${version}.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		List<UpdateSiteNSFVFSFile> features = getFeatures();
		List<UpdateSiteNSFVFSFile> plugins = getPlugins();
		Element artifacts = DOMUtil.createElement(doc, repository, "artifacts"); //$NON-NLS-1$
		artifacts.setAttribute("size", StringUtil.toString(features.size() + plugins.size())); //$NON-NLS-1$
		
		for(UpdateSiteNSFVFSFile feature : features) {
			Element artifact = DOMUtil.createElement(doc, artifacts, "artifact"); //$NON-NLS-1$
			artifact.setAttribute("classifier", "org.eclipse.update.feature"); //$NON-NLS-1$ //$NON-NLS-2$
			artifact.setAttribute("id", feature.getId()); //$NON-NLS-1$
			artifact.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
			
			Element properties = DOMUtil.createElement(doc, artifact, "properties"); //$NON-NLS-1$
			properties.setAttribute("size", "3"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Element artifactSize = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			artifactSize.setAttribute("name", "artifact.size"); //$NON-NLS-1$ //$NON-NLS-2$
			artifactSize.setAttribute("value", StringUtil.toString(feature.getSize())); //$NON-NLS-1$
			
			Element downloadSize = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			downloadSize.setAttribute("name", "download.size"); //$NON-NLS-1$ //$NON-NLS-2$
			downloadSize.setAttribute("value", StringUtil.toString(feature.getSize())); //$NON-NLS-1$
			
			Element contentType = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			contentType.setAttribute("name", "download.contentType"); //$NON-NLS-1$ //$NON-NLS-2$
			contentType.setAttribute("value", feature.getMimeType()); //$NON-NLS-1$
		}
		
		for(UpdateSiteNSFVFSFile plugin : plugins) {
			Element artifact = DOMUtil.createElement(doc, artifacts, "artifact"); //$NON-NLS-1$
			artifact.setAttribute("classifier", "osgi.bundle"); //$NON-NLS-1$ //$NON-NLS-2$
			artifact.setAttribute("id", plugin.getId()); //$NON-NLS-1$
			artifact.setAttribute("version", plugin.getVersion()); //$NON-NLS-1$
			
			Element properties = DOMUtil.createElement(doc, artifact, "properties"); //$NON-NLS-1$
			properties.setAttribute("size", "2"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Element artifactSize = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			artifactSize.setAttribute("name", "artifact.size"); //$NON-NLS-1$ //$NON-NLS-2$
			artifactSize.setAttribute("value", StringUtil.toString(plugin.getSize())); //$NON-NLS-1$
			
			Element downloadSize = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
			downloadSize.setAttribute("name", "download.size"); //$NON-NLS-1$ //$NON-NLS-2$
			downloadSize.setAttribute("value", StringUtil.toString(plugin.getSize())); //$NON-NLS-1$
		}
		
		return new XMLDocumentVFSFile(this, this.name + VFS.SEPARATOR + "artifacts.xml", doc); //$NON-NLS-1$
	}
	
	private VFSFile createContentXml() throws XMLException, IOException {
		org.w3c.dom.Document doc = DOMUtil.createDocument();
		
		return new XMLDocumentVFSFile(this, this.name + VFS.SEPARATOR + "content.xml", doc); //$NON-NLS-1$
	}
}
