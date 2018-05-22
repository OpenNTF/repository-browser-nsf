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
