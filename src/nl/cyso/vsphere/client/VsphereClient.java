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
import java.util.List;

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualEthernetCardMacType;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualSCSISharing;

public class VsphereClient {
	private static VirtualMachineConfigSpec createVmConfigSpec(String datastoreName, int diskSizeMB, String mac, String network, ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ConfigTarget configTarget = null;
		try {
			configTarget = VsphereQuery.getConfigTargetForHost(computeResMor, hostMor);
		} catch (Exception e) {
			Formatter.printErrorLine("Failed to retrieve Config Target");
			Formatter.printError(e);
			System.exit(-1);
		}

		List<DistributedVirtualPortgroupInfo> portGroups = VsphereQuery.getVirtualPortgroupsForConfigTarget(configTarget, network);
		List<DistributedVirtualSwitchInfo> switches = VsphereQuery.getVirtualSwitchesForConfigTarget(configTarget, "dvSwitch");
		List<DatastoreSummary> datastores = VsphereQuery.getDatastoresForConfigTarget(configTarget, datastoreName);

		if (portGroups.size() < 1) {
			Formatter.printErrorLine("Failed to retrieve requested PortGroup");
			System.exit(-1);
		}

		if (switches.size() < 1) {
			Formatter.printErrorLine("Failed to retrieve Switches");
			System.exit(-1);
		}

		if (datastores.size() < 1) {
			Formatter.printErrorLine("Failed to retrieve DataStores");
			System.exit(-1);
		}

		String networkName = portGroups.get(0).getPortgroupName();
		String switchUuid = switches.get(0).getSwitchUuid();
		ManagedObjectReference datastoreRef = datastores.get(0).getDatastore();
		String datastoreVolume = VsphereQuery.getVolumeName(datastoreName);

		VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
		vmfi.setVmPathName(String.format("%s", datastoreVolume));

		VirtualDeviceConfigSpec scsiCtrlSpec = VsphereFactory.getScsiController(0, VirtualSCSISharing.NO_SHARING, VirtualDeviceConfigSpecOperation.ADD);

		if (diskSizeMB < 10 * 1024) {
			diskSizeMB = 10 * 1024;
		}

		VirtualDeviceConfigSpec disk1Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 0, datastoreRef, datastoreName, 10 * 1024);
		VirtualDeviceConfigSpec disk2Spec = null;

		if (diskSizeMB > 10 * 1024) {
			disk2Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 1, datastoreRef, datastoreName, diskSizeMB - 10 * 1024);
		}

		DistributedVirtualSwitchPortConnection port = VsphereFactory.getPortForNetworkAndSwitch(networkName, switchUuid);
		VirtualDeviceConfigSpec nicSpec = VsphereFactory.getVirtualNicForPortGroup(port, VirtualEthernetCardMacType.MANUAL, Configuration.getString("mac"), VirtualDeviceConfigSpecOperation.ADD);

		List<VirtualDeviceConfigSpec> deviceConfigSpec = new ArrayList<VirtualDeviceConfigSpec>();
		deviceConfigSpec.addAll(Arrays.asList(scsiCtrlSpec, disk1Spec, nicSpec));

		if (disk2Spec != null) {
			deviceConfigSpec.add(disk2Spec);
		}

		VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
		configSpec.setFiles(vmfi);
		configSpec.getDeviceChange().addAll(deviceConfigSpec);

		return configSpec;
	}

	public static void createVirtualMachine() throws RemoteException, Exception {
		ManagedObjectReference dcmor = VsphereQuery.getMOREFsInContainerByType(VsphereManager.getServiceContent().getRootFolder(), "Datacenter").get(Configuration.get("dc"));

		if (dcmor == null) {
			Formatter.printErrorLine("Datacenter " + Configuration.get("dc") + " not found.");
			return;
		}

		ManagedObjectReference hostmor = VsphereQuery.getMOREFsInContainerByType(dcmor, "HostSystem").get(Configuration.get("esxnode"));
		if (hostmor == null) {
			Formatter.printErrorLine("Host " + Configuration.get("esxnode") + " not found");
			return;
		}

		ManagedObjectReference crmor = (ManagedObjectReference) VsphereQuery.getEntityProps(hostmor, new String[] { "parent" }).get("parent");
		if (crmor == null) {
			Formatter.printErrorLine("No Compute Resource Found On Specified Host");
			return;
		}

		ManagedObjectReference resourcepoolmor = (ManagedObjectReference) VsphereQuery.getEntityProps(crmor, new String[] { "resourcePool" }).get("resourcePool");
		ManagedObjectReference vmFolderMor = (ManagedObjectReference) VsphereQuery.getEntityProps(dcmor, new String[] { "vmFolder" }).get("vmFolder");

		VirtualMachineConfigSpec vmConfigSpec = VsphereClient.createVmConfigSpec(Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), Configuration.getString("mac"), Configuration.getString("network"), crmor, hostmor);
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation(Configuration.getString("description"));
		vmConfigSpec.setMemoryMB(Long.valueOf(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.valueOf(Configuration.getString("cpu")));
		vmConfigSpec.setNumCoresPerSocket(1);
		vmConfigSpec.setGuestId(VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST.value());

		ManagedObjectReference taskmor = VsphereManager.getVimPort().createVMTask(vmFolderMor, vmConfigSpec, resourcepoolmor, hostmor);
		if (VsphereQuery.getTaskResultAfterDone(taskmor)) {
			System.out.printf("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn"));
		} else {
			String msg = "Failure: Creating [ " + Configuration.get("fqdn") + "] VM";
			throw new RuntimeException(msg);
		}
		ManagedObjectReference vmMor = (ManagedObjectReference) VsphereQuery.getEntityProps(taskmor, new String[] { "info.result" }).get("info.result");

		// Start the Newly Created VM.
		System.out.println("Powering on the newly created VM " + Configuration.get("fqdn"));
		VsphereClient.powerOnVM(vmMor);

		VsphereManager.disconnect();
	}

	public static void powerOnVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		ManagedObjectReference cssTask = VsphereManager.getVimPort().powerOnVMTask(vmMor, null);
		if (VsphereQuery.getTaskResultAfterDone(cssTask)) {
			System.out.println("Success: VM powered on successfully");
		} else {
			String msg = "Failure: starting [ " + vmMor.getValue() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void powerOffVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		ManagedObjectReference cssTask = VsphereManager.getVimPort().powerOffVMTask(vmMor);
		if (VsphereQuery.getTaskResultAfterDone(cssTask)) {
			System.out.println("Success: VM powered off successfully");
		} else {
			String msg = "Failure: starting [ " + vmMor.getValue() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void shutdownVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		VsphereManager.getVimPort().shutdownGuest(vmMor);
		System.out.println("Success: VM shutdown requested");
	}
}
