package org.openntf.website.repositorybrowser.fs.usnsf;

class PackageInfo {
	private final String name;
	private final String version;
	
	PackageInfo(String name, String version) {
		this.name = name;
		this.version = version;
	}
	public String getName() {
		return name;
	}
	public String getVersion() {
		return version;
	}
}
