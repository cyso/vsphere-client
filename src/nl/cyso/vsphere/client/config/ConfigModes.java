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

import java.util.Arrays;
import java.util.List;

import nl.nekoconeko.configmode.ConfigMode;
import nl.nekoconeko.configmode.ConfigParameter;

import org.apache.commons.cli.OptionGroup;

public class ConfigModes extends nl.nekoconeko.configmode.ConfigModes {

	static {
		// Set version information
		ConfigModes.setVersionInfo(Version.getVersion());

		// Configuration file
		ConfigParameter config = new ConfigParameter("c", "config", true, "FILE", "Use a configuration file");

		// Connection information
		ConfigParameter username = new ConfigParameter("u", "username", true, "USER", "vSphere username");
		ConfigParameter password = new ConfigParameter("p", "password", true, "PASSWORD", "vSphere password");
		ConfigParameter server = new ConfigParameter("s", "server", true, "SERVER", "vSphere server URI");

		List<ConfigParameter> configopts = Arrays.asList(config, username, password, server);

		// Modes
		ConfigParameter helpmode = new ConfigParameter("h", "help", true, "COMMAND", "Show help and examples");
		helpmode.setOptionalArg(true);
		ConfigParameter versionmode = new ConfigParameter("v", "version", false, "Show version information");
		ConfigParameter listmode = new ConfigParameter("l", "list", true, "TYPE", "List vSphere objects (folder|vm|cluster|esxnode|storage|network). VM objects can be filtered using --fqdn. esxnode, storage and network require a --cluster");
		ConfigParameter addmode = new ConfigParameter("a", "add-vm", false, "Add a new VM");
		ConfigParameter removemode = new ConfigParameter("r", "remove-vm", false, "Remove a VM. Requires confirmation");
		ConfigParameter poweronmode = new ConfigParameter("y", "poweron-vm", false, "Start an existing VM");
		ConfigParameter poweroffmode = new ConfigParameter("t", "poweroff-vm", false, "Stop an existing VM (hard shutdown). Requires confirmation");
		ConfigParameter shutdownmode = new ConfigParameter("z", "shutdown-vm", false, "Shutdown an existing VM (soft shutdown). Requires confirmation");
		ConfigParameter rebootmode = new ConfigParameter("x", "reboot-vm", false, "Reboot an existing VM (soft shutdown). Requires confirmation");
		ConfigParameter modifymode = new ConfigParameter("m", "modify-vm", false, "Modify an existing VM. Requires confirmation. Note that the VM must be powered off for most actions.");

		// Selectors
		ConfigParameter dc = new ConfigParameter("dc", true, "VDC", "Select this Data Center");
		ConfigParameter folder = new ConfigParameter("folder", true, "F", "Select this Folder. Specify as a Unix path, e.g.: /Customers/C");
		ConfigParameter storage = new ConfigParameter("storage", true, "S", "Select this Storage node. Mutually exclusive with --storagecluster");
		ConfigParameter storagecluster = new ConfigParameter("storagecluster", true, "SC", "Select this Storage cluster. Mutually exclusive with --storage");
		ConfigParameter esxnode = new ConfigParameter("esxnode", true, "E", "Select this ESX node. Mutually exclusive with --esxcluster");
		ConfigParameter esxcluster = new ConfigParameter("esxcluster", true, "EC", "Select this ESX cluster. Mutually exclusive with --esxnode");
		ConfigParameter cluster = new ConfigParameter("cluster", true, "C", "Select this Cluster");

		OptionGroup storageopt = new OptionGroup();
		storageopt.addOption(storage);
		storageopt.addOption(storagecluster);
		storageopt.setRequired(true);

		OptionGroup nodeopt = new OptionGroup();
		nodeopt.addOption(esxnode);
		nodeopt.addOption(esxcluster);
		nodeopt.setRequired(true);

		ConfigParameter template = new ConfigParameter("template", true, "TEMPLATE", "Select this template");
		template.setOptionalArg(true);

		ConfigParameter action = new ConfigParameter("action", true, "ACTION", "What action to take for --modify-vm mode (add|modify|delete). add/delete is only relevant for --network, use modify in all other cases");

		// User input
		ConfigParameter fqdn = new ConfigParameter("fqdn", true, "FQDN", "Name of object to create");
		ConfigParameter description = new ConfigParameter("description", true, "DESC", "Description of object to create");
		ConfigParameter network = new ConfigParameter("network", true, "NETWORK", "Network of the object to create");
		ConfigParameter mac = new ConfigParameter("mac", true, "MAC", "MAC address of the object to create");
		ConfigParameter cpu = new ConfigParameter("cpu", true, "CPU", "Amount of CPUs (cores) of the object to create");
		ConfigParameter memory = new ConfigParameter("memory", true, "MEM", "Memory (in MB) of the object to create");
		ConfigParameter os = new ConfigParameter("os", true, "OS", "Operating System of the object to create");
		ConfigParameter disk = new ConfigParameter("disk", true, "DISK", "Disk size (in MB) of the object to create");

		ConfigParameter odd = new ConfigParameter("odd", true, "ISO", "ODD drive to create with ISO file to mount. Use with --storage to select the datastore where the ISO file resides");
		ConfigParameter floppy = new ConfigParameter("floppy", true, "FLP", "FDD drive to create with floppy file to mount. Use with --storage to select the datastore where the ISO file resides");

		ConfigParameter property = new ConfigParameter("parameter", true, "PARAM", "Virtual Machine parameter to modify");
		ConfigParameter value = new ConfigParameter("value", true, "VALUE", "Virtual Machine parameter value");

		ConfigParameter confirm = new ConfigParameter("confirm", false, null, "Confirm destructive actions, and allow them to execute.");

		List<ConfigParameter> creationopts = Arrays.asList(template, fqdn, description, network, mac, cpu, memory, os, disk);

		// Output options
		ConfigParameter detailed = new ConfigParameter("detailed", false, null, "Output detailed information about the selected objects");
		ConfigParameter properties = new ConfigParameter("properties", false, null, "Display all configuration parameters about the selected objects");
		ConfigParameter depth = new ConfigParameter("depth", true, "DEPTH", "How deep to recurse into the Virtual Machine tree. Currently only used by --list vm");

		OptionGroup modes = new OptionGroup();
		modes.addOption(helpmode);
		modes.addOption(versionmode);
		modes.addOption(listmode);
		modes.addOption(addmode);
		modes.addOption(removemode);
		modes.addOption(poweronmode);
		modes.addOption(poweroffmode);
		modes.addOption(shutdownmode);
		modes.addOption(rebootmode);
		modes.addOption(modifymode);
		modes.setRequired(true);

		OptionGroup modifymodes = new OptionGroup();
		modifymodes.addOption(description);
		modifymodes.addOption(network);
		modifymodes.addOption(cpu);
		modifymodes.addOption(memory);
		modifymodes.addOption(property);
		modifymodes.addOption(odd);
		modifymodes.addOption(floppy);
		modifymodes.setRequired(true);

		ConfigMode root = new ConfigMode();
		root.addOptionGroup(modes);
		root.addOptions(configopts);

		ConfigMode help = new ConfigMode();
		help.addRequiredOption(helpmode);

		ConfigMode version = new ConfigMode();
		version.addRequiredOption(versionmode);

		ConfigMode list = new ConfigMode();
		list.addRequiredOption(listmode);
		list.addOptions(configopts);
		list.addRequiredOption(dc);
		list.addOption(folder);
		list.addOption(fqdn);
		list.addOption(cluster);
		list.addOption(detailed);
		list.addOption(properties);
		list.addOption(depth);

		ConfigMode removevm = new ConfigMode();
		removevm.addRequiredOption(removemode);
		removevm.addOptions(configopts);
		removevm.addRequiredOption(fqdn);
		removevm.addRequiredOption(dc);
		removevm.addOption(folder);
		removevm.addOption(confirm);

		ConfigMode poweronvm = new ConfigMode();
		poweronvm.addRequiredOption(poweronmode);
		poweronvm.addOptions(configopts);
		poweronvm.addRequiredOption(fqdn);
		poweronvm.addRequiredOption(dc);
		poweronvm.addOption(folder);

		ConfigMode poweroffvm = new ConfigMode();
		poweroffvm.addRequiredOption(poweroffmode);
		poweroffvm.addOptions(configopts);
		poweroffvm.addRequiredOption(fqdn);
		poweroffvm.addRequiredOption(dc);
		poweroffvm.addOption(folder);
		poweroffvm.addOption(confirm);

		ConfigMode shutdownvm = new ConfigMode();
		shutdownvm.addRequiredOption(shutdownmode);
		shutdownvm.addOptions(configopts);
		shutdownvm.addRequiredOption(fqdn);
		shutdownvm.addRequiredOption(dc);
		shutdownvm.addOption(folder);
		shutdownvm.addOption(confirm);

		ConfigMode rebootvm = new ConfigMode();
		rebootvm.addRequiredOption(rebootmode);
		rebootvm.addOptions(configopts);
		rebootvm.addRequiredOption(fqdn);
		rebootvm.addRequiredOption(dc);
		rebootvm.addOption(folder);
		rebootvm.addOption(confirm);

		ConfigMode modifyvm = new ConfigMode();
		modifyvm.addRequiredOption(modifymode);
		modifyvm.addOptions(configopts);
		modifyvm.addRequiredOption(fqdn);
		modifyvm.addRequiredOption(dc);
		modifyvm.addRequiredOption(action);
		modifyvm.addOptionGroup(modifymodes);
		modifyvm.addOption(folder);
		modifyvm.addOption(storage);
		modifyvm.addOption(mac);
		modifyvm.addOption(value);
		modifyvm.addOption(confirm);

		ConfigMode addvm = new ConfigMode();
		addvm.addRequiredOption(addmode);
		addvm.addOptions(configopts);
		addvm.addOptionGroup(storageopt);
		addvm.addOptionGroup(nodeopt);
		addvm.addRequiredOption(dc);
		addvm.addOption(folder);
		addvm.addRequiredOptions(creationopts);

		ConfigModes.addMode("ROOT", root);
		ConfigModes.addMode("HELP", help);
		ConfigModes.addMode("VERSION", version);
		ConfigModes.addMode("LIST", list);
		ConfigModes.addMode("ADDVM", addvm);
		ConfigModes.addMode("REMOVEVM", removevm);
		ConfigModes.addMode("POWERONVM", poweronvm);
		ConfigModes.addMode("POWEROFFVM", poweroffvm);
		ConfigModes.addMode("SHUTDOWNVM", shutdownvm);
		ConfigModes.addMode("REBOOTVM", rebootvm);
		ConfigModes.addMode("MODIFYVM", modifyvm);
	}

	public static void init() {
		// Do nothing, only used to automatically invoke the static constructor
	}
}
