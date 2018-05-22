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
package org.openntf.website.repositorybrowser.fs;

import java.util.Comparator;

import com.ibm.commons.vfs.VFSResource;

public class VFSResourceComparator implements Comparator<VFSResource> {
	
	public static final VFSResourceComparator INSTANCE = new VFSResourceComparator();

	@Override
	public int compare(VFSResource a, VFSResource b) {
		return String.CASE_INSENSITIVE_ORDER.compare(sortable(a), sortable(b));
	}

	private static String sortable(VFSResource res) {
		return (res.isFolder() ? "AAA":"ZZZ") + res.getName();
	}
}
