package org.openntf.website.repositorybrowser.api;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openntf.website.repositorybrowser.bo.FileEntry;

import com.ibm.xsp.extlib.util.ExtLibUtil;

public class ContentViewFacade implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String path = "";

	public String getPath() {
		return path;
	}

	public void initPath(String path) {
		this.path = path;
	}

	public List<FileEntry> getEntries() {
		List<FileEntry> entries = new ArrayList<FileEntry>();
		try {
			String dataDir = ExtLibUtil.getCurrentSession().getEnvironmentString("directory", true);
			dataDir = dataDir + "/domino/html/repository/" + path;
			File file = new File(dataDir);
			for (File currentFile : file.listFiles()) {
				FileEntry entry = FileEntry.buildEntryFromFile(currentFile, path);
				entries.add(entry);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collections.sort(entries);
		return entries;
	}
	
	public String backURL() {
		if (path.equals("")) {
			return "";
		}
		String[] pathElements = path.split("/");
		StringBuilder sb = new StringBuilder("/home.xsp?path=");
		for (int counter = 1; counter < pathElements.length; counter++) {
			sb.append(pathElements[counter-1] +"/");
		}
		return sb.toString();
	}
}
