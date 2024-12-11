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
package org.openntf.website.repositorybrowser.fs;

import java.util.stream.Stream;

import com.ibm.commons.vfs.VFS;
import com.ibm.commons.vfs.VFSException;

/**
 * Represents a factory class able to produce abstract filesystem representations
 * for use in repository browsing.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public interface FilesystemFactory {
	public static final String EXTENSION_POINT = FilesystemFactory.class.getName();
	
	/**
	 * Returns a stream of virtualized filesystems.
	 * 
	 * <p>Implementation note: for stream usage, implementing classes should rethrow
	 * {@link VFSException}s as {@link RuntimeException}s.
	 * 
	 * @return a {@link Stream} of {@link VFS} objects
	 */
	Stream<VFS> getFilesystems();
}
