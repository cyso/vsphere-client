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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardMacType;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyImageBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VirtualVmxnet3;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.VirtualMachine;

public class VsphereFactory {
	private static int key = new Random().nextInt(10) + 10;

	private static int getKey() {
		return key++;
	}

	protected static VirtualMachineConfigSpec createVirtualMachineConfigSpec(String datastoreName, int diskSizeMB, int diskSplit, boolean suggestDatastore, String mac, String network, ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws RuntimeFault, RemoteException {
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

		VirtualEthernetCardMacType macmode = VirtualEthernetCardMacType.manual;
		if (mac == null) {
			macmode = VirtualEthernetCardMacType.generated;
		}

		VirtualDeviceConfigSpec nicSpec = null;
		if (portGroups.size() >= 1 && switches.size() >= 1) {
			String networkName = portGroups.get(0).getPortgroupName();
			String switchUuid = switches.get(0).getSwitchUuid();

			DistributedVirtualSwitchPortConnection port = VsphereFactory.getPortForNetworkAndSwitch(networkName, switchUuid);
			nicSpec = VsphereFactory.getVirtualNicForPortGroup(port, macmode, mac, VirtualDeviceConfigSpecOperation.add);
		} else {
			List<NetworkSummary> networks = VsphereQuery.getNetworksForConfigTarget(configTarget, network);
			if (networks.size() < 1) {
				Formatter.printErrorLine("Could not find network");
				System.exit(-1);
			}
			NetworkSummary nw = networks.get(0);
			nicSpec = VsphereFactory.getVirtualNicForNetwork(nw, macmode, mac, VirtualDeviceConfigSpecOperation.add);
		}

		Datastore store = null;
		if (suggestDatastore) {
			ClusterComputeResource ccr = new ClusterComputeResource(VsphereManager.getServerConnection(), computeResMor);
			store = VsphereQuery.recommendDatastore(ccr, datastoreName);
		} else {
			List<DatastoreSummary> datastores = VsphereQuery.getDatastoresForConfigTarget(configTarget, datastoreName);

			if (datastores.size() < 1) {
				Formatter.printErrorLine("Failed to retrieve DataStores");
				System.exit(-1);
			}
			store = new Datastore(VsphereManager.getServerConnection(), datastores.get(0).getDatastore());
		}

		ManagedObjectReference datastoreRef = store.getMOR();
		String datastoreVolume = VsphereQuery.getVolumeName(store.getName());

		VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
		vmfi.setVmPathName(String.format("%s", datastoreVolume));

		VirtualDeviceConfigSpec scsiCtrlSpec = VsphereFactory.getScsiController(0, VirtualSCSISharing.noSharing, VirtualDeviceConfigSpecOperation.add);

		if (diskSizeMB < 10 * 1024) {
			diskSizeMB = 10 * 1024;
		}

		VirtualDeviceConfigSpec disk1Spec = null;
		VirtualDeviceConfigSpec disk2Spec = null;

		if (diskSizeMB > diskSplit) {
			disk1Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 0, datastoreRef, diskSplit);
			disk2Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 1, datastoreRef, diskSizeMB - diskSplit);
		} else {
			disk1Spec = VsphereFactory.createVirtualDisk(scsiCtrlSpec.getDevice().getKey(), 0, datastoreRef, diskSizeMB);
		}

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
	protected static VirtualDeviceConfigSpec createVirtualDisk(int controllerKey, int unit, ManagedObjectReference datastoreRef, int diskSizeMB) {
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

	protected static VirtualDeviceConfigSpec getFloppyDiskDrive(VirtualMachine vm, String datastore, String dc, String filename) {
		VirtualFloppy floppy = new VirtualFloppy();
		floppy.setKey(VsphereFactory.getKey());

		int devs = 0;
		for (VirtualDevice device : vm.getConfig().getHardware().getDevice()) {
			if ((device instanceof VirtualFloppy)) {
				devs += device.getUnitNumber() + 1;
			}
		}

		switch (devs) {
		case 0:
		case 2:
			floppy.setUnitNumber(0);
			break;
		case 1:
			floppy.setUnitNumber(1);
			break;
		default:
			Formatter.printErrorLine("All floppy slots are used.");
			System.exit(-1);
			break;
		}

		try {
			VirtualFloppyImageBackingInfo flpBacking = new VirtualFloppyImageBackingInfo();

			Datastore ds = new Datastore(VsphereManager.getServerConnection(), VsphereQuery.getDatastoreReference(datastore, dc));
			flpBacking.setDatastore(ds.getMOR());
			flpBacking.setFileName(String.format("%s %s", VsphereQuery.getVolumeName(ds.getName()), filename));

			floppy.setBacking(flpBacking);
		} catch (Exception e) {
			Formatter.printStackTrace(e);
			Formatter.printErrorLine("Failed to find datastore: " + datastore);
			System.exit(-1);
		}

		VirtualDeviceConfigSpec floppySpec = new VirtualDeviceConfigSpec();
		floppySpec.setOperation(VirtualDeviceConfigSpecOperation.add);
		floppySpec.setDevice(floppy);

		return floppySpec;
	}

	protected static VirtualDeviceConfigSpec getCdromDrive(VirtualMachine vm, String datastore, String dc, String filename) {
		VirtualCdrom cdrom = new VirtualCdrom();
		cdrom.setKey(VsphereFactory.getKey());

		Map<Integer, Integer> devs = new HashMap<Integer, Integer>(4);
		for (VirtualDevice device : vm.getConfig().getHardware().getDevice()) {
			if (device instanceof VirtualIDEController) {
				if (!devs.containsKey(device.getKey())) {
					devs.put(device.getKey(), 0);
				}
			} else if (device instanceof VirtualCdrom) {
				VirtualCdrom cd = (VirtualCdrom) device;
				if (!devs.containsKey(cd.getControllerKey())) {
					devs.put(cd.getControllerKey(), cd.getUnitNumber() + 1);
				} else {
					devs.put(cd.getControllerKey(), devs.get(cd.getControllerKey()) + (cd.getUnitNumber() + 1));
				}
			}
		}

		for (java.util.Map.Entry<Integer, Integer> dev : devs.entrySet()) {
			if (dev.getValue() == 0 || dev.getValue() == 2) {
				cdrom.setControllerKey(dev.getKey());
				cdrom.setUnitNumber(0);
			} else if (dev.getValue() == 1) {
				cdrom.setControllerKey(dev.getKey());
				cdrom.setUnitNumber(1);
			}
		}

		if (cdrom.getUnitNumber() == null || cdrom.getControllerKey() == null) {
			Formatter.printErrorLine("Could not find IDE controller, or all IDE controller slots are used.");
			System.exit(-1);
		}

		try {
			VirtualCdromIsoBackingInfo isoBacking = new VirtualCdromIsoBackingInfo();

			Datastore ds = new Datastore(VsphereManager.getServerConnection(), VsphereQuery.getDatastoreReference(datastore, dc));
			isoBacking.setDatastore(ds.getMOR());
			isoBacking.setFileName(String.format("%s %s", VsphereQuery.getVolumeName(ds.getName()), filename));

			cdrom.setBacking(isoBacking);
		} catch (Exception e) {
			Formatter.printStackTrace(e);
			Formatter.printErrorLine("Failed to find datastore: " + datastore);
			System.exit(-1);
		}

		VirtualDeviceConfigSpec cdromSpec = new VirtualDeviceConfigSpec();
		cdromSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
		cdromSpec.setDevice(cdrom);

		return cdromSpec;
	}

	protected static DistributedVirtualSwitchPortConnection getPortForNetworkAndSwitch(String networkName, String switchUuid) throws RuntimeFault, RemoteException {
		ManagedObjectReference netw = VsphereQuery.getDistributedVirtualPortGroupForNetwork(networkName);
		DistributedVirtualSwitchPortConnection port = new DistributedVirtualSwitchPortConnection();
		port.setPortgroupKey(netw.getVal());
		port.setSwitchUuid(switchUuid);

		return port;
	}

	private static VirtualDeviceConfigSpec getVirtualNicForBacking(VirtualDeviceBackingInfo backing, VirtualEthernetCardMacType mac, String customMac, VirtualDeviceConfigSpecOperation action) {
		VirtualEthernetCard nic = new VirtualVmxnet3();
		nic.setBacking(backing);
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

	protected static VirtualDeviceConfigSpec getVirtualNicForNetwork(NetworkSummary network, VirtualEthernetCardMacType mac, String customMac, VirtualDeviceConfigSpecOperation action) {
		VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
		nicBacking.setNetwork(network.getNetwork());
		nicBacking.setDeviceName(network.getName());

		return VsphereFactory.getVirtualNicForBacking(nicBacking, mac, customMac, action);
	}

	protected static VirtualDeviceConfigSpec getVirtualNicForPortGroup(DistributedVirtualSwitchPortConnection port, VirtualEthernetCardMacType mac, String customMac, VirtualDeviceConfigSpecOperation action) {
		VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
		nicBacking.setPort(port);

		return VsphereFactory.getVirtualNicForBacking(nicBacking, mac, customMac, action);
	}
}
