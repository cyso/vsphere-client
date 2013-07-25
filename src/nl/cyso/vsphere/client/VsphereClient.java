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
		ManagedObjectReference vmFolderMor = VsphereQuery.getVMRootFolder(dcmor);

		VirtualMachineConfigSpec vmConfigSpec = VsphereFactory.createVmConfigSpec(Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), Configuration.getString("mac"), Configuration.getString("network"), crmor, hostmor);
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation(Configuration.getString("description"));
		vmConfigSpec.setMemoryMB(Long.valueOf(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.valueOf(Configuration.getString("cpu")));
		vmConfigSpec.setNumCoresPerSocket(1);
		vmConfigSpec.setGuestId(VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST.value());

		ManagedObjectReference taskmor = VsphereManager.getVimPort().createVMTask(vmFolderMor, vmConfigSpec, resourcepoolmor, hostmor);
		if (VsphereQuery.getTaskResultAfterDone(taskmor)) {
			Formatter.printInfoLine(String.format("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn")));
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
			Formatter.printInfoLine("Success: VM powered on successfully");
		} else {
			String msg = "Failure: starting [ " + vmMor.getValue() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void powerOffVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		ManagedObjectReference cssTask = VsphereManager.getVimPort().powerOffVMTask(vmMor);
		if (VsphereQuery.getTaskResultAfterDone(cssTask)) {
			Formatter.printInfoLine("Success: VM powered off successfully");
		} else {
			String msg = "Failure: starting [ " + vmMor.getValue() + "] VM";
			throw new RuntimeException(msg);
		}
	}

	public static void shutdownVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		VsphereManager.getVimPort().shutdownGuest(vmMor);
		Formatter.printInfoLine("Success: VM shutdown requested");
	}
}
