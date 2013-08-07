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
		ConfigParameter username = new ConfigParameter("u", "username", true, "USER", "vCloud Director username");
		ConfigParameter password = new ConfigParameter("p", "password", true, "PASSWORD", "vCloud Director password");
		ConfigParameter server = new ConfigParameter("s", "server", true, "SERVER", "vCloud Director server URI");

		List<ConfigParameter> configopts = Arrays.asList(config, username, password, server);

		// Modes
		ConfigParameter helpmode = new ConfigParameter("h", "help", true, "COMMAND", "Show help and examples");
		helpmode.setOptionalArg(true);
		ConfigParameter versionmode = new ConfigParameter("v", "version", false, "Show version information");
		ConfigParameter listmode = new ConfigParameter("l", "list", true, "TYPE", "List vSphere objects (folder|vm|template|storage)");
		ConfigParameter addmode = new ConfigParameter("a", "add-vm", false, "Add a new VM");
		ConfigParameter removemode = new ConfigParameter("r", "remove-vm", false, "Remove a VM");
		ConfigParameter poweronmode = new ConfigParameter("y", "poweron-vm", false, "Start an existing VM");
		ConfigParameter poweroffmode = new ConfigParameter("t", "poweroff-vm", false, "Stop an existing VM (hard shutdown)");
		ConfigParameter shutdownmode = new ConfigParameter("z", "shutdown-vm", false, "Shutdown an existing VM (soft shutdown)");

		// Selectors
		ConfigParameter dc = new ConfigParameter("dc", true, "VDC", "Select this Data Center");
		ConfigParameter folder = new ConfigParameter("folder", true, "F", "Select this Folder");
		ConfigParameter cluster = new ConfigParameter("cluster", true, "C", "Select this Cluster");
		ConfigParameter storage = new ConfigParameter("storage", true, "S", "Select this Storage Pool");
		ConfigParameter esxnode = new ConfigParameter("esxnode", true, "E", "Select this ESX node");

		List<ConfigParameter> selectionopts1 = Arrays.asList(dc, esxnode, storage);
		List<ConfigParameter> selectionopts2 = Arrays.asList(folder, cluster);

		ConfigParameter template = new ConfigParameter("template", true, "TEMPLATE", "Select this template");
		template.setOptionalArg(true);

		// User input
		ConfigParameter fqdn = new ConfigParameter("fqdn", true, "FQDN", "Name of object to create");
		ConfigParameter description = new ConfigParameter("description", true, "DESC", "Description of object to create");
		ConfigParameter network = new ConfigParameter("network", true, "NETWORK", "Network of the object to create");
		ConfigParameter mac = new ConfigParameter("mac", true, "MAC", "MAC address of the object to create");
		ConfigParameter cpu = new ConfigParameter("cpu", true, "CPU", "Amount of CPUs (cores) of the object to create");
		ConfigParameter memory = new ConfigParameter("memory", true, "MEM", "Memory (in MB) of the object to create");
		ConfigParameter os = new ConfigParameter("os", true, "OS", "Operating System of the object to create");
		ConfigParameter disk = new ConfigParameter("disk", true, "DISK", "Disk size (in MB) of the object to create");

		List<ConfigParameter> creationopts = Arrays.asList(template, fqdn, description, network, mac, cpu, memory, os, disk);

		OptionGroup modes = new OptionGroup();
		modes.addOption(helpmode);
		modes.addOption(versionmode);
		modes.addOption(listmode);
		modes.addOption(addmode);
		modes.addOption(removemode);
		modes.addOption(poweronmode);
		modes.addOption(poweroffmode);
		modes.addOption(shutdownmode);
		modes.setRequired(true);

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
		list.addOption(dc);
		list.addOption(folder);
		list.addOption(cluster);
		list.addOption(storage);

		ConfigMode addvm = new ConfigMode();
		addvm.addRequiredOption(addmode);
		addvm.addOptions(configopts);
		addvm.addRequiredOptions(selectionopts1);
		addvm.addOptions(selectionopts2);
		addvm.addRequiredOptions(creationopts);

		ConfigMode removevm = new ConfigMode();
		removevm.addRequiredOption(removemode);
		removevm.addOptions(configopts);
		removevm.addRequiredOption(fqdn);
		removevm.addOption(mac);

		ConfigMode poweronvm = new ConfigMode();
		poweronvm.addOptions(configopts);
		poweronvm.addRequiredOption(fqdn);
		poweronvm.addOption(mac);

		ConfigMode poweroffvm = new ConfigMode();
		poweroffvm.addOptions(configopts);
		poweroffvm.addRequiredOption(fqdn);
		poweroffvm.addOption(mac);

		ConfigMode shutdownvm = new ConfigMode();
		shutdownvm.addOptions(configopts);
		shutdownvm.addRequiredOption(fqdn);
		shutdownvm.addOption(mac);

		ConfigModes.addMode("ROOT", root);
		ConfigModes.addMode("HELP", help);
		ConfigModes.addMode("VERSION", version);
		ConfigModes.addMode("LIST", list);
		ConfigModes.addMode("ADDVM", addvm);
		ConfigModes.addMode("REMOVEVM", removevm);
		ConfigModes.addMode("POWERONVM", poweronvm);
		ConfigModes.addMode("POWEROFFVM", poweroffvm);
		ConfigModes.addMode("SHUTDOWNVM", shutdownvm);
	}

	public static void init() {
		// Do nothing, only used to automatically invoke the static constructor
	}
}
