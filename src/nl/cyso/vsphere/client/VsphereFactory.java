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
import java.util.Random;

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardMacType;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyDeviceBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VirtualVmxnet3;

public class VsphereFactory {
	private static int key = new Random().nextInt(10) + 10;

	private static int getKey() {
		return key++;
	}

	protected static VirtualMachineConfigSpec createVirtualMachineConfigSpec(String datastoreName, int diskSizeMB, String mac, String network, ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws RuntimeFault, RemoteException {
		ConfigTarget configTarget = null;
		try {
			configTarget = VsphereQuery.getConfigTargetForHost(computeResMor, hostMor);
		} catch (Exception e) {
			Formatter.printErrorLine("Failed to retrieve Config Target");
			Formatter.printStackTrace(e);
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

		VirtualDeviceConfigSpec scsiCtrlSpec = VsphereFactory.getScsiController(0, VirtualSCSISharing.noSharing, VirtualDeviceConfigSpecOperation.add);

		if (diskSizeMB < 10 * 1024) {
			diskSizeMB = 10 * 1024;
		}

		VirtualDeviceConfigSpec disk1Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 0, datastoreRef, datastoreName, 10 * 1024);
		VirtualDeviceConfigSpec disk2Spec = null;

		if (diskSizeMB > 10 * 1024) {
			disk2Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 1, datastoreRef, datastoreName, diskSizeMB - 10 * 1024);
		}

		DistributedVirtualSwitchPortConnection port = VsphereFactory.getPortForNetworkAndSwitch(networkName, switchUuid);
		VirtualDeviceConfigSpec nicSpec = VsphereFactory.getVirtualNicForPortGroup(port, VirtualEthernetCardMacType.manual, Configuration.getString("mac"), VirtualDeviceConfigSpecOperation.add);

		VirtualDeviceConfigSpec[] deviceConfigSpec = null;
		if (disk2Spec != null) {
			deviceConfigSpec = new VirtualDeviceConfigSpec[] { scsiCtrlSpec, disk1Spec, disk2Spec, nicSpec };
		} else {
			deviceConfigSpec = new VirtualDeviceConfigSpec[] { scsiCtrlSpec, disk1Spec, nicSpec };
		}

		VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
		configSpec.setFiles(vmfi);
		configSpec.setDeviceChange(deviceConfigSpec);

		return configSpec;
	}

	/**
	 * Creates the virtual disk.
	 * 
	 * @param volName the vol name
	 * @param controllerKey the disk ctlr key
	 * @param datastoreRef the datastore ref
	 * @param diskSizeMB the disk size in mb
	 * @return the virtual device config spec object
	 */
	protected static VirtualDeviceConfigSpec createVirtualDisk(int controllerKey, int unit, ManagedObjectReference datastoreRef, String volName, int diskSizeMB) {
		VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

		VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();

		diskfileBacking.setFileName("");
		diskfileBacking.setDatastore(datastoreRef);
		diskfileBacking.setDiskMode(VirtualDiskMode.persistent.toString());
		diskfileBacking.setThinProvisioned(false);
		diskfileBacking.setEagerlyScrub(false);

		VirtualDisk disk = new VirtualDisk();
		disk.setKey(new Integer(0));
		disk.setControllerKey(controllerKey);
		disk.setCapacityInKB(1024 * diskSizeMB);
		disk.setBacking(diskfileBacking);
		disk.setKey(VsphereFactory.getKey());
		disk.setUnitNumber(unit);

		diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);
		diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
		diskSpec.setDevice(disk);

		return diskSpec;
	}

	protected static VirtualDeviceConfigSpec getScsiController(int bus, VirtualSCSISharing sharing, VirtualDeviceConfigSpecOperation action) {
		VirtualSCSIController scsiCtrl = new ParaVirtualSCSIController();
		scsiCtrl.setBusNumber(bus);
		scsiCtrl.setSharedBus(sharing);
		scsiCtrl.setKey(VsphereFactory.getKey());

		VirtualDeviceConfigSpec scsiCtrlSpec = new VirtualDeviceConfigSpec();
		scsiCtrlSpec.setOperation(action);
		scsiCtrlSpec.setDevice(scsiCtrl);

		return scsiCtrlSpec;
	}

	protected static VirtualDeviceConfigSpec getFloppyDiskDrive(String deviceName, VirtualDeviceConfigSpecOperation action) {
		VirtualFloppy floppy = new VirtualFloppy();
		floppy.setKey(VsphereFactory.getKey());

		VirtualFloppyDeviceBackingInfo flpBacking = new VirtualFloppyDeviceBackingInfo();
		flpBacking.setDeviceName(deviceName);
		floppy.setBacking(flpBacking);

		VirtualDeviceConfigSpec floppySpec = new VirtualDeviceConfigSpec();
		floppySpec.setOperation(action);
		floppySpec.setDevice(floppy);

		return floppySpec;
	}

	protected static DistributedVirtualSwitchPortConnection getPortForNetworkAndSwitch(String networkName, String switchUuid) throws RuntimeFault, RemoteException {
		ManagedObjectReference netw = VsphereQuery.getMOREFsInContainerByType(VsphereManager.getServiceInstance().getRootFolder().getMOR(), "DistributedVirtualPortgroup").get(networkName);
		DistributedVirtualSwitchPortConnection port = new DistributedVirtualSwitchPortConnection();
		port.setPortgroupKey(netw.getVal());
		port.setSwitchUuid(switchUuid);

		return port;
	}

	protected static VirtualDeviceConfigSpec getVirtualNicForPortGroup(DistributedVirtualSwitchPortConnection port, VirtualEthernetCardMacType mac, String customMac, VirtualDeviceConfigSpecOperation action) {
		VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
		nicBacking.setPort(port);

		VirtualEthernetCard nic = new VirtualVmxnet3();
		nic.setBacking(nicBacking);
		nic.setKey(VsphereFactory.getKey());
		nic.setAddressType(mac.toString());
		if (mac == VirtualEthernetCardMacType.manual) {
			nic.setMacAddress(customMac);
		}

		VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
		nicSpec.setOperation(action);
		nicSpec.setDevice(nic);

		return nicSpec;
	}
}
