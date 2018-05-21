package org.openntf.website.repositorybrowser.bo;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import org.openntf.website.repositorybrowser.Constants;

/**
 * Represents a repository file or directory entry from the filesystem.
 * 
 * @since 1.0
 * @author Christian Guedemann
 */
public class FileEntry implements Serializable, Comparable<FileEntry> {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private boolean folder;
	private String href;
	
	/**
	 * Constructs a file entry from the provided Java NIO Path object, with the given
	 * base path.
	 * 
	 * @param file the {@link Path} representing the filesystem file
	 * @param currentPath the base URL {@link Path}
	 * @return a newly-constructed {@code FileEntry}
	 * @since 1.1
	 */
	public static FileEntry fromPath(Path path, Path currentPath) {
		
		Path relativePath = Constants.REPOSITORY_BASE_DIR.relativize(path);
		
		FileEntry entry = new FileEntry();
		if (Files.isDirectory(path)) {
			entry.folder = true;
			entry.href = "/home.xsp?path=" + relativePath.toString().replace(File.separatorChar, '/') + "/";
		} else {
			entry.folder = false;
			entry.href = "/.ibmxspres/domino/repository/" + relativePath.toString().replace(File.separatorChar, '/');
		}
		entry.name = path.getFileName().toString();
		return entry;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isFolder() {
		return folder;
	}
	public void setFolder(boolean isFolder) {
		this.folder = isFolder;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getHref() {
		return href;
	}

	protected String getSortableName() {
		return this.folder ? "AAA":"ZZZ" + this.name;
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
