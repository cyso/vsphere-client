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
	/**
	 * Creates the vm config spec object.
	 * 
	 * @param vmName the vm name
	 * @param datastoreName the datastore name
	 * @param diskSizeMB the disk size in mb
	 * @param computeResMor the compute res moref
	 * @param hostMor the host mor
	 * @return the virtual machine config spec object
	 * @throws RuntimeFaultFaultMsg
	 * @throws InvalidPropertyFaultMsg
	 * @throws Exception the exception
	 */
	private VirtualMachineConfigSpec createVmConfigSpec(String datastoreName, int diskSizeMB, String mac, String network, ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
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
		VirtualDeviceConfigSpec diskSpec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 0, datastoreRef, datastoreName, diskSizeMB);

		ManagedObjectReference netw = VsphereQuery.getMOREFsInContainerByType(VsphereManager.getServiceContent().getRootFolder(), "DistributedVirtualPortgroup").get(networkName);
		DistributedVirtualSwitchPortConnection port = new DistributedVirtualSwitchPortConnection();
		port.setPortgroupKey(netw.getValue());
		port.setSwitchUuid(switchUuid);

		VirtualDeviceConfigSpec nicSpec = VsphereFactory.getVirtualNicForPortGroup(port, VirtualEthernetCardMacType.MANUAL, Configuration.getString("mac"), VirtualDeviceConfigSpecOperation.ADD);

		List<VirtualDeviceConfigSpec> deviceConfigSpec = Arrays.asList(scsiCtrlSpec, diskSpec, nicSpec);

		VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
		configSpec.setFiles(vmfi);
		configSpec.getDeviceChange().addAll(deviceConfigSpec);

		return configSpec;
	}

	public void createVirtualMachine() throws RemoteException, Exception {
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
		VirtualMachineConfigSpec vmConfigSpec = this.createVmConfigSpec(Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), Configuration.getString("mac"), Configuration.getString("network"), crmor, hostmor);
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation("VirtualMachine Annotation");
		vmConfigSpec.setMemoryMB(Long.valueOf(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.valueOf(Configuration.getString("cpu")));
		vmConfigSpec.setGuestId(VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST.value());
		ManagedObjectReference taskmor = VsphereManager.getVimPort().createVMTask(vmFolderMor, vmConfigSpec, resourcepoolmor, hostmor);
		if (VsphereQuery.getTaskResultAfterDone(taskmor)) {
			System.out.printf("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn"));
		} else {
			String msg = "Failure: Creating [ " + Configuration.get("fqdn") + "] VM";
			throw new RuntimeException(msg);
		}
		ManagedObjectReference vmMor = (ManagedObjectReference) VsphereQuery.getEntityProps(taskmor, new String[] { "info.result" }).get("info.result");
		System.out.println("Powering on the newly created VM " + Configuration.get("fqdn"));

		// Start the Newly Created VM.
		// this.powerOnVM(vmMor);

		VsphereManager.disconnect();
	}

	/**
	 * Power on vm.
	 * 
	 * @param vmMor the vm moref
	 * @throws RemoteException the remote exception
	 * @throws Exception the exception
	 */
	public void powerOnVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		ManagedObjectReference cssTask = VsphereManager.getVimPort().powerOnVMTask(vmMor, null);
		if (VsphereQuery.getTaskResultAfterDone(cssTask)) {
			System.out.println("Success: VM started Successfully");
		} else {
			String msg = "Failure: starting [ " + vmMor.getValue() + "] VM";
			throw new RuntimeException(msg);
		}
	}

}
