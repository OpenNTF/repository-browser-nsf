package org.openntf.website.repositorybrowser.fs.composite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.openntf.website.repositorybrowser.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

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
		for(VFSFile file : getFiles()) {
			if(this.isAccepted(file)) {
				result.add(new CompositeSiteFileEntry(this, file, System.currentTimeMillis()));
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void doReadResources(String path, List result, IFilter filter) {
		for(VFSFile file : getFiles()) {
			if(this.isAccepted(file) && (filter == null || filter.accept(file))) {
				result.add(file);
			}
		}
	}
	
	@Override
	protected VFSFolder doCreateVFSFolder(String folderName) {
		return new CompositeSiteVFSFolder(this, folderName);
	}
	
	@Override
	protected VFSFile doCreateVFSFile(String fileName) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected FolderEntry doCreateFolderEntry(VFSFolder folder) {
		return new CompositeSiteFolderEntry(this, folder, System.currentTimeMillis());
	}
	
	@Override
	protected FileEntry doCreateFileEntry(VFSFile file) {
		return new CompositeSiteFileEntry(this, file, System.currentTimeMillis());
	}

	@Override
	protected void doClose() {
		// NOP
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
	private Collection<CompositeSiteVFSFile> getFiles() {
		Map<String, Object> requestScope = ExtLibUtil.getRequestScope();
		String cacheKey = getClass().getName() + "_files";
		return (Collection<CompositeSiteVFSFile>)requestScope.computeIfAbsent(cacheKey, (key) -> {
			List<CompositeSiteVFSFile> result = new ArrayList<>();
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
	private CompositeSiteVFSFile createCompositeContent() throws XMLException, IOException {
		Document doc = DOMUtil.createDocument();

		{
			ProcessingInstruction proc = doc.createProcessingInstruction("compositeMetadataRepository", "version='1.0.0'");
			doc.appendChild(proc);
		}

		{
			Element repository = doc.createElement("repository");
			doc.appendChild(repository);
			repository.setAttribute("name", Translation.translate("appName"));
			repository.setAttribute("type", "org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository");
			repository.setAttribute("version", "1.0.0");

			Element properties = DOMUtil.createElement(doc, repository, "properties");
			properties.setAttribute("size", "1");
			{
				Element property = DOMUtil.createElement(doc, properties, "property");
				property.setAttribute("name", "p2.atomic.composite.loading");
				property.setAttribute("value", "true");
			}

			Element children = DOMUtil.createElement(doc, repository, "children");
			AtomicInteger count = new AtomicInteger(0);
			
			Constants.getFilesystems()
				.filter(vfs -> !(vfs instanceof CompositeSiteVFS))
				.forEach(vfs -> {
					try {
						((List<VFSFile>)vfs.getRoot().findFiles("content.jar", true)).stream()
							.map(VFSResource::getParent)
							.forEach(folder -> {
								Element child = DOMUtil.createElement(doc, children, "child");
								child.setAttribute("location", vfs.getFolder(folder).getPath());
								count.incrementAndGet();
							});
						((List<VFSFile>)vfs.getRoot().findFiles("content.xml", true)).stream()
							.map(VFSResource::getParent)
							.forEach(folder -> {
								Element child = DOMUtil.createElement(doc, children, "child");
								child.setAttribute("location", vfs.getFolder(folder).getPath());
								count.incrementAndGet();
							});
					} catch (VFSException e) {
						throw new RuntimeException(e);
					}
				});

			children.setAttribute("size", count.toString());
		}
		
		
		return new CompositeSiteVFSFile(this, "compositeContent.xml", doc);
	}
	
	@SuppressWarnings("unchecked")
	private CompositeSiteVFSFile createCompositeArtifacts() throws XMLException, IOException {
		Document doc = DOMUtil.createDocument();

		{
			ProcessingInstruction proc = doc.createProcessingInstruction("compositeArtifactRepository", "version='1.0.0'");
			doc.appendChild(proc);
		}

		{
			Element repository = doc.createElement("repository");
			doc.appendChild(repository);
			repository.setAttribute("name", Translation.translate("appName"));
			repository.setAttribute("type", "org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository");
			repository.setAttribute("version", "1.0.0");

			Element properties = DOMUtil.createElement(doc, repository, "properties");
			properties.setAttribute("size", "0");

			Element children = DOMUtil.createElement(doc, repository, "children");
			AtomicInteger count = new AtomicInteger(0);
			Constants.getFilesystems()
			.filter(vfs -> !(vfs instanceof CompositeSiteVFS))
			.forEach(vfs -> {
				try {
					((List<VFSFile>)vfs.getRoot().findFiles("artifacts.jar", true)).stream()
						.map(VFSResource::getParent)
						.forEach(folder -> {
							Element child = DOMUtil.createElement(doc, children, "child");
							child.setAttribute("location", vfs.getFolder(folder).getPath());
							count.incrementAndGet();
						});
					((List<VFSFile>)vfs.getRoot().findFiles("artifacts.xml", true)).stream()
						.map(VFSResource::getParent)
						.forEach(folder -> {
							Element child = DOMUtil.createElement(doc, children, "child");
							child.setAttribute("location", vfs.getFolder(folder).getPath());
							count.incrementAndGet();
						});
				} catch (VFSException e) {
					throw new RuntimeException(e);
				}
			});
			children.setAttribute("size", count.toString());
		}
		
		return new CompositeSiteVFSFile(this, "compositeArtifacts.xml", doc);
	}
}
