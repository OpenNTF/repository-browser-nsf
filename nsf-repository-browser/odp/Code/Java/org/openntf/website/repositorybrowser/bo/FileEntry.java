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
			entry.href = "/home.xsp?path=" + relativePath.toString().replace(File.separatorChar, '/') + "/"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			entry.folder = false;
			entry.href = "/.ibmxspres/domino/repository/" + relativePath.toString().replace(File.separatorChar, '/'); //$NON-NLS-1$
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
		return this.folder ? "AAA":"ZZZ" + this.name; //$NON-NLS-1$ //$NON-NLS-2$
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
