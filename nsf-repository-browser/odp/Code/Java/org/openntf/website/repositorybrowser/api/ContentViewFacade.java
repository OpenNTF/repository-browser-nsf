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
package org.openntf.website.repositorybrowser.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openntf.website.repositorybrowser.Constants;
import org.openntf.website.repositorybrowser.fs.MimeTypeProvider;
import org.openntf.website.repositorybrowser.fs.VFSResourceComparator;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.util.io.StreamUtil;
import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;
import com.ibm.commons.vfs.VFSFolder;
import com.ibm.commons.vfs.VFSResource;
import com.ibm.commons.xml.XMLException;
import com.ibm.xsp.component.UIViewRootEx2;
import com.ibm.xsp.designer.context.XSPContext;
import com.ibm.xsp.extlib.util.ExtLibUtil;
import com.ibm.xsp.webapp.XspHttpServletResponse;

public class ContentViewFacade implements Serializable {
	private static final long serialVersionUID = 1L;

	private String path = StringUtil.EMPTY_STRING;
	private boolean redirecting = false;
	
	public void init() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		HttpServletRequest req = (HttpServletRequest)externalContext.getRequest();
		HttpServletResponse res = (HttpServletResponse)externalContext.getResponse();
		
		// Make sure the URL contains home.xsp, for Eclipse's sake
		XSPContext context = XSPContext.getXSPContext(facesContext);
		String url = context.getUrl().toString();
		String requestUrl = req.getRequestURL().toString();
		if(!requestUrl.startsWith(url)) {
			res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			res.addHeader("Location", url);
			redirecting = true;
			return;
		}
		
		// First, check the path info
		String pathInfo = externalContext.getRequestPathInfo();
		if(StringUtil.isNotEmpty(pathInfo)) {
			// Then use the provided extra path, minus the leading "/"
			this.path = pathInfo.substring(1);
		} else {
			// Otherwise, fall back to the "path" query param
			@SuppressWarnings("unchecked")
			Map<String, String> param = (Map<String, String>)ExtLibUtil.resolveVariable("param");
			this.path = StringUtil.toString(param.get("path"));
		}
	}
	
	public void beforeRenderResponse() throws IOException, XMLException, VFSException {
		VFSResource file = getResource();
		
		// If it's a directory, let the XPage handle rendering
		if(file.isFolder()) {
			return;
		}

		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpServletRequest req = (HttpServletRequest)facesContext.getExternalContext().getRequest();
		boolean isHead = "HEAD".equals(req.getMethod());
		XspHttpServletResponse res = (XspHttpServletResponse)facesContext.getExternalContext().getResponse();

		try(ServletOutputStream os = res.getOutputStream()) {
			if(!file.isFile() || !file.exists()) {
				// If it's not a regular file, throw a 404
				res.setStatus(HttpServletResponse.SC_NOT_FOUND);
				res.setContentType("text/plain");
				String message = "Not found: " + path;
				res.setContentLength(message.length());
				if(!isHead) {
					os.print(message);
				}
			} else {
				// If all is good, stream the file
			
				VFSFile vfsFile = (VFSFile)file;
				long length = vfsFile.getSize();
				if(length > Integer.MAX_VALUE) {
					throw new IllegalStateException("File is too large to stream: " + path);
				}
				res.setContentLength((int)length);
				
				String mimeType;
				if(vfsFile instanceof MimeTypeProvider) {
					mimeType = ((MimeTypeProvider)vfsFile).getMimeType();
				} else {
					File systemFile = vfsFile.getSystemFile();
					if(systemFile != null) {
						Path systemPath = systemFile.toPath();
						mimeType = Files.probeContentType(systemPath);
					} else {
						mimeType = "application/octet-stream";
					}
				}
				res.setContentType(mimeType);
				
				if(!isHead) {
					try(InputStream is = vfsFile.getInputStream()) {
						StreamUtil.copyStream(is, os);
					}
				}
			}
		} finally {
			facesContext.responseComplete();
		}
	}

	public String getPath() {
		return path;
	}
	
	/**
	 * @return {@code true} if the XPage should render a
	 *        directory listing; {@code false} otherwise
	 */
	public boolean isPageRendered() {
		if(redirecting) {
			return false;
		}
		if(StringUtil.isEmpty(path)) {
			return true;
		}
		
		VFSResource res = getResource();
		return res.isFolder();
	}

	public List<VFSResource> getEntries() throws Exception {
		return Constants.getFilesystems()
			.map(vfs -> vfs.getFolder(path))
			.map(ContentViewFacade::findResources)
			.map(Collection::stream)
			.flatMap(Function.identity())
			.sorted(VFSResourceComparator.INSTANCE)
			.collect(Collectors.toList());
	}
	
	public String getBackURL() {
		if (StringUtil.isEmpty(path)) {
			return StringUtil.EMPTY_STRING;
		}
		UIViewRootEx2 viewRoot = (UIViewRootEx2)FacesContext.getCurrentInstance().getViewRoot();
		String pageName = viewRoot.getPageName();
		
		String[] pathElements = path.split("/"); //$NON-NLS-1$
		
		return pageName + "/" + //$NON-NLS-1$
			Arrays.stream(Arrays.copyOfRange(pathElements, 0, pathElements.length-1))
			.collect(Collectors.joining("/")); //$NON-NLS-1$
	}
	
	public String getDirectLink() {
		return "/.ibmxspres/domino/" + Constants.REPOSITORY_BASE + "/" + path + "/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	// *******************************************************************************
	// * Internal utility methods
	// *******************************************************************************

	private VFSResource getResource() {
		return Constants.getFilesystems()
			.map(vfs -> getResource(vfs, path))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * A type-checked and exception-squashed wrapper for {@link VFSFolder#findResources()}
	 */
	@SuppressWarnings("unchecked")
	private static List<VFSResource> findResources(VFSFolder folder) {
		try {
			return (List<VFSResource>)folder.findResources();
		} catch (VFSException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * A type-checked and exception-squashed wrapper for {@link VFSFolder#findResource()}
	 */
	private static VFSResource getResource(VFS vfs, String path) {
		try {
			VFSFolder folder = vfs.getRoot();
			VFSResource res = folder;
			for(String pathBit : StringUtil.splitString(path, '/')) {
				if(StringUtil.isNotEmpty(path)) {
					res = folder.findResource(pathBit, false);
					if(res != null && res.isFolder()) {
						folder = (VFSFolder)res;
					}
				}
			}
			
			return res;
		} catch (VFSException e) {
			throw new RuntimeException(e);
		}
	}
}
