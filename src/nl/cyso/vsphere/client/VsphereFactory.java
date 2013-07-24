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

import java.util.Random;

import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ParaVirtualSCSIController;
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
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VirtualVmxnet3;

public class VsphereFactory {
	private static int key = new Random().nextInt(10) + 10;

	private static int getKey() {
		return key++;
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
		diskfileBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());
		diskfileBacking.setThinProvisioned(false);
		diskfileBacking.setEagerlyScrub(false);

		VirtualDisk disk = new VirtualDisk();
		disk.setKey(new Integer(0));
		disk.setControllerKey(controllerKey);
		disk.setCapacityInKB(1024 * diskSizeMB);
		disk.setBacking(diskfileBacking);
		disk.setKey(VsphereFactory.getKey());
		disk.setUnitNumber(unit);

		diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
		diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
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

	protected static VirtualDeviceConfigSpec getVirtualNicForPortGroup(DistributedVirtualSwitchPortConnection port, VirtualEthernetCardMacType mac, String customMac, VirtualDeviceConfigSpecOperation action) {
		VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
		nicBacking.setPort(port);

		VirtualEthernetCard nic = new VirtualVmxnet3();
		nic.setBacking(nicBacking);
		nic.setKey(VsphereFactory.getKey());
		nic.setAddressType(mac.value());
		if (mac == VirtualEthernetCardMacType.MANUAL) {
			nic.setMacAddress(customMac);
		}

		VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
		nicSpec.setOperation(action);
		nicSpec.setDevice(nic);

		return nicSpec;
	}
}
