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
import java.util.Map;
import java.util.TreeMap;

import nl.cyso.vsphere.client.config.ConfigModes;
import nl.cyso.vsphere.client.config.Configuration;
import nl.cyso.vsphere.client.config.Version;
import nl.nekoconeko.configmode.Formatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.configuration.ConfigurationException;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.VirtualMachine;

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
			} else if (mode.equals("REMOVEVM") || mode.equals("POWERONVM") || mode.equals("POWEROFFVM") || mode.equals("SHUTDOWNVM")) {
				Configuration.load(mode, args);

				Formatter.printInfoLine("Selecting root Virtual Machine folder");

				ManagedObjectReference rootFolder;
				if (Configuration.has("folder") && !Configuration.getString("folder").equals("")) {
					rootFolder = VsphereQuery.findVirtualMachineFolder(Configuration.getString("dc"), Configuration.getString("folder"));
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
				}
			} else if (mode.equals("LIST")) {
				if (!Configuration.getString("list-type").equals("FOLDER") && !Configuration.getString("list-type").equals("VM")) {
					Formatter.usageError("Invalid List type selected", "LIST", true);
				}

				Configuration.load("LIST", args);

				Formatter.printInfoLine("Selecting root Virtual Machine folder");

				String rootFolder;
				if (Configuration.has("folder") && !Configuration.getString("folder").equals("")) {
					rootFolder = Configuration.getString("folder");
				} else {
					rootFolder = "/";
				}

				if (rootFolder == null) {
					Formatter.printErrorLine("Could not select root Virtual Machine folder");
					System.exit(-1);
				}

				Formatter.printInfoLine("Walking tree");

				Map<String, ManagedObjectReference> objects;
				if (Configuration.getString("list-type").equals("FOLDER")) {
					objects = VsphereQuery.findVirtualMachineFolders(Configuration.getString("dc"), rootFolder);
				} else {
					ManagedObjectReference folder = VsphereQuery.findVirtualMachineFolder(Configuration.getString("dc"), rootFolder);
					int depth = 0;
					if (Configuration.has("depth")) {
						try {
							depth = Integer.parseInt(Configuration.getString("depth"));
						} catch (NumberFormatException nfe) {
							Formatter.printErrorLine("Failed to parse --depth value, using 0 instead");
						}
					}

					objects = VsphereQuery.findVirtualMachines(null, folder, depth);
				}

				if (objects == null || objects.isEmpty()) {
					Formatter.printInfoLine("No objects found!");
				} else {
					Map<String, ManagedObjectReference> sorted = new TreeMap<String, ManagedObjectReference>(objects);
					Formatter.printBorderedInfo(String.format("Objects found in folder: %s\n", rootFolder));
					for (java.util.Map.Entry<String, ManagedObjectReference> object : sorted.entrySet()) {
						if (!Configuration.has("detailed") || Configuration.getString("list-type").equals("FOLDER")) {
							Formatter.printInfoLine(object.getKey());
							continue;
						}

						VirtualMachine vm = new VirtualMachine(VsphereManager.getServerConnection(), object.getValue());
						HostSystem host = new HostSystem(VsphereManager.getServerConnection(), vm.getRuntime().getHost());

						Formatter.printInfoLine(String.format("%40s - %20s - CPU:%d/MEM:%d", vm.getName(), host.getName(), vm.getConfig().getCpuAllocation().getShares().getShares() / 1000, vm.getConfig().getMemoryAllocation().getShares().getShares() / 10));
					}
				}
			} else {
				throw new UnsupportedOperationException("Mode not yet implemented");
			}
		} catch (RemoteException re) {
			Formatter.printError("Failed to execute action: ");
			Formatter.printErrorLine(re);
		} catch (Exception e) {
			Formatter.printErrorLine("An unexpected error occurred: ");
			Formatter.printStackTrace(e);
		} finally {
			VsphereManager.disconnect();
		}
	}
}
