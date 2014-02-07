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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.cyso.vsphere.client.config.ConfigModes;
import nl.cyso.vsphere.client.config.Version;
import nl.cyso.vsphere.client.tools.ResourceHelper;
import nl.nekoconeko.configmode.ConfigModeSorter;
import nl.nekoconeko.configmode.ConfigParameter;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

public class ManPage {
	private String getHeaderSection() {
		DateFormat df = new SimpleDateFormat("dd MMM yyyy");
		return String.format(".TH %s %d \"%s\" \"%s-%s\" \"%s\"\n", Version.getVersion().getProjectName(), 1, df.format(new Date()), Version.getVersion().getReleaseVersion(), Version.getVersion().getBuildVersion(), "");
	}

	private String getNameSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH NAME\n");
		section.append(String.format("%s - a tool to manage vSphere datacenter objects\n", Version.getVersion().getProjectName()));
		return section.toString();
	}

	private String getSynopsisSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH SYNOPSIS\n");

		HelpFormatter help = new HelpFormatter();
		help.setSyntaxPrefix("");

		Writer str = new StringWriter();
		PrintWriter pw = new PrintWriter(str);

		help.printUsage(pw, 1000, Version.getVersion().getProjectName(), ConfigModes.getMode("ROOT"));
		section.append(str.toString() + "\n");

		for (String m : ConfigModes.getModes().keySet()) {
			if (m == "ROOT") {
				continue;
			}
			section.append(String.format(".B %s\n", m.toString()));
			str = new StringWriter();
			pw = new PrintWriter(str);
			help.printUsage(pw, 1000, Version.getVersion().getProjectName(), ConfigModes.getMode(m));
			section.append(".RS 4\n");
			section.append(str.toString() + "\n");
			section.append(".RE\n");
		}
		return section.toString().replace("-", "\\-");
	}

	private String getDescriptionSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH DESCRIPTION\n");
		section.append(String.format(".B %s\n", Version.getVersion().getProjectName()));
		section.append("is a tool to manage objects in vSphere, using the vSphere API.\n\n");
		section.append("There are several modes of operation, all of which can be found in the synopsis below. ");
		section.append("Each mode has a different set of required and optional arguments, which can also be found in the synopsis. ");
		section.append("Help mode can be used in a context sensitive manner. For example, \n");
		section.append(".I \\-h\n");
		section.append("will show all the modes, and \n");
		section.append(".I \\-h ADDVM\n");
		section.append("will show help about the ADDVM mode.\n\n");
		section.append("All commands require proper authentication. This can be provided on the command line by using\n");
		section.append(".I \\-u \\-p \\-s\n");
		section.append("or by creating a configuration file and specifying it with\n");
		section.append(".I \\-c config-file\n");
		return section.toString();
	}

	private String getOptionsSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH OPTIONS\n");

		List<Option> options = ConfigModes.getConsolidatedModes().getAllOptions();
		Collections.sort(options, ConfigModeSorter.CONFIGPARAMETER_ALPHANUM);

		for (Object opt : options) {
			if (!opt.getClass().equals(ConfigParameter.class)) {
				continue;
			}
			ConfigParameter o = (ConfigParameter) opt;
			if (o.getOpt() != null) {
				section.append(String.format(".B \\-%s\n", o.getOpt()));
			}
			if (o.getLongOpt() != null) {
				section.append(String.format(".B \\-\\-%s\n", o.getLongOpt()));
			}
			if (o.hasArgName()) {
				section.append(String.format(".I %s\n", o.getArgName()));
			}
			if (o.getDescription() != null) {
				section.append(".RS 4\n");
				section.append(o.getDescription() + "\n");
				section.append(".RE\n");
			}
			section.append("\n");
		}

		return section.toString();
	}

	private String getConfigurationSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH CONFIGURATION\n");
		section.append("All command line parameters can optionally be provided using a configuration file. Exception on this are the mode selectors. ");
		section.append("The configuration file uses a simple format, which is:\n\n");
		section.append(".RS 4\n");
		section.append(".I option\n");
		section.append("=\n");
		section.append(".I value\n\n");
		section.append(".RE\n");
		section.append(".I option\n");
		section.append("is the same as the long options which can be specified on the command line. For example, this is a valid configuration line:\n\n");
		section.append(".RS 4\n");
		section.append("username=user@Organization\n\n");
		section.append(".RE\n");
		section.append("Configuration options are parsed in the following order: \n\n");
		section.append(".IP 1 4");
		section.append("The\n");
		section.append(".I \\-c\n");
		section.append("option.\n");
		section.append(".IP 2 4\n");
		section.append("All options provided on the command line, in the order they are specified.\n");
		section.append(".RE\n\n");
		section.append("It is possible to override already specified configuration options by specifying them again. Duplicate options will take ");
		section.append("the value of the last one specified. An example configuration file can be found in the distribution package.\n");
		return section.toString();
	}

	private String getBugsSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH BUGS\n");
		section.append("No major known bugs exist at this time.\n");
		return section.toString();
	}

	private String getAuthorsSection() {
		StringBuilder section = new StringBuilder();
		section.append(".SH AUTHOR\n");
		section.append(".MT n.douma@nekoconeko.nl\n");
		section.append("Nick Douma\n");
		section.append(".ME\n");
		return section.toString();
	}

	private String getExamplesSection() {
		StringBuilder section = new StringBuilder();

		try {
			String ex = ResourceHelper.readResourceIntoString(this.getClass(), "examples.txt", "UTF-8");
			BufferedReader examples = ResourceHelper.textToReader(ex);

			section.append(".SH EXAMPLES\n");

			String line = null;
			while ((line = examples.readLine()) != null) {
				if (line.contains("[")) {
					section.append(".RS 4\n");
					section.append(".B " + line.replaceAll("\\[(.*)\\]", "$1") + "\n");
					section.append(".RE\n");
				} else if (line.trim().equals("\n")) {
					continue;
				} else {
					section.append(line + "\n");
				}
			}
		} catch (IOException e) {
			return "";
		}

		return section.toString();
	}

	public static void main(String[] args) {
		ConfigModes.init();
		ManPage m = new ManPage();
		System.out.print(m.getHeaderSection());
		System.out.print(m.getNameSection());
		System.out.print(m.getDescriptionSection());
		System.out.print(m.getSynopsisSection());
		System.out.print(m.getOptionsSection());
		System.out.print(m.getConfigurationSection());
		System.out.print(m.getExamplesSection());
		System.out.print(m.getBugsSection());
		System.out.print(m.getAuthorsSection());
	}
}
