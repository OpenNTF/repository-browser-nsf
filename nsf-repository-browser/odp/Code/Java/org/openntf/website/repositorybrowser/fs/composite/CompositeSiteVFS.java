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
package org.openntf.website.repositorybrowser.fs.composite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.openntf.website.repositorybrowser.Constants;
import org.openntf.website.repositorybrowser.fs.mem.XMLDocumentVFSFile;
import org.openntf.website.repositorybrowser.fs.mem.MemoryVFSFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;
import com.ibm.commons.vfs.VFSFilter.IFilter;
import com.ibm.commons.vfs.VFSFolder;
import com.ibm.commons.vfs.VFSResource;
import com.ibm.commons.xml.DOMUtil;
import com.ibm.commons.xml.XMLException;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import beans.Translation;

/**
 * Virtual filesystem to represent the "compositeContent" and "compositeArtifacts"
 * files at the root of the repository.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
class CompositeSiteVFS extends VFS {
	public static final CompositeSiteVFS INSTANCE = new CompositeSiteVFS();
	
	@Override
	protected boolean doIsReadonly() throws VFSException {
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doReadEntries(VFS vfs, String path, List result) {
		if(StringUtil.isEmpty(path) || "/".equals(path)) {
			for(VFSFile file : getFiles()) {
				if(this.isAccepted(file)) {
					try {
						result.add(new CompositeSiteFileEntry(this, file, file.getLastModificationDate()));
					} catch (VFSException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doReadResources(String path, List result, IFilter filter) {
		if(StringUtil.isEmpty(path) || "/".equals(path)) {
			for(VFSFile file : getFiles()) {
				if(this.isAccepted(file) && (filter == null || filter.accept(file))) {
					result.add(file);
				}
			}
		}
	}
	
	@Override
	protected VFSFolder doCreateVFSFolder(String folderName) {
		return new MemoryVFSFolder(this, folderName);
	}
	
	@Override
	protected VFSFile doCreateVFSFile(String fileName) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected FolderEntry doCreateFolderEntry(VFSFolder folder) {
		try {
			return new CompositeSiteFolderEntry(this, folder, folder.getLastModificationDate());
		} catch (VFSException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected FileEntry doCreateFileEntry(VFSFile file) {
		try {
			return new CompositeSiteFileEntry(this, file, file.getLastModificationDate());
		} catch (VFSException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doClose() {
		// NOP
	}
	
	@Override
	public boolean hasEntryCache() {
		return false;
	}
	
	// *******************************************************************************
	// * Internal implementation methods
	// *******************************************************************************
	
	private static class CompositeSiteFolderEntry extends VFS.FolderEntry {
		public CompositeSiteFolderEntry(VFS vfs, VFSFolder folder, long lastModified) {
			super(vfs, folder, lastModified);
		}
	}
	
	private static class CompositeSiteFileEntry extends VFS.FileEntry {
		public CompositeSiteFileEntry(VFS vfs, VFSFile file, long lastModified) {
			super(vfs, file, lastModified);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Collection<XMLDocumentVFSFile> getFiles() {
		Map<String, Object> requestScope = ExtLibUtil.getRequestScope();
		String cacheKey = getClass().getName() + "_files"; //$NON-NLS-1$
		return (Collection<XMLDocumentVFSFile>)requestScope.computeIfAbsent(cacheKey, (key) -> {
			List<XMLDocumentVFSFile> result = new ArrayList<>();
			try {
				result.add(createCompositeContent());
				result.add(createCompositeArtifacts());
			} catch (XMLException | IOException e) {
				throw new RuntimeException(e);
			}
			return result;
		});
	}
	
	@SuppressWarnings("unchecked")
	private XMLDocumentVFSFile createCompositeContent() throws XMLException, IOException {
		Document doc = DOMUtil.createDocument();
		final long[] lastMod = new long[] { 0 };

		{
			ProcessingInstruction proc = doc.createProcessingInstruction("compositeMetadataRepository", "version='1.0.0'"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(proc);
		}

		{
			Element repository = doc.createElement("repository"); //$NON-NLS-1$
			doc.appendChild(repository);
			repository.setAttribute("name", Translation.translate("appName")); //$NON-NLS-1$ //$NON-NLS-2$
			repository.setAttribute("type", "org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository"); //$NON-NLS-1$ //$NON-NLS-2$
			repository.setAttribute("version", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$

			Element properties = DOMUtil.createElement(doc, repository, "properties"); //$NON-NLS-1$
			properties.setAttribute("size", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			{
				Element property = DOMUtil.createElement(doc, properties, "property"); //$NON-NLS-1$
				property.setAttribute("name", "p2.atomic.composite.loading"); //$NON-NLS-1$ //$NON-NLS-2$
				property.setAttribute("value", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			Element children = DOMUtil.createElement(doc, repository, "children"); //$NON-NLS-1$
			AtomicInteger count = new AtomicInteger(0);
			
			Constants.getFilesystems()
				.filter(vfs -> !(vfs instanceof CompositeSiteVFS))
				.forEach(vfs -> {
					try {
						((List<VFSFile>)vfs.getRoot().findFiles("content.jar", true)).stream() //$NON-NLS-1$
							.map(VFSResource::getParent)
							.forEach(folder -> {
								try {
									lastMod[0] = Math.max(lastMod[0], folder.getLastModificationDate());
								} catch (VFSException e) {
									throw new RuntimeException(e);
								}
								
								Element child = DOMUtil.createElement(doc, children, "child"); //$NON-NLS-1$
								child.setAttribute("location", vfs.getFolder(folder).getPath()); //$NON-NLS-1$
								count.incrementAndGet();
							});
						((List<VFSFile>)vfs.getRoot().findFiles("content.xml", true)).stream() //$NON-NLS-1$
							.map(VFSResource::getParent)
							.forEach(folder -> {
								try {
									lastMod[0] = Math.max(lastMod[0], folder.getLastModificationDate());
								} catch (VFSException e) {
									throw new RuntimeException(e);
								}
								
								Element child = DOMUtil.createElement(doc, children, "child"); //$NON-NLS-1$
								child.setAttribute("location", vfs.getFolder(folder).getPath()); //$NON-NLS-1$
								count.incrementAndGet();
							});
					} catch (VFSException e) {
						throw new RuntimeException(e);
					}
				});

			children.setAttribute("size", count.toString()); //$NON-NLS-1$
		}
		
		
		return new XMLDocumentVFSFile(this, "compositeContent.xml", doc, lastMod[0]); //$NON-NLS-1$
	}
	
	@SuppressWarnings("unchecked")
	private XMLDocumentVFSFile createCompositeArtifacts() throws XMLException, IOException {
		Document doc = DOMUtil.createDocument();
		final long[] lastMod = new long[] { 0 };

		{
			ProcessingInstruction proc = doc.createProcessingInstruction("compositeArtifactRepository", "version='1.0.0'"); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(proc);
		}

		{
			Element repository = doc.createElement("repository"); //$NON-NLS-1$
			doc.appendChild(repository);
			repository.setAttribute("name", Translation.translate("appName")); //$NON-NLS-1$ //$NON-NLS-2$
			repository.setAttribute("type", "org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository"); //$NON-NLS-1$ //$NON-NLS-2$
			repository.setAttribute("version", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$

			Element properties = DOMUtil.createElement(doc, repository, "properties"); //$NON-NLS-1$
			properties.setAttribute("size", "0"); //$NON-NLS-1$ //$NON-NLS-2$

			Element children = DOMUtil.createElement(doc, repository, "children"); //$NON-NLS-1$
			AtomicInteger count = new AtomicInteger(0);
			Constants.getFilesystems()
			.filter(vfs -> !(vfs instanceof CompositeSiteVFS))
			.forEach(vfs -> {
				try {
					((List<VFSFile>)vfs.getRoot().findFiles("artifacts.jar", true)).stream() //$NON-NLS-1$
						.map(VFSResource::getParent)
						.forEach(folder -> {
							try {
								lastMod[0] = Math.max(lastMod[0], folder.getLastModificationDate());
							} catch (VFSException e) {
								throw new RuntimeException(e);
							}
							
							Element child = DOMUtil.createElement(doc, children, "child"); //$NON-NLS-1$
							child.setAttribute("location", vfs.getFolder(folder).getPath()); //$NON-NLS-1$
							count.incrementAndGet();
						});
					((List<VFSFile>)vfs.getRoot().findFiles("artifacts.xml", true)).stream() //$NON-NLS-1$
						.map(VFSResource::getParent)
						.forEach(folder -> {
							try {
								lastMod[0] = Math.max(lastMod[0], folder.getLastModificationDate());
							} catch (VFSException e) {
								throw new RuntimeException(e);
							}
							
							Element child = DOMUtil.createElement(doc, children, "child"); //$NON-NLS-1$
							child.setAttribute("location", vfs.getFolder(folder).getPath()); //$NON-NLS-1$
							count.incrementAndGet();
						});
				} catch (VFSException e) {
					throw new RuntimeException(e);
				}
			});
			children.setAttribute("size", count.toString()); //$NON-NLS-1$
		}
		
		return new XMLDocumentVFSFile(this, "compositeArtifacts.xml", doc, lastMod[0]); //$NON-NLS-1$
	}
}
