/**
 * Copyright (c) 2016-2024 Christian Guedemann, Jesse Gallagher
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.openntf.website.repositorybrowser.fs.MimeTypeProvider;
import org.w3c.dom.Document;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;
import com.ibm.commons.xml.DOMUtil;
import com.ibm.commons.xml.Format;
import com.ibm.commons.xml.XMLException;

/**
 * An in-memory implementation of a {@link VFSFile} backed by a {@link Document}.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class XMLDocumentVFSFile extends VFSFile implements MimeTypeProvider {
	private final byte[] content;
	private long lastModificationDate;
	
	public XMLDocumentVFSFile(VFS vfs, String name, Document xmlDoc, long lastModificationDate) throws XMLException, IOException {
		super(vfs, name);
		this.lastModificationDate = lastModificationDate;
		
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			DOMUtil.serialize(baos, xmlDoc, Format.defaultFormat);
			this.content = baos.toByteArray();
		}
	}

	@Override
	protected InputStream doGetInputStream() throws VFSException {
		return new ByteArrayInputStream(content);
	}

	@Override
	protected OutputStream doGetOutputStream(boolean arg0) throws VFSException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected long doGetSize() throws VFSException {
		return content.length;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List doDelete() throws VFSException {
		throw new UnsupportedOperationException();
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
		return "text/xml"; //$NON-NLS-1$
	}
}
