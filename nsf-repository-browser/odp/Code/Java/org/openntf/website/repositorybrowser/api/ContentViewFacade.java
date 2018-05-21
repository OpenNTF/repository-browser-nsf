package org.openntf.website.repositorybrowser.api;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
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

	private String path = "";

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
				
		return Files.list(dataPath)
			.map(p -> FileEntry.fromPath(p, Paths.get(path)))
			.sorted()
			.collect(Collectors.toList());
	}
	
	public String getBackURL() {
		if (path.equals("")) {
			return "";
		}
		UIViewRootEx2 viewRoot = (UIViewRootEx2)FacesContext.getCurrentInstance().getViewRoot();
		String pageName = viewRoot.getPageName();
		
		String[] pathElements = path.split("/");
		
		return pageName + "?path=" +
			Arrays.stream(Arrays.copyOfRange(pathElements, 0, pathElements.length-1))
			.collect(Collectors.joining("/"));
	}
	
	public String getDirectLink() {
		return "/.ibmxspres/domino/" + Constants.REPOSITORY_BASE + "/" + path + "/";
	}
}
