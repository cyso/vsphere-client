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
package nl.cyso.vsphere.client;

import java.net.URL;

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;

public class VsphereManager {
	private static ServiceInstance serviceInstance;

	static {
		try {
			VsphereManager.serviceInstance = new ServiceInstance(new URL(Configuration.getString("server")), Configuration.getString("username"), Configuration.getString("password"), true);
		} catch (Exception e) {
			Formatter.printErrorLine("Failed to connect to vSphere");
			Formatter.printStackTrace(e);
			System.exit(-1);
		}
	}

	protected static ServiceInstance getServiceInstance() {
		return serviceInstance;
	}

	protected static ServerConnection getServerConnection() {
		return serviceInstance.getServerConnection();
	}

	protected static void disconnect() {
		VsphereManager.getServerConnection().logout();
	}
}
