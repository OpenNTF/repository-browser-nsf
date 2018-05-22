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
package org.openntf.website.repositorybrowser.fs.usnsf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openntf.eclipse.osgi.util.ManifestElement;

import com.ibm.commons.util.StringUtil;
import com.ibm.commons.vfs.VFS;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;

/**
 * A concrete implementation of {@link UpdateSiteNSFVFSFile} with additional
 * methods to access plugin metadata.
 * 
 * @author Jesse Gallagher
 * @since 2.0.0
 */
public class UpdateSiteNSFVFSPlugin extends UpdateSiteNSFVFSFile {
	public static final String ITEM_FILE = "plugin.file"; //$NON-NLS-1$
	public static final String ITEM_MOD = "plugin.file.lastModified"; //$NON-NLS-1$
	public static final String ITEM_PROVIDER = "plugin.providername"; //$NON-NLS-1$
	public static final String ITEM_NAME = "plugin.name"; //$NON-NLS-1$
	public static final String ITEM_FRAGMENT = "plugin.fragment"; //$NON-NLS-1$
	
	public static final String ITEM_MANIFESTMF = "plugin.manifest.mf"; //$NON-NLS-1$
	
	private String manifestContent;
	private Map<String, String> manifest;

	public UpdateSiteNSFVFSPlugin(VFS vfs, String name, String id, String version, Document doc) {
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
	
	public String getProviderName() throws NotesException {
		return doc.getItemValueString(ITEM_PROVIDER);
	}
	
	public String getPluginName() throws NotesException {
		return doc.getItemValueString(ITEM_NAME);
	}
	
	public boolean isFragment() throws NotesException {
		return "true".equals(doc.getItemValueString(ITEM_FRAGMENT)); //$NON-NLS-1$
	}
	
	public String getFragmentHost() throws NotesException, IOException {
		return getManifest().get("Fragment-Host"); //$NON-NLS-1$
	}
	
	public List<PluginInfo> getRequiredPlugins() throws NotesException, IOException {
		String require = getManifest().get("Require-Bundle"); //$NON-NLS-1$
		if(StringUtil.isEmpty(require)) {
			return Collections.emptyList();
		} else {
			ManifestElement[] elements = ManifestElement.parseHeader("Require-Bundle", require); //$NON-NLS-1$
			return Arrays.stream(elements)
				.filter(el -> !StringUtil.equals("optional", el.getAttribute("resolution"))) //$NON-NLS-1$ //$NON-NLS-2$
				.map(el -> {
					String version = el.getAttribute("version"); //$NON-NLS-1$
					if(StringUtil.isEmpty(version)) {
						version = "0.0.0"; //$NON-NLS-1$
					}
					return new PluginInfo(el.getValue(), version);
				})
				.collect(Collectors.toList());
		}
	}
	
	public List<PackageInfo> getImportedPackages() throws NotesException, IOException {
		String imports = getManifest().get("Import-Package"); //$NON-NLS-1$
		if(StringUtil.isEmpty(imports)) {
			return Collections.emptyList();
		} else {
			ManifestElement[] elements = ManifestElement.parseHeader("Import-Package", imports); //$NON-NLS-1$
			return Arrays.stream(elements)
				.filter(el -> !StringUtil.equals("optional", el.getAttribute("resolution"))) //$NON-NLS-1$ //$NON-NLS-2$
				.map(el -> {
					String version = el.getAttribute("version"); //$NON-NLS-1$
					if(StringUtil.isEmpty(version)) {
						version = "0.0.0"; //$NON-NLS-1$
					}
					return new PackageInfo(el.getValue(), version);
				})
				.collect(Collectors.toList());
		}
	}
	
	public synchronized String getManifestContent() throws NotesException {
		if(this.manifestContent == null) {
			RichTextItem item = (RichTextItem)doc.getFirstItem(ITEM_MANIFESTMF);
			this.manifestContent = item.getUnformattedText();
		}
		return this.manifestContent;
	}
	
	// *******************************************************************************
	// * Internal utility methods
	// *******************************************************************************

	public synchronized Map<String, String> getManifest() throws NotesException, IOException {
		if(this.manifest == null) {
			String text = getManifestContent();
			ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes());
			this.manifest = ManifestElement.parseBundleManifest(bais, null);
		}
		return this.manifest;
	}
}
