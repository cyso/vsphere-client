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

Alternatively, if using Ubuntu or Debian, you can try using the vsphere-client PPA at: https://launchpad.net/~lordgaav/+archive/vsphere-client

SYNOPSIS
--------
	
	vsphere-client -a | -h <COMMAND> | -l <TYPE> | -m | -r | -t | -v | -x | -y | -z [-c <FILE>]    [-p <PASSWORD>]  [-s <SERVER>]  [-u <USER>]

**HELP**

	vsphere-client -h <COMMAND>

**ADDVM**

	vsphere-client -a [-c <FILE>] --cpu <CPU> --dc <VDC> --description <DESC> --disk <DISK> --esxcluster <EC> | --esxnode <E>  [--folder <F>] --fqdn <FQDN> --mac <MAC> --memory <MEM> --network <NETWORK> --os <OS> [-p <PASSWORD>] [-s <SERVER>] --storage <S> | --storagecluster <SC>  --template <TEMPLATE> [-u <USER>]

**MODIFYVM**

	vsphere-client --action <ACTION> [-c <FILE>] [--confirm] --cpu <CPU> | --description <DESC> | --memory <MEM> | --network <NETWORK> | --parameter <PARAM> --dc <VDC>  [--folder <F>] --fqdn <FQDN> -m --mac <MAC>   [-p <PASSWORD>]  [-s <SERVER>] [-u <USER>] [--value <VALUE>]

**POWERONVM**

	vsphere-client [-c <FILE>] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>] -y

**VERSION**

	vsphere-client -v

**REBOOTVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>] -x

**LIST**

	vsphere-client [-c <FILE>] [--cluster <C>] --dc <VDC> [--depth <DEPTH>] [--detailed] [--folder <F>] --fqdn <FQDN> -l <TYPE> [-p <PASSWORD>] [--properties] [-s <SERVER>] [-u <USER>]

**POWEROFFVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] -t [-u <USER>]

**SHUTDOWNVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] [-s <SERVER>] [-u <USER>] -z

**REMOVEVM**

	vsphere-client [-c <FILE>] [--confirm] --dc <VDC> [--folder <F>] --fqdn <FQDN> [-p <PASSWORD>] -r [-s <SERVER>] [-u <USER>]

OPTIONS
-------
**-a** **--add-vm** *arg* 

Add a new VM

**--action** *ACTION* 

What action to take for --modify-vm mode (add|modify|delete). add/delete is only relevant for --network, use modify in all other cases

**-c** **--config** *FILE* 

Use a configuration file

**--cluster** *C* 

Select this Cluster

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

**--esxcluster** *EC* 

Select this ESX cluster. Mutually exclusive with --esxnode

**--esxnode** *E* 

Select this ESX node. Mutually exclusive with --esxcluster

**--folder** *F* 

Select this Folder. Specify as a Unix path, e.g.: /Customers/C

**--fqdn** *FQDN* 

Name of object to create

**-h** **--help** *COMMAND* 

Show help and examples

**-l** **--list** *TYPE* 

List vSphere objects (folder|vm|cluster|esxnode|storage|network). VM objects can be filtered using --fqdn. esxnode, storage and network require a --cluster

**--mac** *MAC* 

MAC address of the object to create

**--memory** *MEM* 

Memory (in MB) of the object to create

**-m** **--modify-vm** *arg* 

Modify an existing VM. Requires confirmation. Note that the VM must be powered off for most actions.

**--network** *NETWORK* 

Network of the object to create

**--os** *OS* 

Operating System of the object to create

**--parameter** *PARAM* 

Virtual Machine parameter to modify

**-p** **--password** *PASSWORD* 

vSphere password

**--properties** 

Display all configuration parameters about the selected objects

**-r** **--remove-vm** *arg* 

Remove a VM. Requires confirmation

**-s** **--server** *SERVER* 

vSphere server URI

**--storage** *S* 

Select this Storage node. Mutually exclusive with --storagecluster

**--storagecluster** *SC* 

Select this Storage cluster. Mutually exclusive with --storage

**--template** *TEMPLATE* 

Select this template

**-t** **--poweroff-vm** *arg* 

Stop an existing VM (hard shutdown). Requires confirmation

**-u** **--username** *USER* 

vSphere username

**--value** *VALUE* 

Virtual Machine parameter value

**-v** **--version** *arg* 

Show version information

**-x** **--reboot-vm** *arg* 

Reboot an existing VM (soft shutdown). Requires confirmation

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

