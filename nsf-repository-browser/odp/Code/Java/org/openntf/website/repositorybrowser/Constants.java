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
package org.openntf.website.repositorybrowser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openntf.website.repositorybrowser.fs.FilesystemFactory;

import com.ibm.commons.vfs.VFS;
import com.ibm.xsp.application.ApplicationEx;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import lotus.domino.NotesException;

public enum Constants {
	;
	
	public static final String REPOSITORY_BASE = "repository"; //$NON-NLS-1$
	public static final Path REPOSITORY_BASE_DIR;
	
	static {
		try {
			String dataDir = ExtLibUtil.getCurrentSession().getEnvironmentString("directory", true); //$NON-NLS-1$
			REPOSITORY_BASE_DIR = Paths.get(dataDir, "domino", "html", REPOSITORY_BASE); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Stream<VFS> getFilesystems() {
		Map<String, Object> requestScope = ExtLibUtil.getRequestScope();
		String cacheKey = Constants.class.getName() + "_filesystems";
		return ((List<VFS>)requestScope.computeIfAbsent(cacheKey, (key) -> {
			ApplicationEx app = ApplicationEx.getInstance();
			List<FilesystemFactory> factories = (List<FilesystemFactory>)app.findServices(FilesystemFactory.EXTENSION_POINT);
			return factories.stream()
				.map(FilesystemFactory::getFilesystems)
				.flatMap(Function.identity())
				.collect(Collectors.toList());
		})).stream();
	}
}
