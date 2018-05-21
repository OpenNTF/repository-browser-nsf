package org.openntf.website.repositorybrowser.bo;

import java.io.File;
import java.io.Serializable;

public class FileEntry implements Serializable, Comparable<FileEntry> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String icon;
	private boolean isFolder;
	private String href;
	
	public static FileEntry buildEntryFromFile(File file, String currentPath) {
		FileEntry entry = new FileEntry();
		if (file.isDirectory()) {
			entry.icon = "/folder.png";
			entry.isFolder = true;
			entry.href ="/home.xsp?path="+currentPath + file.getName() +"/";
		} else {
			entry.icon = "/page.png";
			entry.isFolder = false;
			entry.href = "../repository/"+currentPath + file.getName();
		}
		entry.name = file.getName();
		return entry;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public boolean isFolder() {
		return isFolder;
	}
	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getHref() {
		return href;
	}

	protected String getSortableName() {
		return this.isFolder ? "AAA":"ZZZ" + this.name;
	}
	
	public int compareTo(FileEntry o) {
		String thisName = getSortableName();
		String otherName = o.getSortableName();
		return thisName.compareTo(otherName);
	}

	@Override
	public int hashCode() {
		return getSortableName().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof FileEntry && getSortableName().equals(((FileEntry)obj).getSortableName());
	}
}
