package org.openntf.website.repositorybrowser.fs;

import java.util.stream.Stream;

import org.openntf.website.repositorybrowser.Constants;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.filesystem.FileVFS;

public class LocalFilesystemFactory implements FilesystemFactory {

	@Override
	public Stream<VFS> getFilesystems() {
		try {
			return Stream.of(
				new FileVFS(Constants.REPOSITORY_BASE_DIR.toString())
			);
		} catch (VFSException e) {
			throw new RuntimeException(e);
		}
	}

}
