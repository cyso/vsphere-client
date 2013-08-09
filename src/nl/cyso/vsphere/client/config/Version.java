/*           
 * Copyright (c) 2013 Cyso < development [at] cyso . nl >
 *           
 * This file is part of vsphere-client.
 *           
 * vsphere-client is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *           
 * vsphere-client is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *           
 * You should have received a copy of the GNU General Public License
 * along with vsphere-client. If not, see <http://www.gnu.org/licenses/>.
 */
package nl.cyso.vsphere.client.config;

public class Version extends nl.nekoconeko.configmode.Version {
	/**
	 * Official name of the project
	 */
	public final String PROJECT_NAME = "vsphere-client";
	/**
	 * Official release version of vsphere-client
	 */
	public final String RELEASE_VERSION = "0.1";
	/**
	 * Actual Git revision according to `git describe` during bumping
	 */
	public final String BUILD_VERSION = "000000";

	private static Version instance = null;

	public static Version getVersion() {
		if (Version.instance == null) {
			Version.instance = new nl.cyso.vsphere.client.config.Version();
		}
		return Version.instance;
	}

	@Override
	public String getProjectName() {
		return PROJECT_NAME;
	}

	@Override
	public String getReleaseVersion() {
		return RELEASE_VERSION;
	}

	@Override
	public String getBuildVersion() {
		return BUILD_VERSION;
	}
}
