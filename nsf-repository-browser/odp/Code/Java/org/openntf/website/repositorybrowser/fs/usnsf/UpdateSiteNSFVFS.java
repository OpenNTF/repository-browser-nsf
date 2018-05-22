/**
 * Copyright (c) 2018 Christian Guedemann, Jesse Gallagher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.website.repositorybrowser.fs.usnsf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
	public static final String VIEW_FRAGMENTS = "vwFragments"; //$NON-NLS-1$
	public static final String VIEW_FEATURES = "vwFeatures"; //$NON-NLS-1$
	
	public static final int ICON_ENABLED = 202;
	
	private final String name;
	private final Database database;

	private final VFSFolder root;
	private final List<VFSResource> rootResources;
	
	private List<UpdateSiteNSFVFSPlugin> plugins;
	private List<UpdateSiteNSFVFSFeature> features;
	
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
		} catch(XMLException | IOException | NotesException | VFSException | DOMException e) {
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
	
	private synchronized List<UpdateSiteNSFVFSPlugin> getPlugins() {
		if(this.plugins == null) {
			try {
				this.plugins = new ArrayList<>();
				
				for(String viewName : Arrays.asList(VIEW_PLUGINS, VIEW_FRAGMENTS)) {
					View view = database.getView(viewName);
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
								this.plugins.add(new UpdateSiteNSFVFSPlugin(this, this.name + VFS.SEPARATOR + "plugins" + VFS.SEPARATOR + fileName, name, version, doc)); //$NON-NLS-1$
							}
						}
						
						ViewEntry tempEntry = entry;
						entry = nav.getNext();
						tempEntry.recycle();
					}
				}
				
			} catch(NotesException e) {
				throw new RuntimeException(e);
			}
		}
		return this.plugins;
	}
	private synchronized List<UpdateSiteNSFVFSFeature> getFeatures() {
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
							this.features.add(new UpdateSiteNSFVFSFeature(this, this.name + VFS.SEPARATOR + "features" + VFS.SEPARATOR + fileName, name, version, doc)); //$NON-NLS-1$
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
		
		List<UpdateSiteNSFVFSFeature> features = getFeatures();
		List<UpdateSiteNSFVFSPlugin> plugins = getPlugins();
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
	
	private VFSFile createContentXml() throws XMLException, IOException, DOMException, NotesException {
		org.w3c.dom.Document doc = DOMUtil.createDocument();
		
		{
			ProcessingInstruction proc = doc.createProcessingInstruction("metadataRepository", "version='1.1.0'"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(proc);
		}
		
		Element repository = DOMUtil.createElement(doc, "repository"); //$NON-NLS-1$
		repository.setAttribute("name", database.getTitle()); //$NON-NLS-1$
		repository.setAttribute("type", "org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository"); //$NON-NLS-1$ //$NON-NLS-2$
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
		
		List<UpdateSiteNSFVFSFeature> features = getFeatures();
		List<UpdateSiteNSFVFSPlugin> plugins = getPlugins();
		Element units = DOMUtil.createElement(doc, repository, "units"); //$NON-NLS-1$
		units.setAttribute("size", StringUtil.toString(features.size() + plugins.size())); //$NON-NLS-1$
		
		for(UpdateSiteNSFVFSFeature feature : features) {
			Element unit = DOMUtil.createElement(doc, units, "unit"); //$NON-NLS-1$
			unit.setAttribute("id", feature.getId() + ".feature.group"); //$NON-NLS-1$ //$NON-NLS-2$
			unit.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
			
			{
				Element update = DOMUtil.createElement(doc, unit, "update"); //$NON-NLS-1$
				update.setAttribute("id", feature.getId() + ".feature.group"); //$NON-NLS-1$ //$NON-NLS-2$
				update.setAttribute("range", "[0.0.0," + feature.getVersion() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				update.setAttribute("severity", "0"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			{
				Element properties = DOMUtil.createElement(doc, unit, "properties"); //$NON-NLS-1$
				properties.setAttribute("size", "4"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element propName = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
				propName.setAttribute("name", "org.eclipse.equinox.p2.name"); //$NON-NLS-1$ //$NON-NLS-2$
				propName.setAttribute("value", feature.getFeatureName()); //$NON-NLS-1$
	
				Element propDesc = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
				propDesc.setAttribute("name", "org.eclipse.equinox.p2.description"); //$NON-NLS-1$ //$NON-NLS-2$
				propDesc.setAttribute("value", feature.getFeatureDescription()); //$NON-NLS-1$
	
				Element propDescUrl = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
				propDescUrl.setAttribute("name", "org.eclipse.equinox.p2.description.url"); //$NON-NLS-1$ //$NON-NLS-2$
				propDescUrl.setAttribute("value", feature.getFeatureDescriptionUrl()); //$NON-NLS-1$
				
				Element propTypeGroup = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
				propTypeGroup.setAttribute("name", "org.eclipse.equinox.p2.type.group"); //$NON-NLS-1$ //$NON-NLS-2$
				propTypeGroup.setAttribute("value", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			{
				Element provides = DOMUtil.createElement(doc, unit, "provides"); //$NON-NLS-1$
				provides.setAttribute("size", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element provided = DOMUtil.createElement(doc, provides, "provided"); //$NON-NLS-1$
				provided.setAttribute("namespace", "org.eclipse.equinox.p2.iu"); //$NON-NLS-1$ //$NON-NLS-2$
				provided.setAttribute("name", feature.getId() + ".feature.group"); //$NON-NLS-1$ //$NON-NLS-2$
				provided.setAttribute("version", feature.getVersion()); //$NON-NLS-1$
			}
			
			{
				int requiresSize = 0;
				Element requires = DOMUtil.createElement(doc, unit, "requires"); //$NON-NLS-1$
				
				List<String> requiredFeatures = feature.getImportedFeatures();
				List<String> requiredVersions = feature.getImportedFeatureVersions();
				for(int i = 0; i < requiredFeatures.size(); i++) {
					String id = requiredFeatures.get(i);
					if(StringUtil.isNotEmpty(id)) {
						Element required = DOMUtil.createElement(doc, requires, "required"); //$NON-NLS-1$
						required.setAttribute("namespace", "org.eclipse.equinox.p2.iu"); //$NON-NLS-1$ //$NON-NLS-2$
						required.setAttribute("name", id + ".feature.group"); //$NON-NLS-1$ //$NON-NLS-2$
						if(requiredVersions.size() > i) {
							String version = requiredVersions.get(i);
							if(StringUtil.isNotEmpty(version)) {
								required.setAttribute("range", version); //$NON-NLS-1$
							} else {
								required.setAttribute("range", "0.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
							}
						} else {
							required.setAttribute("range", "0.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
						}
						
						requiresSize++;
					}
				}
				
				for(PluginInfo plugin : feature.getPlugins()) {
					Element required = DOMUtil.createElement(doc, requires, "required"); //$NON-NLS-1$
					required.setAttribute("namespace", "org.eclipse.equinox.p2.iu"); //$NON-NLS-1$ //$NON-NLS-2$
					required.setAttribute("name", plugin.getId()); //$NON-NLS-1$
					required.setAttribute("range", "[" + plugin.getVersion() + "," + plugin.getVersion() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					
					requiresSize++;
				}
				
				{
					Element required = DOMUtil.createElement(doc, requires, "required"); //$NON-NLS-1$
					required.setAttribute("namespace", "org.eclipse.equinox.p2.iu"); //$NON-NLS-1$ //$NON-NLS-2$
					required.setAttribute("name", feature.getId() + ".feature.jar"); //$NON-NLS-1$ //$NON-NLS-2$
					required.setAttribute("range", "[" + feature.getVersion() + "," + feature.getVersion() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					
					Element filter = DOMUtil.createElement(doc, required, "filter"); //$NON-NLS-1$
					filter.setTextContent("(org.eclipse.update.install.features=true)"); //$NON-NLS-1$
					
					requiresSize++;
				}
				
				requires.setAttribute("size", StringUtil.toString(requiresSize)); //$NON-NLS-1$
			}
			
			{
				Element touchpoint = DOMUtil.createElement(doc, unit, "touchpoint"); //$NON-NLS-1$
				touchpoint.setAttribute("id", "null"); //$NON-NLS-1$ //$NON-NLS-2$
				touchpoint.setAttribute("version", "0.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			{
				Element licenses = DOMUtil.createElement(doc, unit, "licenses"); //$NON-NLS-1$
				licenses.setAttribute("size", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element license = DOMUtil.createElement(doc, licenses, "license"); //$NON-NLS-1$
				license.setAttribute("uri", feature.getLicenseUrl()); //$NON-NLS-1$
				license.setAttribute("url", feature.getLicenseUrl()); //$NON-NLS-1$
				license.setTextContent(feature.getLicense());
			}
			
			{
				Element copyright = DOMUtil.createElement(doc, unit, "copyright"); //$NON-NLS-1$
				copyright.setAttribute("uri", feature.getCopyrightUrl()); //$NON-NLS-1$
				copyright.setAttribute("url", feature.getCopyrightUrl()); //$NON-NLS-1$
				copyright.setTextContent(feature.getCopyright());
			}
		}
		
		for(UpdateSiteNSFVFSPlugin plugin : plugins) {
			Element unit = DOMUtil.createElement(doc, units, "unit"); //$NON-NLS-1$
			unit.setAttribute("id", plugin.getId()); //$NON-NLS-1$
			unit.setAttribute("version", plugin.getVersion()); //$NON-NLS-1$
			
			{
				Element update = DOMUtil.createElement(doc, unit, "update"); //$NON-NLS-1$
				update.setAttribute("id", plugin.getId() + ".feature.group"); //$NON-NLS-1$ //$NON-NLS-2$
				update.setAttribute("range", "[0.0.0," + plugin.getVersion() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				update.setAttribute("severity", "0"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			{
				Element properties = DOMUtil.createElement(doc, unit, "properties"); //$NON-NLS-1$
				properties.setAttribute("size", "2"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element propName = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
				propName.setAttribute("name", "org.eclipse.equinox.p2.name"); //$NON-NLS-1$ //$NON-NLS-2$
				propName.setAttribute("value", plugin.getPluginName()); //$NON-NLS-1$
				
				Element propProvider = DOMUtil.createElement(doc, properties, "provider"); //$NON-NLS-1$
				propProvider.setAttribute("name", "org.eclipse.equinox.p2.provider"); //$NON-NLS-1$ //$NON-NLS-2$
				propProvider.setAttribute("value", plugin.getProviderName()); //$NON-NLS-1$
			}
			
			{
				Element provides = DOMUtil.createElement(doc, unit, "provides"); //$NON-NLS-1$
				provides.setAttribute("size", StringUtil.toString(plugin.isFragment() ? 4 : 3)); //$NON-NLS-1$
				
				Element providedIu = DOMUtil.createElement(doc, provides, "provided"); //$NON-NLS-1$
				providedIu.setAttribute("namespace", "org.eclipse.equinox.p2.iu"); //$NON-NLS-1$ //$NON-NLS-2$
				providedIu.setAttribute("name", plugin.getId()); //$NON-NLS-1$
				providedIu.setAttribute("version", plugin.getVersion()); //$NON-NLS-1$
				
				Element providedBundle = DOMUtil.createElement(doc, provides, "provided"); //$NON-NLS-1$
				providedBundle.setAttribute("namespace", "osgi.bundle"); //$NON-NLS-1$ //$NON-NLS-2$
				providedBundle.setAttribute("name", plugin.getId()); //$NON-NLS-1$
				providedBundle.setAttribute("version", plugin.getVersion()); //$NON-NLS-1$
				
				Element providedType = DOMUtil.createElement(doc, provides, "provided"); //$NON-NLS-1$
				providedType.setAttribute("namespace", "org.eclipse.equinox.p2.eclipse.type"); //$NON-NLS-1$ //$NON-NLS-2$
				providedType.setAttribute("name", "bundle"); //$NON-NLS-1$ //$NON-NLS-2$
				providedType.setAttribute("version", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
				
				if(plugin.isFragment()) {
					String host = plugin.getFragmentHost();
					String version;
					Optional<UpdateSiteNSFVFSPlugin> hostPlugin = getPlugins().stream().filter(p -> p.getId().equals(host)).findFirst();
					if(hostPlugin.isPresent()) {
						version = hostPlugin.get().getVersion();
					} else {
						version = "0.0.0"; //$NON-NLS-1$
					}
					
					Element providedFragment = DOMUtil.createElement(doc, provides, "provided"); //$NON-NLS-1$
					providedFragment.setAttribute("namespace", "osgi.fragment"); //$NON-NLS-1$ //$NON-NLS-2$
					providedFragment.setAttribute("name", host); //$NON-NLS-1$
					providedFragment.setAttribute("version", version); //$NON-NLS-1$
				}
			}
			
			{
				Element requires = DOMUtil.createElement(doc, unit, "requires"); //$NON-NLS-1$
				int requiresCount = 0;
				
				for(PluginInfo info : plugin.getRequiredPlugins()) {
					Element required = DOMUtil.createElement(doc, requires, "required"); //$NON-NLS-1$
					required.setAttribute("namespace", "osgi.bundle"); //$NON-NLS-1$ //$NON-NLS-2$
					required.setAttribute("name", info.getId()); //$NON-NLS-1$
					required.setAttribute("range", info.getVersion()); //$NON-NLS-1$
					
					requiresCount++;
				}
				for(PackageInfo info : plugin.getImportedPackages()) {
					Element required = DOMUtil.createElement(doc, requires, "required"); //$NON-NLS-1$
					required.setAttribute("namespace", "java.package"); //$NON-NLS-1$ //$NON-NLS-2$
					required.setAttribute("name", info.getName()); //$NON-NLS-1$
					required.setAttribute("range", info.getVersion()); //$NON-NLS-1$
					
					requiresCount++;
				}
				
				requires.setAttribute("size", StringUtil.toString(requiresCount)); //$NON-NLS-1$
			}
			
			{
				Element artifacts = DOMUtil.createElement(doc, unit, "artifacts"); //$NON-NLS-1$
				artifacts.setAttribute("size", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element artifact = DOMUtil.createElement(doc, artifacts, "artifact"); //$NON-NLS-1$
				artifact.setAttribute("classifier", "osgi.bundle"); //$NON-NLS-1$ //$NON-NLS-2$
				artifact.setAttribute("id", plugin.getId()); //$NON-NLS-1$
				artifact.setAttribute("version", plugin.getVersion()); //$NON-NLS-1$
			}
			
			{
				Element touchpoint = DOMUtil.createElement(doc, unit, "touchpoint"); //$NON-NLS-1$
				touchpoint.setAttribute("id", "org.eclipse.equinox.p2.osgi"); //$NON-NLS-1$ //$NON-NLS-2$
				touchpoint.setAttribute("version", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			{
				Element touchpointData = DOMUtil.createElement(doc, unit, "touchpointData"); //$NON-NLS-1$
				touchpointData.setAttribute("size", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element instructions = DOMUtil.createElement(doc, touchpointData, "instructions"); //$NON-NLS-1$
				instructions.setAttribute("size", "2"); //$NON-NLS-1$ //$NON-NLS-2$
				
				Element instZipped = DOMUtil.createElement(doc, instructions, "instruction"); //$NON-NLS-1$
				instZipped.setAttribute("key", "zipped"); //$NON-NLS-1$ //$NON-NLS-2$
				instZipped.setTextContent("false"); //$NON-NLS-1$
				
				Element instManifest = DOMUtil.createElement(doc, instructions, "manifest"); //$NON-NLS-1$
				instManifest.setAttribute("key", "manifest"); //$NON-NLS-1$ //$NON-NLS-2$
				// TODO trim this down?
				instManifest.setTextContent(plugin.getManifestContent());
			}
		}
		
		return new XMLDocumentVFSFile(this, this.name + VFS.SEPARATOR + "content.xml", doc); //$NON-NLS-1$
	}
}
