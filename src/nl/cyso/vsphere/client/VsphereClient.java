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

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VsphereClient {
	public static void createVirtualMachine() throws RemoteException, Exception {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(Configuration.getString("dc"));
		if (dcmor == null) {
			Formatter.printErrorLine("Datacenter " + Configuration.get("dc") + " not found.");
			return;
		}

		ManagedObjectReference hostmor = VsphereQuery.getHostNodeReference(Configuration.getString("esxnode"), dcmor);
		if (hostmor == null) {
			Formatter.printErrorLine("Host " + Configuration.get("esxnode") + " not found");
			return;
		}

		ManagedObjectReference crmor = VsphereQuery.getReferenceParent(hostmor);
		if (crmor == null) {
			Formatter.printErrorLine("No Compute Resource Found On Specified Host");
			return;
		}

		ManagedObjectReference resourcepoolmor = VsphereQuery.getResourcePoolReference(hostmor);
		Folder vmFolder = new Datacenter(VsphereManager.getServerConnection(), dcmor).getVmFolder();

		VirtualMachineConfigSpec vmConfigSpec = VsphereFactory.createVmConfigSpec(Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), Configuration.getString("mac"), Configuration.getString("network"), crmor, hostmor);
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation(Configuration.getString("description"));
		vmConfigSpec.setMemoryMB(Long.valueOf(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.valueOf(Configuration.getString("cpu")));
		vmConfigSpec.setNumCoresPerSocket(1);
		vmConfigSpec.setGuestId(VirtualMachineGuestOsIdentifier.ubuntu64Guest.toString());

		Task task = vmFolder.createVM_Task(vmConfigSpec, new ResourcePool(VsphereManager.getServerConnection(), resourcepoolmor), new HostSystem(VsphereManager.getServerConnection(), hostmor));
		if (task.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine(String.format("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn")));
		} else {
			String msg = "Failure: Creating [ " + Configuration.get("fqdn") + "] VM";
			throw new RuntimeException(msg);
		}
		ManagedObjectReference vmMor = (ManagedObjectReference) VsphereQuery.getEntityProps(task.getMOR(), new String[] { "info.result" }).get("info.result");

		// Start the Newly Created VM.
		System.out.println("Powering on the newly created VM " + Configuration.get("fqdn"));
		VsphereClient.powerOnVM(vmMor);
	}

	public static void powerOnVM(ManagedObjectReference vmmor) throws RemoteException, Exception {
		VsphereClient.powerOnVM(new VirtualMachine(VsphereManager.getServerConnection(), vmmor));
	}

	public static void powerOnVM(VirtualMachine vm) throws RemoteException, Exception {
		Task powerOnTask = vm.powerOnVM_Task(null);
		if (powerOnTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM powered on successfully");
		} else {
			String msg = "Failure: starting [ " + vm.getName() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void powerOffVM(ManagedObjectReference vmmor) throws RemoteException, Exception {
		VsphereClient.powerOffVM(new VirtualMachine(VsphereManager.getServerConnection(), vmmor));
	}

	public static void powerOffVM(VirtualMachine vm) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState != VirtualMachinePowerState.poweredOn) {
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

	public static void shutdownVM(ManagedObjectReference vmmor) throws RemoteException, Exception {
		VsphereClient.shutdownVM(new VirtualMachine(VsphereManager.getServerConnection(), vmmor));
	}

	public static void shutdownVM(VirtualMachine vm) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState == VirtualMachinePowerState.poweredOn) {
			vm.shutdownGuest();
			Formatter.printInfoLine("Success: VM shutdown message sent successfully");
		}
	}

	public static void deleteVirtualMachine(ManagedObjectReference vmmor) throws RemoteException, Exception {
		VsphereClient.deleteVirtualMachine(new VirtualMachine(VsphereManager.getServerConnection(), vmmor));
	}

	public static void deleteVirtualMachine(VirtualMachine vm) throws RemoteException, Exception {
		VirtualMachinePowerState powerState = vm.getRuntime().getPowerState();
		if (powerState == VirtualMachinePowerState.poweredOn) {
			VsphereClient.powerOffVM(vm);
		}

		Task destroyTask = vm.destroy_Task();
		if (destroyTask.waitForTask() == Task.SUCCESS) {
			Formatter.printInfoLine("Success: VM destroyed successfully");
		} else {
			String msg = "Failure: destroying [ " + vm.getName() + "] VM";
			throw new RuntimeException(msg);
		}
	}
}
