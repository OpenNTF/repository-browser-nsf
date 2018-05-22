package org.openntf.website.repositorybrowser.fs.usnsf;

import java.io.File;
import java.util.List;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFolder;

public class UpdateSiteNSFVFSFolder extends VFSFolder {

	protected UpdateSiteNSFVFSFolder(VFS vfs, String name) {
		super(vfs, name);
	}

	@Override
	protected boolean doMkDir() throws VFSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected List doDelete() throws VFSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean doExists() throws VFSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected long doGetLastModificationDate() throws VFSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected File doGetSystemFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean doIsReadOnly() throws VFSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String doRename(String arg0) throws VFSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void doSetLastModificationDate(long arg0) throws VFSException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getPeer() {
		// TODO Auto-generated method stub
		return null;
	}

}
