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

import java.rmi.RemoteException;

import nl.cyso.vsphere.client.config.ConfigModes;
import nl.cyso.vsphere.client.config.Configuration;
import nl.cyso.vsphere.client.config.Version;
import nl.nekoconeko.configmode.Formatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.configuration.ConfigurationException;

public class Entry {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Start out in ROOT ConfigMode
		CommandLine cli = null;

		// Initialize Formatter
		Formatter.setVersionInfo(Version.getVersion());
		ConfigModes.init();

		// Try to parse all ROOT cli options
		cli = Configuration.parseCli("ROOT", args);

		// Load the config if it was specified
		if (cli.hasOption("config")) {
			try {
				Configuration.loadFile(cli.getOptionValue("config"));
			} catch (ConfigurationException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		// Load all options parsed for ROOT ConfigMode
		Configuration.load(cli);

		// Now we know which ConfigMode was selected

		// Display (specific) help
		if (Configuration.get("mode").equals("HELP")) {
			if (Configuration.has("help-type")) {
				ConfigModes.printConfigModeHelp(Configuration.get("help-type").toString());
			} else {
				ConfigModes.printConfigModeHelp("root");
			}
			System.exit(0);
		}

		// From this point on we want a header displayed
		Formatter.printHeader();

		// Display version information
		if (Configuration.get("mode").equals("VERSION")) {
			System.exit(0);
		}

		if (!Configuration.has("username") || !Configuration.has("password") || !Configuration.has("server")) {
			Formatter.usageError("No credentials were set, or server uri was missing", "root");
		}

		VsphereClient client = new VsphereClient();
		String mode = Configuration.getString("mode");
		if (mode.equals("ADDVM")) {
			Configuration.load("ADDVM", args);
			try {
				client.createVirtualMachine();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
