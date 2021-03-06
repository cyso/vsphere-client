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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import nl.cyso.vsphere.client.config.Configuration;
import nl.cyso.vsphere.client.constants.BootDeviceType;
import nl.cyso.vsphere.client.constants.ListModeType;
import nl.cyso.vsphere.client.constants.VMGuestType;
import nl.nekoconeko.configmode.Formatter;

import org.apache.commons.lang.StringUtils;

import com.vmware.vim25.ArrayOfHostDatastoreBrowserSearchResults;
import com.vmware.vim25.ConcurrentAccess;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.FileFault;
import com.vmware.vim25.FileInfo;
import com.vmware.vim25.FileQueryFlags;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.HostDatastoreBrowserSearchSpec;
import com.vmware.vim25.InsufficientResourcesFault;
import com.vmware.vim25.InvalidDatastore;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.StorageIOAllocationInfo;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardMacType;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyImageBackingInfo;
import com.vmware.vim25.VirtualMachineBootOptions;
import com.vmware.vim25.VirtualMachineBootOptionsBootableCdromDevice;
import com.vmware.vim25.VirtualMachineBootOptionsBootableDevice;
import com.vmware.vim25.VirtualMachineBootOptionsBootableDiskDevice;
import com.vmware.vim25.VirtualMachineBootOptionsBootableEthernetDevice;
import com.vmware.vim25.VirtualMachineBootOptionsBootableFloppyDevice;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VsphereClient {
	public static ManagedObjectReference createVirtualMachine() throws RemoteException, Exception {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(Configuration.getString("dc"));
		if (dcmor == null) {
			throw new RuntimeException("Datacenter " + Configuration.get("dc") + " not found.");
		}

		ManagedObjectReference hostmor = null;
		if (Configuration.has("esxnode") && !Configuration.getString("esxnode").equals("")) {
			hostmor = VsphereQuery.getHostNodeReference(Configuration.getString("esxnode"), dcmor);
		} else if (Configuration.has("esxcluster") && !Configuration.getString("esxcluster").equals("")) {
			try {
				ClusterComputeResource cluster = VsphereQuery.getClustersForDatacenter(Configuration.getString("dc")).get(Configuration.getString("esxcluster"));
				hostmor = VsphereQuery.recommendHostSystem(cluster).getMOR();
			} catch (NullPointerException ne) {

			}
		}

		if (hostmor == null) {
			throw new RuntimeException("Host " + Configuration.get("esxnode") + " not found");
		}

		ManagedObjectReference crmor = VsphereQuery.getReferenceParent(hostmor);
		if (crmor == null) {
			throw new RuntimeException("No Compute Resource Found On Specified Host");
		}

		ManagedObjectReference resourcepoolmor = VsphereQuery.getResourcePoolReference(hostmor);
		Folder vmFolder = null;
		if (Configuration.has("folder") && !Configuration.getString("folder").equals("")) {
			ManagedObjectReference folderMor = VsphereQuery.findVirtualMachineFolder(dcmor, Configuration.getString("folder"), 0);
			if (folderMor == null) {
				throw new RuntimeException(String.format("Could not find VM folder [%s]", Configuration.getString("folder")));
			}
			vmFolder = new Folder(VsphereManager.getServerConnection(), folderMor);
		} else {
			vmFolder = new Datacenter(VsphereManager.getServerConnection(), dcmor).getVmFolder();
		}
		String mac = null;
		if (Configuration.has("mac") && !Configuration.getString("mac").equals("")) {
			mac = Configuration.getString("mac");
		}

		int disksplit = 10240;
		if (Configuration.has("disksplit") && !Configuration.getString("disksplit").equals("")) {
			disksplit = Integer.valueOf(Configuration.getString("disksplit"));
		}

		VirtualMachineConfigSpec vmConfigSpec;
		if (Configuration.has("storagecluster") && !Configuration.getString("storagecluster").equals("")) {
			vmConfigSpec = VsphereFactory.createVirtualMachineConfigSpec(Configuration.getString("storagecluster"), Integer.valueOf(Configuration.getString("disk")), disksplit, true, mac, Configuration.getString("network"), crmor, hostmor);
		} else {
			vmConfigSpec = VsphereFactory.createVirtualMachineConfigSpec(Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), disksplit, false, mac, Configuration.getString("network"), crmor, hostmor);
		}
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation(Configuration.getString("description"));
		vmConfigSpec.setMemoryMB(Long.parseLong(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.parseInt(Configuration.getString("cpu")));
		vmConfigSpec.setNumCoresPerSocket(1);

		if (Configuration.has("guest") && !Configuration.getString("guest").equals("")) {
			VMGuestType guest = null;
			try {
				guest = VMGuestType.valueOf(Configuration.getString("guest"));
			} catch (IllegalArgumentException iae) {
				throw new RuntimeException("Failure: invalid guest type selected: " + Configuration.getString("guest"));
			}
			vmConfigSpec.setGuestId(guest.toString());
		} else {
			// Fallback to generic 64bit guest
			vmConfigSpec.setGuestId(VMGuestType.otherGuest64.toString());
		}

		Task task = vmFolder.createVM_Task(vmConfigSpec, new ResourcePool(VsphereManager.getServerConnection(), resourcepoolmor), new HostSystem(VsphereManager.getServerConnection(), hostmor));
		if (task.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine(String.format("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn")));
		} else {
			LocalizedMethodFault error = task.getTaskInfo().getError();
			throw new RuntimeException("Failure: creating [ " + Configuration.get("fqdn") + " ] VM: " + error == null ? "" : error.getLocalizedMessage());
		}
		return (ManagedObjectReference) VsphereQuery.getTaskInfoResult(task);
	}

	public static void powerOnVirtualMachine(ManagedObjectReference vmmor) throws RemoteException, Exception {
		VsphereClient.powerOnVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor));
	}

	public static void powerOnVirtualMachine(VirtualMachine vm) throws RemoteException, Exception {
		VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOff);

		Task powerOnTask = vm.powerOnVM_Task(null);
		if (powerOnTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM powered on successfully");
		} else {
			LocalizedMethodFault error = powerOnTask.getTaskInfo().getError();
			throw new RuntimeException("Failure: starting [ " + vm.getName() + " ] VM: " + error == null ? "" : error.getLocalizedMessage());
		}
	}

	public static void powerOffVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.powerOffVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void powerOffVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOn);

		if (!confirmed) {
			Formatter.printInfoLine(String.format("WhatIf: Would have powered off VM %s now, but confirmation was not given.", vm.getName()));
			return;
		}

		Task powerOffTask = vm.powerOffVM_Task();
		if (powerOffTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM powered off successfully");
		} else {
			LocalizedMethodFault error = powerOffTask.getTaskInfo().getError();
			throw new RuntimeException("Failure: stopping [ " + vm.getName() + " ] VM: " + error == null ? "" : error.getLocalizedMessage());
		}
	}

	public static void shutdownVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.shutdownVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void shutdownVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOn);

		if (!confirmed) {
			Formatter.printInfoLine(String.format("WhatIf: Would have shutdown VM %s now, but confirmation was not given.", vm.getName()));
			return;
		}

		vm.shutdownGuest();
		Formatter.printInfoLine("Success: VM shutdown message sent successfully");
	}

	public static void rebootVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.rebootVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void rebootVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOn);

		if (!confirmed) {
			Formatter.printInfoLine(String.format("WhatIf: Would have reboot VM %s now, but confirmation was not given.", vm.getName()));
			return;
		}

		vm.rebootGuest();
		Formatter.printInfoLine("Success: VM reboot message sent successfully");
	}

	public static void deleteVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.deleteVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void deleteVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		if (VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOn, false)) {
			VsphereClient.powerOffVirtualMachine(vm, confirmed);
		}

		if (!confirmed) {
			Formatter.printInfoLine(String.format("WhatIf: Would have destroyed VM %s now, but confirmation was not given.", vm.getName()));
			return;
		}

		Task destroyTask = vm.destroy_Task();
		if (destroyTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM destroyed successfully");
		} else {
			LocalizedMethodFault error = destroyTask.getTaskInfo().getError();
			throw new RuntimeException("Failure: destroying [ " + vm.getName() + " ] VM: " + error == null ? "" : error.getLocalizedMessage());
		}
	}

	public static void modifyVirtualMachine(ManagedObjectReference vm, boolean confirmed) throws InvalidName, VmConfigFault, DuplicateName, TaskInProgress, FileFault, InvalidState, ConcurrentAccess, InvalidDatastore, InsufficientResourcesFault, RuntimeFault, RemoteException, InterruptedException {
		VsphereClient.modifyVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vm), confirmed);
	}

	public static void modifyVirtualMachine(VirtualMachine vm, boolean confirmed) throws InvalidName, VmConfigFault, DuplicateName, TaskInProgress, FileFault, InvalidState, ConcurrentAccess, InvalidDatastore, InsufficientResourcesFault, RuntimeFault, RemoteException, InterruptedException {
		VirtualMachineConfigSpec spec = new VirtualMachineConfigSpec();
		if (Configuration.getString("action").toLowerCase().equals("modify")) {
			if (Configuration.has("description")) {
				spec.setAnnotation(Configuration.getString("description"));
			} else if (Configuration.has("cpu") || Configuration.has("memory")) {
				VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOff);

				if (Configuration.has("cpu")) {
					spec.setNumCPUs(Integer.parseInt(Configuration.getString("cpu")));
					spec.setNumCoresPerSocket(1);
				} else if (Configuration.has("memory")) {
					spec.setMemoryMB(Long.parseLong(Configuration.getString("memory")));
				}
			} else if (Configuration.has("parameter")) {
				if (!Configuration.has("value")) {
					Formatter.usageError("When modifying parameters, also specify --value.", "MODIFYVM", true);
				}

				OptionValue parameter = new OptionValue();
				parameter.setKey(Configuration.getString("parameter"));
				parameter.setValue(Configuration.getString("value"));

				spec.setExtraConfig(new OptionValue[] { parameter });
			} else if (Configuration.has("boot")) {
				VirtualMachineBootOptions bootopts = new VirtualMachineBootOptions();
				String[] order = Configuration.getString("boot").split(",");
				if (order.length < 1) {
					throw new RuntimeException("Failure: At least one boot option must be provided");
				}
				List<VirtualMachineBootOptionsBootableDevice> bootorder = new ArrayList<VirtualMachineBootOptionsBootableDevice>(order.length);
				for (String ord : order) {
					BootDeviceType type = null;
					try {
						type = BootDeviceType.valueOf(ord.toUpperCase());
					} catch (IllegalArgumentException iae) {
						throw new RuntimeException("Failure: Invalid boot device specified: " + ord);
					}
					switch (type) {
					case CDROM:
						VirtualMachineBootOptionsBootableCdromDevice cd = new VirtualMachineBootOptionsBootableCdromDevice();
						bootorder.add(cd);
						break;
					case FLOPPY:
						VirtualMachineBootOptionsBootableFloppyDevice floppy = new VirtualMachineBootOptionsBootableFloppyDevice();
						bootorder.add(floppy);
						break;
					case DISK:
						VirtualMachineBootOptionsBootableDiskDevice disk = new VirtualMachineBootOptionsBootableDiskDevice();
						List<VirtualDisk> disks = VsphereQuery.getVirtualMachineDiskDrives(vm);
						if (disks.size() < 1) {
							throw new RuntimeException("Failure: VirtualMachine has no disks");
						}
						disk.setDeviceKey(disks.get(0).getKey());
						bootorder.add(disk);
						break;
					case NETWORK:
						VirtualMachineBootOptionsBootableEthernetDevice nic = new VirtualMachineBootOptionsBootableEthernetDevice();
						VirtualEthernetCard[] nics = VsphereQuery.getVirtualMachineNetworks(vm).values().toArray(new VirtualEthernetCard[0]);
						if (nics.length < 1) {
							throw new RuntimeException("Failure: VirtualMachine has no NICs");
						}
						nic.setDeviceKey(nics[0].getKey());
						bootorder.add(nic);
						break;
					}
				}

				bootopts.setBootOrder(bootorder.toArray(new VirtualMachineBootOptionsBootableDevice[0]));
				spec.setBootOptions(bootopts);
			} else if (Configuration.has("cpureservation") || Configuration.has("memreservation")) {
				if (Configuration.has("cpureservation")) {
					ResourceAllocationInfo resource = new ResourceAllocationInfo();
					long res = Long.parseLong(Configuration.getString("cpureservation"));
					resource.setReservation(res);
					spec.setCpuAllocation(resource);
					Formatter.printInfoLine(String.format("Setting CPU reservation to %d Mhz", res));
				}
				if (Configuration.has("memreservation")) {
					ResourceAllocationInfo resource = new ResourceAllocationInfo();
					long res = Long.parseLong(Configuration.getString("memreservation"));
					resource.setReservation(Long.parseLong(Configuration.getString("memreservation")));
					spec.setMemoryAllocation(resource);
					Formatter.printInfoLine(String.format("Setting Memory reservation to %d MB", res));
				}
			} else if (Configuration.has("iopslimit") && Configuration.has("value")) {
				List<VirtualDisk> disks = VsphereQuery.getVirtualMachineDiskDrives(vm);
				for (VirtualDisk virtualDisk : disks) {
					if (virtualDisk.getDeviceInfo().getLabel().equals(Configuration.getString("iopslimit"))) {
						StorageIOAllocationInfo iops = virtualDisk.getStorageIOAllocation();
						VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

						iops.setLimit(Long.parseLong(Configuration.getString("value")));
						virtualDisk.setStorageIOAllocation(iops);
						diskSpec.setDevice(virtualDisk);
						diskSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
						spec.setDeviceChange(new VirtualDeviceConfigSpec[] { diskSpec });

						Formatter.printInfoLine(String.format("Setting IOPS limit for %s to %s", Configuration.getString("iopslimit"), Configuration.getString("value")));
						break;
					}
				}
			} else {
				throw new RuntimeException("Failure: invalid combination of options for modifying VMs");
			}
		} else if (Configuration.getString("action").toLowerCase().equals("add")) {
			if (Configuration.has("network")) {
				VirtualEthernetCardMacType type;
				if (Configuration.has("mac")) {
					type = VirtualEthernetCardMacType.manual;
				} else {
					type = VirtualEthernetCardMacType.generated;
				}
				ConfigTarget configTarget = vm.getEnvironmentBrowser().queryConfigTarget(null);
				List<DistributedVirtualPortgroupInfo> portGroups = VsphereQuery.getVirtualPortgroupsForConfigTarget(configTarget, Configuration.getString("network"));
				List<DistributedVirtualSwitchInfo> switches = VsphereQuery.getVirtualSwitchesForConfigTarget(configTarget, "dvSwitch");

				if (portGroups.size() < 1) {
					Formatter.printErrorLine("Failed to retrieve requested PortGroup");
					System.exit(-1);
				}

				if (switches.size() < 1) {
					Formatter.printErrorLine("Failed to retrieve Switches");
					System.exit(-1);
				}

				String networkName = portGroups.get(0).getPortgroupName();
				String switchUuid = switches.get(0).getSwitchUuid();

				DistributedVirtualSwitchPortConnection port = VsphereFactory.getPortForNetworkAndSwitch(networkName, switchUuid);
				VirtualDeviceConfigSpec dev = VsphereFactory.getVirtualNicForPortGroup(port, type, Configuration.getString("mac"), VirtualDeviceConfigSpecOperation.add);
				spec.setDeviceChange(new VirtualDeviceConfigSpec[] { dev });
			} else if ((Configuration.has("odd") || Configuration.has("floppy")) && Configuration.has("storage")) {
				VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOff);

				VirtualDeviceConfigSpec dev = null;
				if (Configuration.has("odd")) {
					dev = VsphereFactory.getCdromDrive(vm, Configuration.getString("storage"), Configuration.getString("dc"), Configuration.getString("odd"));
				} else {
					dev = VsphereFactory.getFloppyDiskDrive(vm, Configuration.getString("storage"), Configuration.getString("dc"), Configuration.getString("floppy"));
				}
				spec.setDeviceChange(new VirtualDeviceConfigSpec[] { dev });
			} else {
				throw new RuntimeException("Failure: invalid combination of options for modifying VMs");
			}
		} else if (Configuration.getString("action").toLowerCase().equals("remove")) {
			if (Configuration.has("network")) {
				Map<String, VirtualEthernetCard> networks = VsphereQuery.getVirtualMachineNetworks(vm);
				VirtualEthernetCard card = null;
				for (java.util.Map.Entry<String, VirtualEthernetCard> netw : networks.entrySet()) {
					if (netw.getKey().contains(Configuration.getString("network"))) {
						card = netw.getValue();
					}
				}
				if (card == null) {
					Formatter.printErrorLine("Failed to retrieve EthernetCard");
					System.exit(-1);
				}
				VirtualDeviceConfigSpec dev = new VirtualDeviceConfigSpec();
				dev.setDevice(card);
				dev.setOperation(VirtualDeviceConfigSpecOperation.remove);
				spec.setDeviceChange(new VirtualDeviceConfigSpec[] { dev });
			} else if ((Configuration.has("odd") || Configuration.has("floppy")) && Configuration.has("storage")) {
				VsphereQuery.checkVirtualMachinePowerState(vm, VirtualMachinePowerState.poweredOff);

				VirtualDevice device = null;
				if (Configuration.has("odd")) {
					List<VirtualCdrom> cdroms = VsphereQuery.getVirtualMachineCdromDrives(vm);
					for (VirtualCdrom virtualCdrom : cdroms) {
						if (((VirtualCdromIsoBackingInfo) virtualCdrom.getBacking()).getFileName().contains(Configuration.getString("odd"))) {
							device = virtualCdrom;
						}
					}
				} else {
					List<VirtualFloppy> floppies = VsphereQuery.getVirtualMachineFloppyDrives(vm);
					for (VirtualFloppy virtualFloppy : floppies) {
						if (((VirtualFloppyImageBackingInfo) virtualFloppy.getBacking()).getFileName().contains(Configuration.getString("floppy"))) {
							device = virtualFloppy;
						}
					}
				}

				if (device == null) {
					Formatter.printErrorLine("Could not find any devices with the given file");
					System.exit(-1);
				}

				VirtualDeviceConfigSpec dev = new VirtualDeviceConfigSpec();
				dev.setDevice(device);
				dev.setOperation(VirtualDeviceConfigSpecOperation.remove);
				spec.setDeviceChange(new VirtualDeviceConfigSpec[] { dev });
			} else {
				throw new RuntimeException("Failure: invalid combination of options for modifying VMs");
			}
		} else {
			throw new RuntimeException("Failure: invalid combination of options for modifying VMs");
		}

		if (!confirmed) {
			Formatter.printInfoLine(String.format("WhatIf: Would have modified VM %s now, but confirmation was not given.", vm.getName()));
			return;
		}

		Task modifyTask = vm.reconfigVM_Task(spec);
		if (modifyTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM modified successfully");
		} else {
			LocalizedMethodFault error = modifyTask.getTaskInfo().getError();
			throw new RuntimeException("Failure: modifying [ " + vm.getName() + " ] VM: " + error == null ? "" : error.getLocalizedMessage());
		}
	}

	protected static void VMFolderListMode(ListModeType listType) throws InvalidProperty, RuntimeFault, RemoteException {
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

		int depth = 0;
		if (Configuration.has("depth")) {
			try {
				depth = Integer.parseInt(Configuration.getString("depth"));
			} catch (NumberFormatException nfe) {
				Formatter.printErrorLine("Failed to parse --depth value, using 0 instead");
			}
		}

		Map<String, ManagedObjectReference> objects;
		if (listType == ListModeType.FOLDER) {
			objects = VsphereQuery.findVirtualMachineFolders(Configuration.getString("dc"), rootFolder, depth);
		} else {
			ManagedObjectReference folder = VsphereQuery.findVirtualMachineFolder(Configuration.getString("dc"), rootFolder, 0);

			objects = VsphereQuery.findVirtualMachines(null, folder, depth);
		}

		if (objects == null || objects.isEmpty()) {
			Formatter.printInfoLine("No objects found!");
		} else {
			Map<String, ManagedObjectReference> sorted = new TreeMap<String, ManagedObjectReference>(objects);
			Formatter.printBorderedInfo(String.format("Objects found in folder: %s\n", rootFolder));
			for (java.util.Map.Entry<String, ManagedObjectReference> object : sorted.entrySet()) {
				if (listType == ListModeType.VM && Configuration.has("fqdn") && !object.getKey().contains(Configuration.getString("fqdn"))) {
					continue;
				}

				if (!Configuration.has("detailed") || listType == ListModeType.FOLDER) {
					Formatter.printInfoLine(object.getKey());
				}

				if (listType == ListModeType.VM && (Configuration.has("detailed") || Configuration.has("properties"))) {
					VirtualMachine vm = new VirtualMachine(VsphereManager.getServerConnection(), object.getValue());
					if (Configuration.has("detailed")) {
						HostSystem host = new HostSystem(VsphereManager.getServerConnection(), vm.getRuntime().getHost());
						Map<String, VirtualEthernetCard> cards = VsphereQuery.getVirtualMachineNetworks(vm);
						List<String> card_info = new ArrayList<String>(cards.size());
						for (java.util.Map.Entry<String, VirtualEthernetCard> card : cards.entrySet()) {
							card_info.add(String.format("%s@%s", card.getValue().getMacAddress(), card.getKey()));
						}
						String networks = StringUtils.join(card_info, " | ");
						String annotation = vm.getConfig().getAnnotation();

						String vmTools = "unknown";
						VirtualMachineToolsStatus vmToolsStatus = vm.getGuest().getToolsStatus();

						switch (vmToolsStatus) {
						case toolsOk:
						case toolsOld:
							vmTools = "installed";
							break;
						case toolsNotInstalled:
							vmTools = "not installed";
							break;
						case toolsNotRunning:
							vmTools = "not running";
							break;
						}

						// FQDN ESXNODE CPU/MEM ReservationC/ReservationM Tools
						Formatter.printInfoLine(String.format("%-53s %20s CPU:%d/MEM:%d RCPU:%d/RMEM:%d Tools:%s", object.getKey(), host.getName(), VsphereQuery.getVirtualMachineCPU(vm), VsphereQuery.getVirtualMachineMemory(vm), vm.getResourceConfig().getCpuAllocation().getReservation(), vm.getResourceConfig().getMemoryAllocation().getReservation(), vmTools));
						// MAC@Network... Description
						Formatter.printInfoLine(String.format("- [%s] [%s]", networks, annotation == null ? "" : annotation.replace('\n', ' ')));
						String diskInfo = "-";
						List<VirtualDisk> disks = VsphereQuery.getVirtualMachineDiskDrives(vm);
						for (VirtualDisk virtualDisk : disks) {
							String limit = virtualDisk.getStorageIOAllocation().getLimit().toString();
							if (limit.equals("-1")) {
								limit = "unlimited";
							}
							diskInfo += String.format(" [%s Size: %d MB IOPS: %s]", virtualDisk.getDeviceInfo().getLabel(), virtualDisk.getCapacityInKB() / 1024, limit);
						}
						Formatter.printInfoLine(diskInfo);
					}
					if (Configuration.has("properties")) {
						Formatter.printInfoLine("- Virtual Machine properties");
						OptionValue[] props = vm.getConfig().getExtraConfig();
						Arrays.sort(props, new Comparator<OptionValue>() {
							@Override
							public int compare(OptionValue o1, OptionValue o2) {
								return o1.getKey().compareTo(o2.getKey());
							}
						});
						for (OptionValue val : props) {
							Formatter.printInfoLine(String.format(" - [%s - %s]", val.getKey(), val.getValue()));
						}
					}
				}
			}
		}
	}

	protected static void ComputeFolderListMode(ListModeType listType) throws InvalidProperty, RuntimeFault, RemoteException {
		if (!Configuration.has("cluster") && (listType == ListModeType.ESXNODE || listType == ListModeType.NETWORK || listType == ListModeType.STORAGE)) {
			Formatter.usageError("ESXNode, Network and Storage listing requires --cluster", "LIST", true);
		}

		Map<String, ClusterComputeResource> objects = VsphereQuery.getClustersForDatacenter(Configuration.getString("dc"));
		if (listType == ListModeType.CLUSTER) {
			Formatter.printBorderedInfo("Objects found\n");

			Map<String, ClusterComputeResource> sorted = new TreeMap<String, ClusterComputeResource>(objects);
			for (Entry<String, ClusterComputeResource> object : sorted.entrySet()) {
				Formatter.printInfoLine(object.getKey());
			}
			return;
		}

		ClusterComputeResource cluster = objects.get(Configuration.getString("cluster"));
		if (cluster == null) {
			Formatter.printErrorLine("Could not find cluster");
			return;
		}

		ManagedEntity[] childs = new ManagedEntity[0];
		switch (listType) {
		case ESXNODE:
			childs = cluster.getHosts();
			break;
		case NETWORK:
			childs = cluster.getNetworks();
			break;
		case STORAGE:
			childs = cluster.getDatastores();
			break;
		default:
			Formatter.printErrorLine("Invalid list mode selected for ComputeFolderListMode");
			System.exit(-1);
			break;
		}

		Arrays.sort(childs, new Comparator<ManagedEntity>() {
			@Override
			public int compare(ManagedEntity o1, ManagedEntity o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		Formatter.printBorderedInfo(String.format("Objects found in cluster: %s\n", Configuration.getString("cluster")));

		for (ManagedEntity child : childs) {
			Formatter.printInfoLine(child.getName());
		}
	}

	protected static void DatastoreListMode(ManagedObjectReference dsref) throws FileFault, InvalidDatastore, RuntimeFault, RemoteException, InterruptedException {
		VsphereClient.DatastoreListMode(new Datastore(VsphereManager.getServerConnection(), dsref));
	}

	protected static void DatastoreListMode(Datastore ds) throws FileFault, InvalidDatastore, RuntimeFault, RemoteException, InterruptedException {
		FileQueryFlags flags = new FileQueryFlags();
		flags.setFileType(true);
		flags.setFileOwner(true);
		flags.setFileSize(true);

		HostDatastoreBrowserSearchSpec spec = new HostDatastoreBrowserSearchSpec();
		spec.setDetails(flags);

		Task t = ds.getBrowser().searchDatastoreSubFolders_Task(VsphereQuery.getVolumeName(ds.getInfo().getName()), spec);
		t.waitForTask();
		Object res = VsphereQuery.getTaskInfoResult(t);

		List<String> paths = new ArrayList<String>();

		ArrayOfHostDatastoreBrowserSearchResults results = (ArrayOfHostDatastoreBrowserSearchResults) res;
		for (HostDatastoreBrowserSearchResults r : results.getHostDatastoreBrowserSearchResults()) {
			String root = r.getFolderPath().replace(VsphereQuery.getVolumeName(ds.getInfo().getName()) + " ", "").replace(VsphereQuery.getVolumeName(ds.getInfo().getName()), "");
			for (FileInfo file : r.getFile()) {
				paths.add(String.format("%s%s", root, file.getPath()));
			}
		}

		Collections.sort(paths);

		Formatter.printBorderedInfo(String.format("Objects found in datastore: %s\n", ds.getInfo().getName()));

		for (String path : paths) {
			Formatter.printInfoLine(path);
		}
	}
}
