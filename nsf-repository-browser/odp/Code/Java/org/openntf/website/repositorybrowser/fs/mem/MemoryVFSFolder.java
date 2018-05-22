package org.openntf.website.repositorybrowser.fs.mem;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSFolder;

/**
 * An read-only stub implementation of {@link VFSFoler}.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class MemoryVFSFolder extends VFSFolder {

	private long lastModificationDate;
	
	public MemoryVFSFolder(VFS vfs, String path) {
		super(vfs, path);
	}

	@Override
	protected boolean doMkDir() {
		return false;
	}

	@Override
	protected List<?> doDelete() {
		return Collections.emptyList();
	}

	@Override
	protected boolean doExists() {
		return getPath().isEmpty();
	}

	@Override
	protected long doGetLastModificationDate() {
		return lastModificationDate;
	}

	@Override
	protected File doGetSystemFile() {
		// Not applicable
		return null;
	}

	@Override
	protected boolean doIsReadOnly() {
		return true;
	}

	@Override
	protected String doRename(String newName) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doSetLastModificationDate(long lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

	@Override
	public Object getPeer() {
		// I assume this is to get the native representation
		return null;
	}
	

}
