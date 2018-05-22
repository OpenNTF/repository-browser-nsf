package org.openntf.website.repositorybrowser.fs;

import java.util.stream.Stream;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;

/**
 * Represents a factory class able to produce abstract filesystem representations
 * for use in repository browsing.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public interface FilesystemFactory {
	public static final String EXTENSION_POINT = FilesystemFactory.class.getName();
	
	/**
	 * Returns a stream of virtualized filesystems.
	 * 
	 * <p>Implementation note: for stream usage, implementing classes should rethrow
	 * {@link VFSException}s as {@link RuntimeException}s.
	 * 
	 * @return a {@link Stream} of {@link VFS} objects
	 */
	Stream<VFS> getFilesystems();
}
