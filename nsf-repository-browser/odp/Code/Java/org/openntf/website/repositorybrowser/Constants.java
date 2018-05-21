package org.openntf.website.repositorybrowser;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.ibm.xsp.extlib.util.ExtLibUtil;

import lotus.domino.NotesException;

public enum Constants {
	;
	
	public static final String REPOSITORY_BASE = "repository";
	public static final Path REPOSITORY_BASE_DIR;
	
	static {
		try {
			String dataDir = ExtLibUtil.getCurrentSession().getEnvironmentString("directory", true);
			REPOSITORY_BASE_DIR = Paths.get(dataDir, "domino", "html", REPOSITORY_BASE);
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}
}
