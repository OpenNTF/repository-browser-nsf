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

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.vfs.VFS;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

/**
 * A concrete implementation of {@link UpdateSiteNSFVFSFile} with additional
 * methods to access feature metadata.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class UpdateSiteNSFVFSFeature extends UpdateSiteNSFVFSFile {
	public static final String ITEM_FILE = "feature.file"; //$NON-NLS-1$
	public static final String ITEM_MOD = "feature.file.lastModified"; //$NON-NLS-1$
	public static final String ITEM_NAME = "feature.label"; //$NON-NLS-1$
	public static final String ITEM_DESC = "feature.description"; //$NON-NLS-1$
	public static final String ITEM_DESC_URL = "feature.description.url"; //$NON-NLS-1$
	public static final String ITEM_IMPORT_FEATURE = "feature.import.feature"; //$NON-NLS-1$
	public static final String ITEM_IMPORT_VERSION = "feature.import.version"; //$NON-NLS-1$
	public static final String ITEM_PLUGIN_ID = "feature.plugin.id"; //$NON-NLS-1$
	public static final String ITEM_PLUGIN_VERSION = "feature.plugin.version"; //$NON-NLS-1$
	public static final String ITEM_LICENSE = "feature.license"; //$NON-NLS-1$
	public static final String ITEM_LICENSE_URL = "feature.license.url"; //$NON-NLS-1$
	public static final String ITEM_COPYRIGHT = "feature.copyright"; //$NON-NLS-1$
	public static final String ITEM_COPYRIGHT_URL = "feature.copyright.url"; //$NON-NLS-1$
	public static final String ITEM_CATEGORY = "feature.category"; //$NON-NLS-1$
	
	public UpdateSiteNSFVFSFeature(VFS vfs, String name, String id, String version, Document doc) {
		super(vfs, name, id, version, doc);
	}

	@Override
	public long getDocLastModified() {
		try {
			String modString = doc.getItemValueString(ITEM_MOD);
			return Long.parseLong(modString, 10);
		} catch(NotesException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected EmbeddedObject getFile() throws NotesException {
		RichTextItem body = (RichTextItem)doc.getFirstItem(ITEM_FILE);
		return (EmbeddedObject)body.getEmbeddedObjects().get(0);
	}
	
	public String getFeatureName() throws NotesException {
		return doc.getItemValueString(ITEM_NAME);
	}
	
	public String getFeatureDescription() throws NotesException {
		return doc.getItemValueString(ITEM_DESC);
	}
	
	public String getFeatureDescriptionUrl() throws NotesException {
		return doc.getItemValueString(ITEM_DESC_URL);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getImportedFeatures() throws NotesException {
		return doc.getItemValue(ITEM_IMPORT_FEATURE);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> getImportedFeatureVersions() throws NotesException {
		return doc.getItemValue(ITEM_IMPORT_VERSION);
	}
	
	@SuppressWarnings("unchecked")
	public List<PluginInfo> getPlugins() throws NotesException {
		List<PluginInfo> result = new ArrayList<>();
		
		List<String> ids = doc.getItemValue(ITEM_PLUGIN_ID);
		List<String> versions = doc.getItemValue(ITEM_PLUGIN_VERSION);
		for(int i = 0; i < ids.size(); i++) {
			String id = ids.get(i);
			if(StringUtil.isNotEmpty(id)) {
				String version = null;;
				if(versions.size() > i) {
					version = versions.get(i);
				}
				if(StringUtil.isEmpty(version)) {
					version = "0.0.0"; //$NON-NLS-1$
				}
				
				result.add(new PluginInfo(id, version));
			}
		}
		
		return result;
	}
	
	public String getLicense() throws NotesException {
		return doc.getItemValueString(ITEM_LICENSE);
	}
	
	public String getLicenseUrl() throws NotesException {
		return doc.getItemValueString(ITEM_LICENSE_URL);
	}
	
	public String getCopyright() throws NotesException {
		return doc.getItemValueString(ITEM_COPYRIGHT);
	}
	
	public String getCopyrightUrl() throws NotesException {
		return doc.getItemValueString(ITEM_COPYRIGHT_URL);
	}
	
	public String getCategory() throws NotesException {
		return doc.getItemValueString(ITEM_CATEGORY);
	}
}
