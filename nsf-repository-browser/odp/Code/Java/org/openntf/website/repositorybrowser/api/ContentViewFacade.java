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

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.openntf.website.repositorybrowser.Constants;
import org.openntf.website.repositorybrowser.bo.FileEntry;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.component.UIViewRootEx2;

public class ContentViewFacade implements Serializable {
	private static final long serialVersionUID = 1L;

	private String path = StringUtil.EMPTY_STRING;

	public String getPath() {
		return path;
	}

	public void initPath(String path) {
		this.path = path;
	}

	public List<FileEntry> getEntries() throws Exception {
		Path dataPath = StringUtil.isNotEmpty(path) ?
				Constants.REPOSITORY_BASE_DIR.resolve(path) :
				Constants.REPOSITORY_BASE_DIR;
		
		if(!dataPath.startsWith(Constants.REPOSITORY_BASE_DIR)) {
			throw new IllegalArgumentException("Path is not within the repository root: " + path);
		}
				
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
		
		return pageName + "?path=" + //$NON-NLS-1$
			Arrays.stream(Arrays.copyOfRange(pathElements, 0, pathElements.length-1))
			.collect(Collectors.joining("/")); //$NON-NLS-1$
	}
	
	public String getDirectLink() {
		return "/.ibmxspres/domino/" + Constants.REPOSITORY_BASE + "/" + path + "/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
