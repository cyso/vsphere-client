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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

import nl.nekoconeko.configmode.ConfigMode;
import nl.nekoconeko.configmode.Formatter;
import nl.nekoconeko.configmode.IgnorePosixParser;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class Configuration extends nl.nekoconeko.configmode.Configuration {

	static {
		ConfigModes.init();
	}

	public static Object get(String key) {
		return Configuration.valueOrNull(key);
	}

	public static String getString(String key) {
		return (String) Configuration.valueOrNull(key);
	}

	public static void load(String mode, String[] args) {
		try {
			Configuration.load(mode, args, true);
		} catch (Exception e) {
			Formatter.printErrorLine("This error should never happend");
			Formatter.printStackTrace(e);
			System.exit(-1);
		}
	}

	public static void load(String mode, String[] args, boolean dieOnFailure) throws Exception {
		CommandLine cli = Configuration.parseCli(mode, args, dieOnFailure);
		Configuration.load(cli);
	}

	public static void load(CommandLine cli) {
		for (Option opt : cli.getOptions()) {
			if (cli.hasOption(opt.getLongOpt())) {
				if (opt.getLongOpt().equals("help")) {
					Configuration.set("mode", "HELP");
					if (cli.getOptionValue(opt.getLongOpt()) != null) {
						Configuration.set("help-type", cli.getOptionValue(opt.getLongOpt()));
					}
				} else if (opt.getLongOpt().equals("version")) {
					Configuration.set("mode", "VERSION");
				} else if (opt.getLongOpt().equals("list")) {
					Configuration.set("mode", "LIST");
					Configuration.set("list-type", cli.getOptionValue(opt.getLongOpt()).toUpperCase());
				} else if (opt.getLongOpt().equals("add-vm")) {
					Configuration.set("mode", "ADDVM");
				} else if (opt.getLongOpt().equals("remove-vm")) {
					Configuration.set("mode", "REMOVEVM");
				} else if (opt.getLongOpt().equals("poweron-vm")) {
					Configuration.set("mode", "POWERONVM");
				} else if (opt.getLongOpt().equals("poweroff-vm")) {
					Configuration.set("mode", "POWEROFFVM");
				} else if (opt.getLongOpt().equals("shutdown-vm")) {
					Configuration.set("mode", "SHUTDOWNVM");
				} else if (opt.getLongOpt().equals("reboot-vm")) {
					Configuration.set("mode", "REBOOTVM");
				} else if (opt.getLongOpt().equals("modify-vm")) {
					Configuration.set("mode", "MODIFYVM");
				} else if (opt.getLongOpt().equals("upload-to-datastore")) {
					Configuration.set("mode", "UPLOADTODATASTORE");
				} else if (opt.getLongOpt().equals("ip")) {
					try {
						Configuration.set("ip", InetAddress.getByName(cli.getOptionValue(opt.getLongOpt())));
					} catch (UnknownHostException uhe) {
						Configuration.set("ip", (InetAddress) null);
					}
				} else {
					Configuration.set(opt.getLongOpt(), cli.getOptionValue(opt.getLongOpt()));
				}
			}
		}
	}

	public static void loadFile(String filename) throws ConfigurationException {
		org.apache.commons.configuration.Configuration conf = null;
		conf = new PropertiesConfiguration(filename);

		Iterator<String> i = conf.getKeys();
		while (i.hasNext()) {
			String key = i.next();

			if (key.equals("help")) {
				Configuration.set("mode", "HELP");
			} else if (key.equals("version")) {
				Configuration.set("mode", "VERSION");
			} else if (key.equals("list")) {
				Configuration.set("mode", "LIST");
				Configuration.set("list-type", conf.getString(key).toUpperCase());
			} else if (key.equals("ip")) {
				try {
					Configuration.set("ip", InetAddress.getByName(conf.getString(key)));
				} catch (UnknownHostException uhe) {
					Configuration.set("ip", (InetAddress) null);
				}
			} else {
				Configuration.set(key, conf.getString(key));
			}
		}
	}

	public static CommandLine parseCli(String mode, String[] args) {
		try {
			return parseCli(mode, args, true);
		} catch (Exception e) {
			Formatter.printErrorLine("This error should never happend");
			Formatter.printStackTrace(e);
			System.exit(-1);
			return null;
		}
	}

	public static CommandLine parseCli(String mode, String[] args, boolean dieOnFailure) throws Exception {
		CommandLine cli = null;
		Options opt = ConfigModes.getMode(mode);
		Throwable die = null;
		try {
			cli = new IgnorePosixParser(true).parse(opt, args);
		} catch (MissingArgumentException me) {
			Formatter.usageError(me.getLocalizedMessage(), mode, false);
			die = me;
		} catch (MissingOptionException mo) {
			Formatter.usageError(mo.getLocalizedMessage(), mode, false);
			die = mo;
		} catch (AlreadySelectedException ase) {
			Formatter.usageError(ase.getLocalizedMessage(), mode, false);
			die = ase;
		} catch (UnrecognizedOptionException uoe) {
			Formatter.usageError(uoe.getLocalizedMessage(), mode, false);
			die = uoe;
		} catch (ParseException e) {
			Formatter.printStackTrace(e);
			die = e;
		}

		if (dieOnFailure && die != null) {
			System.exit(-1);
		} else if (!dieOnFailure && die != null) {
			throw new Exception("Failed to initialize Configuration", die);
		}

		return cli;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String dumpToString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String dumpToString(String mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String dumpToString(ConfigMode mode) {
		// TODO Auto-generated method stub
		return null;
	}

}
