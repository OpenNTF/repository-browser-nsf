package org.openntf.website.repositorybrowser.fs.composite;

import java.util.stream.Stream;

import org.openntf.website.repositorybrowser.fs.FilesystemFactory;

import com.ibm.commons.vfs.VFS;

public class CompositeSiteFilesystemFactory implements FilesystemFactory {

	@Override
	public Stream<VFS> getFilesystems() {
		return Stream.of(CompositeSiteVFS.INSTANCE);
	}

}
