/**
 * Copyright (c) 2018 Christian Guedemann, Jesse Gallagher
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
