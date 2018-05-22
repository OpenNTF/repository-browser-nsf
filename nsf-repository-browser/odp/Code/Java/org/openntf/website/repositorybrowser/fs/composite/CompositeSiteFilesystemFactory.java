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
package org.openntf.website.repositorybrowser.fs.composite;

import java.util.stream.Stream;

import org.openntf.website.repositorybrowser.fs.FilesystemFactory;

import com.ibm.commons.vfs.VFS;

/**
 * This factory provides a single VFS instance to represent virtual compositeArtifacts
 * and compositeContent files in the filesystem root.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class CompositeSiteFilesystemFactory implements FilesystemFactory {

	@Override
	public Stream<VFS> getFilesystems() {
		return Stream.of(CompositeSiteVFS.INSTANCE);
	}

}
