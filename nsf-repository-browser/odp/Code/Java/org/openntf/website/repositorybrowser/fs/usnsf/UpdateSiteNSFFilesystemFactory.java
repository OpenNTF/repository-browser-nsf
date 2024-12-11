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
package org.openntf.website.repositorybrowser.fs.usnsf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.openntf.website.repositorybrowser.fs.FilesystemFactory;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.vfs.VFS;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewNavigator;
import lotus.domino.ViewEntry;

public class UpdateSiteNSFFilesystemFactory implements FilesystemFactory {
	public static final String VIEW_SITES = "Update Site NSFs";
	
	@Override
	public Stream<VFS> getFilesystems() {
		try {
			Session session = ExtLibUtil.getCurrentSession();
			Database database = ExtLibUtil.getCurrentDatabase();
			
			View view = database.getView(VIEW_SITES);
			if(view == null) {
				throw new IllegalStateException("Could not open view '" + VIEW_SITES + "'");
			}
			view.setAutoUpdate(false);
			
			List<VFS> result = new ArrayList<>();
			
			ViewNavigator nav = view.createViewNav();
			nav.setBufferMaxEntries(400);
			ViewEntry entry = nav.getFirst();
			while(entry != null) {
				entry.setPreferJavaDates(true);
				List<?> columnValues = entry.getColumnValues();
				String name = StringUtil.toString(columnValues.get(0));
				String path = StringUtil.toString(columnValues.get(1));
				if(StringUtil.isNotEmpty(name) && StringUtil.isNotEmpty(path)) {
					Database updateSiteNsf = getDatabase(session, path);
					if(updateSiteNsf != null && updateSiteNsf.isOpen()) {
						result.add(new UpdateSiteNSFVFS(name, updateSiteNsf));
					}
				}
				
				ViewEntry tempEntry = entry;
				entry = nav.getNext();
				tempEntry.recycle();
			}
			
			return result.stream();
		} catch (NotesException e) {
			throw new RuntimeException(e);
		}
	}

	// *******************************************************************************
	// * Internal utility methods
	// *******************************************************************************
	
	private static Database getDatabase(Session session, String path) throws NotesException {
		String server;
		String filePath;
		int bangIndex = path.indexOf("!!");
		if(bangIndex > -1) {
			server = path.substring(0, bangIndex);
			filePath = path.substring(bangIndex+2);
		} else {
			server = "";
			filePath = path;
		}
		return session.getDatabase(server, filePath);
	}
}
