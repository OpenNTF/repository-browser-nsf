package org.openntf.website.repositorybrowser.fs;

/**
 * An extension interface for VFS file classes to provide MIME types
 * in lieu of using a system file.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public interface MimeTypeProvider {
	String getMimeType();
}
