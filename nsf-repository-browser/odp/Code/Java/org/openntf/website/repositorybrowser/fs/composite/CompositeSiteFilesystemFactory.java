package org.openntf.website.repositorybrowser.fs.composite;

import java.util.stream.Stream;

import org.openntf.website.repositorybrowser.fs.FilesystemFactory;

import com.ibm.commons.vfs.VFS;

/**
 * This factory provides a single VFS instance to represent virtual compositeArtifacts
 * and compositeContent files in the filesystem root.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class CompositeSiteFilesystemFactory implements FilesystemFactory {

	@Override
	public Stream<VFS> getFilesystems() {
		return Stream.of(CompositeSiteVFS.INSTANCE);
	}

}
