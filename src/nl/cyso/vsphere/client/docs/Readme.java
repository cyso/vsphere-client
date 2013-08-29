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
package nl.cyso.vsphere.client.docs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import nl.cyso.vsphere.client.config.ConfigModes;
import nl.cyso.vsphere.client.config.Version;
import nl.nekoconeko.configmode.ConfigModeSorter;
import nl.nekoconeko.configmode.ConfigParameter;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.lang.StringUtils;

public class Readme {

	private String getNameSection() {
		StringBuilder section = new StringBuilder();
		String header = String.format("%s - a tool to manage vSphere datacenter objects\n", Version.getVersion().getProjectName());
		section.append(header);
		section.append(StringUtils.repeat("-", header.length()) + "\n\n");
		return section.toString();
	}

	private String getSynopsisSection() {
		StringBuilder section = new StringBuilder();
		section.append("SYNOPSIS\n");
		section.append("--------\n");

		HelpFormatter help = new HelpFormatter();
		help.setSyntaxPrefix("");

		Writer str = new StringWriter();
		PrintWriter pw = new PrintWriter(str);

		help.printUsage(pw, 1000, Version.getVersion().getProjectName(), ConfigModes.getMode("ROOT"));
		section.append("\t\n\t" + str.toString() + "\n");

		for (String m : ConfigModes.getModes().keySet()) {
			if (m == "ROOT") {
				continue;
			}
			str = new StringWriter();
			pw = new PrintWriter(str);
			help.printUsage(pw, 1000, Version.getVersion().getProjectName(), ConfigModes.getMode(m));
			section.append(String.format("**%s**\n\n", m.toString()));
			section.append("\t" + str.toString() + "\n");
		}
		return section.toString();
	}

	private String getDescriptionSection() {
		StringBuilder section = new StringBuilder();
		section.append(String.format("**%s** ", Version.getVersion().getProjectName()));
		section.append("is a tool to manage objects in vSphere, using the vSphere API.\n\n");
		section.append("There are several modes of operation, all of which can be found in the synopsis below. ");
		section.append("Each mode has a different set of required and optional arguments, which can also be found in the synopsis. ");
		section.append("Help mode can be used in a context sensitive manner. For example, ");
		section.append("*-h* ");
		section.append("will show all the modes, and ");
		section.append("*-h ADDVM* ");
		section.append("will show help about the ADDVM mode.\n\n");
		section.append("All commands require proper authentication. This can be provided on the command line by using ");
		section.append("*-u -p -s* ");
		section.append("or by creating a configuration file and specifying it with ");
		section.append("*-c config-file*\n");
		section.append(String.format("**%s** is licensed under the GPLv3 license. For more information, see the *LICENSE* file.\n", Version.getVersion().getProjectName()));
		section.append(String.format("This project uses libraries and routines which may have a different license. Refer to the included licenses in the source files and/or JAR files for more information.\n\n"));
		return section.toString();
	}

	private String getOptionsSection() {
		StringBuilder section = new StringBuilder();
		section.append("OPTIONS\n");
		section.append("-------\n");

		List<Option> options = ConfigModes.getConsolidatedModes().getAllOptions();
		Collections.sort(options, ConfigModeSorter.CONFIGPARAMETER_ALPHANUM);

		for (Object opt : options) {
			if (!opt.getClass().equals(ConfigParameter.class)) {
				continue;
			}
			ConfigParameter o = (ConfigParameter) opt;
			if (o.getOpt() != null) {
				section.append(String.format("**-%s** ", o.getOpt()));
			}
			if (o.getLongOpt() != null) {
				section.append(String.format("**--%s** ", o.getLongOpt()));
			}
			if (o.hasArgName()) {
				section.append(String.format("*%s* ", o.getArgName()));
			}
			if (o.getDescription() != null) {
				section.append("\n\n");
				section.append(o.getDescription() + "\n");
			}
			section.append("\n");
		}

		return section.toString();
	}

	private String getConfigurationSection() {
		StringBuilder section = new StringBuilder();
		section.append("CONFIGURATION\n");
		section.append("-------------\n");
		section.append("All command line parameters can optionally be provided using a configuration file. Exception on this are the mode selectors. ");
		section.append("The configuration file uses a simple format, which is:\n\n");
		section.append("\toption");
		section.append("=");
		section.append("value\n\n");
		section.append("*option* ");
		section.append("is the same as the long options which can be specified on the command line. For example, this is a valid configuration line:\n\n");
		section.append("\tusername=user@Organization\n\n");
		section.append("Configuration options are parsed in the following order: \n\n");
		section.append("1. The ");
		section.append("*-c* ");
		section.append("option.\n");
		section.append("2. All options provided on the command line, in the order they are specified.\n");
		section.append("It is possible to override already specified configuration options by specifying them again. Duplicate options will take ");
		section.append("the value of the last one specified. An example configuration file can be found in the distribution package.\n\n");
		return section.toString();
	}

	private String getBugsSection() {
		StringBuilder section = new StringBuilder();
		section.append("BUGS\n");
		section.append("----\n");
		section.append("No major known bugs exist at this time.\n\n");
		return section.toString();
	}

	private String getAuthorsSection() {
		StringBuilder section = new StringBuilder();
		section.append("AUTHOR\n");
		section.append("------\n");
		section.append("Nick Douma (n.douma@nekoconeko.nl)\n\n");
		return section.toString();
	}

	private String getBuildSection() {
		StringBuilder section = new StringBuilder();
		section.append("BUILDING\n");
		section.append("--------\n");
		section.append(String.format("Building **%s** requires the following:\n\n", Version.getVersion().getProjectName()));
		section.append("1. Oracle Java or OpenJDK >= 6\n");
		section.append("2. Apache Ant >= 1.8\n\n");
		section.append(String.format("Then you can simply call `ant dist` to create a *dist* folder with everything %s needs to run. ", Version.getVersion().getProjectName()));
		section.append("You can also use `ant package-tar` to create a tarball\n\n");
		section.append("Alternatively, if using Ubuntu or Debian, you can try using the vsphere-client PPA at: https://launchpad.net/~lordgaav/+archive/vsphere-client\n\n");

		return section.toString();
	}

	public static void main(String[] args) {
		ConfigModes.init();
		Readme r = new Readme();
		System.out.print(r.getNameSection());
		System.out.print(r.getDescriptionSection());
		System.out.print(r.getBuildSection());
		System.out.print(r.getSynopsisSection());
		System.out.print(r.getOptionsSection());
		System.out.print(r.getConfigurationSection());
		System.out.print(r.getBugsSection());
		System.out.print(r.getAuthorsSection());
	}
}
