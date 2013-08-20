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
import java.util.List;
import java.util.Map;

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.ConcurrentAccess;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.FileFault;
import com.vmware.vim25.InsufficientResourcesFault;
import com.vmware.vim25.InvalidDatastore;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardMacType;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VsphereClient {
	public static ManagedObjectReference createVirtualMachine() throws RemoteException, Exception {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(Configuration.getString("dc"));
		if (dcmor == null) {
			throw new RuntimeException("Datacenter " + Configuration.get("dc") + " not found.");
		}

		ManagedObjectReference hostmor = VsphereQuery.getHostNodeReference(Configuration.getString("esxnode"), dcmor);
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

		VirtualMachineConfigSpec vmConfigSpec = VsphereFactory.createVirtualMachineConfigSpec(Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), Configuration.getString("mac"), Configuration.getString("network"), crmor, hostmor);
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation(Configuration.getString("description"));
		vmConfigSpec.setMemoryMB(Long.parseLong(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.parseInt(Configuration.getString("cpu")));
		vmConfigSpec.setNumCoresPerSocket(1);
		vmConfigSpec.setGuestId(VirtualMachineGuestOsIdentifier.ubuntu64Guest.toString());

		Task task = vmFolder.createVM_Task(vmConfigSpec, new ResourcePool(VsphereManager.getServerConnection(), resourcepoolmor), new HostSystem(VsphereManager.getServerConnection(), hostmor));
		if (task.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine(String.format("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn")));
		} else {
			String msg = "Failure: Creating [ " + Configuration.get("fqdn") + "] VM";
			throw new RuntimeException(msg);
		}
		return VsphereQuery.getTaskInfoResult(task);
	}

	public static void powerOnVirtualMachine(ManagedObjectReference vmmor) throws RemoteException, Exception {
		VsphereClient.powerOnVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor));
	}

	public static void powerOnVirtualMachine(VirtualMachine vm) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState != VirtualMachinePowerState.poweredOff) {
			Formatter.printInfoLine("Warning: VM was not in a powered off state, no action performed.");
			return;
		}

		Task powerOnTask = vm.powerOnVM_Task(null);
		if (powerOnTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM powered on successfully");
		} else {
			String msg = "Failure: starting [ " + vm.getName() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void powerOffVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.powerOffVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void powerOffVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState != VirtualMachinePowerState.poweredOn) {
			Formatter.printInfoLine("Warning: VM was not in a powered on state, no action performed.");
			return;
		}

		if (!confirmed) {
			Formatter.printInfoLine(String.format("WhatIf: Would have powered off VM %s now, but confirmation was not given.", vm.getName()));
			return;
		}

		Task powerOffTask = vm.powerOffVM_Task();
		if (powerOffTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM powered off successfully");
		} else {
			String msg = "Failure: stopping [ " + vm.getName() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void shutdownVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.shutdownVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void shutdownVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState == VirtualMachinePowerState.poweredOn) {
			if (!confirmed) {
				Formatter.printInfoLine(String.format("WhatIf: Would have shutdown VM %s now, but confirmation was not given.", vm.getName()));
				return;
			}

			vm.shutdownGuest();
			Formatter.printInfoLine("Success: VM shutdown message sent successfully");
		} else {
			Formatter.printInfoLine("Warning: VM was not in a powered on state, no action performed.");
		}
	}

	public static void deleteVirtualMachine(ManagedObjectReference vmmor, boolean confirmed) throws RemoteException, Exception {
		VsphereClient.deleteVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor), confirmed);
	}

	public static void deleteVirtualMachine(VirtualMachine vm, boolean confirmed) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState == VirtualMachinePowerState.poweredOn) {
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
			String msg = "Failure: destroying [ " + vm.getName() + "] VM";
			throw new RuntimeException(msg);
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
				VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
				if (powerState == VirtualMachinePowerState.poweredOn) {
					throw new RuntimeException("Invalid power state: Machine is powered on");
				}
				if (Configuration.has("cpu")) {
					spec.setNumCPUs(Integer.parseInt(Configuration.getString("cpu")));
					spec.setNumCoresPerSocket(1);
				} else if (Configuration.has("memory")) {
					spec.setMemoryMB(Long.parseLong(Configuration.getString("memory")));
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
			throw new RuntimeException("Failure: modifying [ " + vm.getName() + " ] VM: " + modifyTask.getTaskInfo().getError().getLocalizedMessage());
		}
	}
}
