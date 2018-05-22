package org.openntf.website.repositorybrowser.fs.usnsf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;
import com.ibm.commons.vfs.VFSFile;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import util.TempFileInputStream;
import util.Utils;

public class UpdateSiteNSFVFSFile extends VFSFile {
	public static final String FORM_FEATURE = "fmFeature"; //$NON-NLS-1$
	public static final String FORM_PLUGIN = "fmPlugin"; //$NON-NLS-1$
	
	public static final String ITEM_FILE_FEATURE = "feature.file"; //$NON-NLS-1$
	public static final String ITEM_FILE_PLUGIN = "plugin.file"; //$NON-NLS-1$
	
	public static final String ITEM_MOD_FEATURE = "feature.file.lastModified"; //$NON-NLS-1$
	public static final String ITEM_MOD_PLUGIN = "plugin.file.lastModified"; //$NON-NLS-1$
	
	public static final String ITEM_ID_FEATURE = "feature.id"; //$NON-NLS-1$
	
	private long lastModificationDate;
	private final Document doc;

	protected UpdateSiteNSFVFSFile(VFS vfs, String name, Document doc) {
		super(vfs, name);
		this.doc = doc;
	}

	@Override
	protected InputStream doGetInputStream() throws VFSException {
		try {
			EmbeddedObject obj = getFile(doc);
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
			EmbeddedObject obj = getFile(doc);
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
	
	long getDocLastModified() {
		try {
			String form = doc.getItemValueString("Form"); //$NON-NLS-1$
			String itemName;
			switch(form) {
			case FORM_FEATURE:
				itemName = ITEM_MOD_FEATURE;
				break;
			case FORM_PLUGIN:
				itemName = ITEM_MOD_PLUGIN;
				break;
			default:
				throw new IllegalArgumentException("Unknown document type: " + form); //$NON-NLS-1$
			}
			
			String modString = doc.getItemValueString(itemName);
			return Long.parseLong(modString, 10);
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}

	// *******************************************************************************
	// * Internal implementation methods
	// *******************************************************************************
	
	private static EmbeddedObject getFile(Document doc) throws NotesException {
		String form = doc.getItemValueString("Form"); //$NON-NLS-1$
		String itemName;
		switch(form) {
		case FORM_FEATURE:
			itemName = ITEM_FILE_FEATURE;
			break;
		case FORM_PLUGIN:
			itemName = ITEM_FILE_PLUGIN;
			break;
		default:
			throw new IllegalArgumentException("Unknown document type: " + form); //$NON-NLS-1$
		}
		RichTextItem body = (RichTextItem)doc.getFirstItem(itemName);
		return (EmbeddedObject)body.getEmbeddedObjects().get(0);
	}
}
