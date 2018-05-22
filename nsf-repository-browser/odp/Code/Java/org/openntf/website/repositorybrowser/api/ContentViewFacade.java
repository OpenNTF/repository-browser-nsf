/**
 * Copyright © 2018 Christian Güdemann, Jesse Gallagher
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openntf.website.repositorybrowser.Constants;
import org.openntf.website.repositorybrowser.bo.FileEntry;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.util.io.StreamUtil;
import com.ibm.xsp.component.UIViewRootEx2;
import com.ibm.xsp.extlib.util.ExtLibUtil;
import com.ibm.xsp.webapp.XspHttpServletResponse;

public class ContentViewFacade implements Serializable {
	private static final long serialVersionUID = 1L;

	private String path = StringUtil.EMPTY_STRING;
	
	public void init() {
		// First, check the path info
		FacesContext facesContext = FacesContext.getCurrentInstance();
		String pathInfo = facesContext.getExternalContext().getRequestPathInfo();
		if(StringUtil.isNotEmpty(pathInfo)) {
			// Then use the provided extra path, minus the leading "/"
			this.path = pathInfo.substring(1);
		} else {
			// Otherwise, fall back to the "path" query param
			@SuppressWarnings("unchecked")
			Map<String, String> param = (Map<String, String>)ExtLibUtil.resolveVariable("param");
			this.path = param.get("path");
		}
	}
	
	public void beforeRenderResponse() throws IOException {
		Path file = getDataPath();
		
		// If it's a directory, let the XPage handle rendering
		if(Files.isDirectory(file)) {
			return;
		}

		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpServletRequest req = (HttpServletRequest)facesContext.getExternalContext().getRequest();
		boolean isHead = "HEAD".equals(req.getMethod());
		XspHttpServletResponse res = (XspHttpServletResponse)facesContext.getExternalContext().getResponse();

		try(ServletOutputStream os = res.getOutputStream()) {
			
			// If it's not a regular file, throw a 404
			if(!Files.isRegularFile(file)) {
				res.setStatus(HttpServletResponse.SC_NOT_FOUND);
				res.setContentType("text/plain");
				String message = "Not found: " + path;
				res.setContentLength(message.length());
				if(!isHead) {
					os.print(message);
				}
			} else {
				// If all is good, stream the file
			
				long length = Files.size(file);
				if(length > Integer.MAX_VALUE) {
					throw new IllegalStateException("File is too large to stream: " + path);
				}
				
				res.setContentLength((int)length);
				res.setContentType(Files.probeContentType(file));
				
				if(!isHead) {
					try(InputStream is = Files.newInputStream(file)) {
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
	 * @return {@code true} if the path provided to the page context is
	 *        a regular file; {@code false} otherwise
	 */
	public boolean isPathDirectory() {
		Path dataPath = getDataPath();
		return Files.isDirectory(dataPath);
	}

	public List<FileEntry> getEntries() throws Exception {
		Path dataPath = getDataPath();
				
		return Files.list(dataPath)
			.map(p -> FileEntry.fromPath(p, Paths.get(path)))
			.sorted()
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
	
	private Path getDataPath() {
		Path dataPath = StringUtil.isNotEmpty(path) ?
				Constants.REPOSITORY_BASE_DIR.resolve(path) :
				Constants.REPOSITORY_BASE_DIR;
		
		if(!dataPath.startsWith(Constants.REPOSITORY_BASE_DIR)) {
			throw new IllegalArgumentException("Path is not within the repository root: " + path);
		}
		
		return dataPath;
	}
}
