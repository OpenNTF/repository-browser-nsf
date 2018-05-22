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
package org.openntf.website.repositorybrowser.fs.usnsf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import org.openntf.website.repositorybrowser.fs.MimeTypeProvider;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import util.TempFileInputStream;
import util.Utils;

public abstract class UpdateSiteNSFVFSFile extends VFSFile implements MimeTypeProvider {
	private long lastModificationDate;
	private final String id;
	private final String version;
	protected final Document doc;

	protected UpdateSiteNSFVFSFile(VFS vfs, String name, String id, String version, Document doc) {
		super(vfs, name);
		this.id = id;
		this.version = version;
		this.doc = doc;
	}

	@Override
	protected InputStream doGetInputStream() throws VFSException {
		try {
			EmbeddedObject obj = getFile();
			try {
				File tempFile = Files.createTempFile(Utils.getTempDirectory(), obj.getSource(), ".dat").toFile(); //$NON-NLS-1$
				tempFile.delete();
				obj.extractFile(tempFile.getAbsolutePath());
				tempFile.deleteOnExit();
				return new TempFileInputStream(tempFile);
			} finally {
				obj.recycle();
			}
		} catch(NotesException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected OutputStream doGetOutputStream(boolean arg0) throws VFSException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long doGetSize() throws VFSException {
		try {
			EmbeddedObject obj = getFile();
			try {
				return obj.getFileSize();
			} finally {
				obj.recycle();
			}
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List doDelete() throws VFSException {
		return null;
	}

	@Override
	protected boolean doExists() throws VFSException {
		return true;
	}

	@Override
	protected long doGetLastModificationDate() throws VFSException {
		return lastModificationDate;
	}

	@Override
	protected File doGetSystemFile() {
		// Not applicable
		return null;
	}

	@Override
	protected boolean doIsReadOnly() throws VFSException {
		return true;
	}

	@Override
	protected String doRename(String newName) throws VFSException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doSetLastModificationDate(long lastModificationDate) throws VFSException {
		this.lastModificationDate = lastModificationDate;
	}

	@Override
	public Object getPeer() {
		return null;
	}
	
	@Override
	public String getMimeType() {
		return "application/java-archive"; //$NON-NLS-1$
	}
	
	public abstract long getDocLastModified();
	
	// *******************************************************************************
	// * p2 metadata access
	// *******************************************************************************
	
	public String getId() {
		return id;
	}
	
	public String getVersion() {
		return version;
	}

	// *******************************************************************************
	// * Internal implementation methods
	// *******************************************************************************
	
	protected abstract EmbeddedObject getFile() throws NotesException;
}
