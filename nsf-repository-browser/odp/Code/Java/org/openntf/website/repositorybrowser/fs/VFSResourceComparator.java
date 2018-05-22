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
