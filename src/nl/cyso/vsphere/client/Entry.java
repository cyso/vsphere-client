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
import nl.cyso.vsphere.client.constants.ListModeType;
import nl.nekoconeko.configmode.Formatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.configuration.ConfigurationException;

import com.vmware.vim25.ManagedObjectReference;

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
				ConfigModes.printConfigModeHelp("ROOT");
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
			Formatter.usageError("No credentials were set, or server uri was missing", "ROOT", true);
		}

		try {
			String mode = Configuration.getString("mode");
			if (mode.equals("ADDVM")) {
				Configuration.load("ADDVM", args);
				VsphereClient.createVirtualMachine();
			} else if (mode.equals("REMOVEVM") || mode.equals("POWERONVM") || mode.equals("POWEROFFVM") || mode.equals("SHUTDOWNVM") || mode.equals("REBOOTVM") || mode.equals("MODIFYVM")) {
				Configuration.load(mode, args);

				Formatter.printInfoLine("Selecting root Virtual Machine folder");

				ManagedObjectReference rootFolder;
				if (Configuration.has("folder") && !Configuration.getString("folder").equals("")) {
					rootFolder = VsphereQuery.findVirtualMachineFolder(Configuration.getString("dc"), Configuration.getString("folder"), 0);
				} else {
					rootFolder = VsphereQuery.getVMRootFolder(Configuration.getString("dc"));
				}

				if (rootFolder == null) {
					Formatter.printErrorLine("Could not select root Virtual Machine folder");
					System.exit(-1);
				}

				Formatter.printInfoLine("Walking folder to find Virtual Machine");

				ManagedObjectReference vm = VsphereQuery.findVirtualMachine(Configuration.getString("fqdn"), rootFolder);

				if (vm == null) {
					Formatter.printErrorLine("Could not find VM");
					System.exit(-1);
				}

				if (mode.equals("REMOVEVM")) {
					Formatter.printInfoLine("Removing Virtual Machine: " + Configuration.getString("fqdn"));
					VsphereClient.deleteVirtualMachine(vm, Configuration.has("confirm"));
				} else if (mode.equals("POWERONVM")) {
					Formatter.printInfoLine("Powering on Virtual Machine: " + Configuration.getString("fqdn"));
					VsphereClient.powerOnVirtualMachine(vm);
				} else if (mode.equals("POWEROFFVM")) {
					Formatter.printInfoLine("Powering off Virtual Machine: " + Configuration.getString("fqdn"));
					VsphereClient.powerOffVirtualMachine(vm, Configuration.has("confirm"));
				} else if (mode.equals("SHUTDOWNVM")) {
					Formatter.printInfoLine("Requesting shutdown of Virtual Machine: " + Configuration.getString("fqdn"));
					VsphereClient.shutdownVirtualMachine(vm, Configuration.has("confirm"));
				} else if (mode.equals("REBOOTVM")) {
					Formatter.printInfoLine("Requesting reboot of Virtual Machine: " + Configuration.getString("fqdn"));
					VsphereClient.rebootVirtualMachine(vm, Configuration.has("confirm"));
				} else if (mode.equals("MODIFYVM")) {
					VsphereClient.modifyVirtualMachine(vm, Configuration.has("confirm"));
				}
			} else if (mode.equals("LIST")) {
				Configuration.load("LIST", args);

				ListModeType listType = null;
				try {
					listType = ListModeType.valueOf(Configuration.getString("list-type"));
				} catch (IllegalArgumentException e) {
					Formatter.usageError("Invalid List type selected", "LIST", true);
				}

				switch (listType) {
				case FOLDER:
				case VM:
					VsphereClient.VMFolderListMode(listType);
					break;
				case CLUSTER:
				case ESXNODE:
				case NETWORK:
				case STORAGE:
					VsphereClient.ComputeFolderListMode(listType);
					break;
				default:
					throw new UnsupportedOperationException("List Mode not yet implemented");
				}

			} else {
				throw new UnsupportedOperationException("Mode not yet implemented");
			}
		} catch (RemoteException re) {
			Formatter.printError("An error occured on vSphere: ");
			Formatter.printErrorLine(re);
			System.exit(-1);
		} catch (RuntimeException re) {
			Formatter.printError("Failed to execute action: ");
			Formatter.printErrorLine(re);
			System.exit(-1);
		} catch (Exception e) {
			Formatter.printErrorLine("An unexpected error occurred: ");
			Formatter.printStackTrace(e);
			System.exit(-1);
		} finally {
			VsphereManager.disconnect();
		}
	}
}
