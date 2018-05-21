/**
 * Copyright © 2018 Christian Güdemann, Jesse Gallagher
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
}
