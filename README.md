vsphere-client - a tool to manage vSphere datacenter objects
-------------------------------------------------------------

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

SYNOPSIS
--------
	
	vsphere-client -a | -h <COMMAND> | -l <TYPE> | -r | -t | -v | -y | -z [-c <FILE>]   [-p <PASSWORD>]  [-s <SERVER>]  [-u <USER>]

**HELP**

	vsphere-client -h <COMMAND>

**ADDVM**

	vsphere-client -a [-c <FILE>] --cpu <CPU> --dc <VDC> --description <DESC> --disk <DISK> --esxnode <E> [--folder <F>] --fqdn <FQDN> --mac <MAC> --memory <MEM> --network <NETWORK> --os <OS> [-p <PASSWORD>] [-s <SERVER>] --storage <S> --template <TEMPLATE> [-u <USER>]

**POWERONVM**

	vsphere-client [-c <FILE>] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>]

**VERSION**

	vsphere-client -v

**LIST**

	vsphere-client [-c <FILE>] --dc <VDC> [--depth <DEPTH>] [--detailed] [--folder <F>] -l <TYPE> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>]

**POWEROFFVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>]

**SHUTDOWNVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>]

**REMOVEVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] -r [-s <SERVER>] [-u <USER>]

OPTIONS
-------
**-a** **--add-vm** *arg* 

Add a new VM

**-c** **--config** *FILE* 

Use a configuration file

**--confirm** 

Confirm destructive actions, and allow them to execute.

**--cpu** *CPU* 

Amount of CPUs (cores) of the object to create

**--dc** *VDC* 

Select this Data Center

**--depth** *DEPTH* 

How deep to recurse into the Virtual Machine tree. Currently only used by --list vm

**--description** *DESC* 

Description of object to create

**--detailed** 

Output detailed information about the selected objects

**--disk** *DISK* 

Disk size (in MB) of the object to create

**--esxnode** *E* 

Select this ESX node

**--folder** *F* 

Select this Folder. Specify as a Unix path, e.g.: /Customers/C

**--fqdn** *FQDN* 

Name of object to create

**-h** **--help** *COMMAND* 

Show help and examples

**-l** **--list** *TYPE* 

List vSphere objects (folder|vm)

**--mac** *MAC* 

MAC address of the object to create

**--memory** *MEM* 

Memory (in MB) of the object to create

**--network** *NETWORK* 

Network of the object to create

**--os** *OS* 

Operating System of the object to create

**-p** **--password** *PASSWORD* 

vSphere password

**-r** **--remove-vm** *arg* 

Remove a VM. Requires confirmation

**-s** **--server** *SERVER* 

vSphere server URI

**--storage** *S* 

Select this Storage Pool

**--template** *TEMPLATE* 

Select this template

**-t** **--poweroff-vm** *arg* 

Stop an existing VM (hard shutdown). Requires confirmation

**-u** **--username** *USER* 

vSphere username

**-v** **--version** *arg* 

Show version information

**-y** **--poweron-vm** *arg* 

Start an existing VM

**-z** **--shutdown-vm** *arg* 

Shutdown an existing VM (soft shutdown). Requires confirmation

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

