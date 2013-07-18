vsphere-client - a tool to manage vSphere datacenter objects
-----------------------------------------------------------

**vsphere-client** is a tool to manage objects in vSphere, using the vSphere API.

There are several modes of operation, all of which can be found in the synopsis below. Each mode has a different set of required and optional arguments, which can also be found in the synopsis. Help mode can be used in a context sensitive manner. For example, *-h* will show all the modes, and *-h ADDVM* will show help about the ADDVM mode.

All commands require proper authentication. This can be provided on the command line by using *-u -p -s* or by creating a configuration file and specifying it with *-c config-file*
**vsphere-client** is licensed under the GPLv3 license. For more information, see the *LICENSE* file.
This project uses libraries and routines which may have a different license. Refer to the included licenses in the source files and/or JAR files for more information.

BUILDING
--------
Building **vsphere-client** requires the following:

1. Oracle Java or OpenJDK >= 6
2. Apache Ant >= 1.8

Then you can simply call `ant dist` to create a *dist* folder with everything vsphere-client needs to run. You can also use `ant package-tar` to create a tarball

CONFIGURATION
-------------
All command line parameters can optionally be provided using a configuration file. Exception on this are the mode selectors. The configuration file uses a simple format, which is:

	option=value

*option* is the same as the long options which can be specified on the command line. For example, this is a valid configuration line:

	username=user@Organization

Configuration options are parsed in the following order: 

1. The *-c* option.
2. All options provided on the command line, in the order they are specified.
It is possible to override already specified configuration options by specifying them again. Duplicate options will take the value of the last one specified. An example configuration file can be found in the distribution package.

BUGS
----
No major known bugs exist at this time.

AUTHOR
------
Nick Douma (n.douma@nekoconeko.nl)

