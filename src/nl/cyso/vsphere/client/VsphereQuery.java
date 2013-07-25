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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineDatastoreInfo;
import com.vmware.vim25.VirtualMachineNetworkInfo;

public class VsphereQuery {
	/**
	 * Returns all the MOREFs of the specified type that are present under the container
	 * 
	 * @param folder {@link ManagedObjectReference} of the container to begin the search from
	 * @param morefType Type of the managed entity that needs to be searched
	 * @return Map of name and MOREF of the managed objects present. If none exist then empty Map is returned
	 * @throws InvalidPropertyFaultMsg
	 * @throws RuntimeFaultFaultMsg
	 */
	protected static Map<String, ManagedObjectReference> getMOREFsInContainerByType(ManagedObjectReference folder, String morefType) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		String PROP_ME_NAME = "name";
		ManagedObjectReference viewManager = VsphereManager.getServiceContent().getViewManager();
		ManagedObjectReference containerView = VsphereManager.getVimPort().createContainerView(viewManager, folder, Arrays.asList(morefType), true);

		Map<String, ManagedObjectReference> tgtMoref = new HashMap<String, ManagedObjectReference>();

		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.setType(morefType);
		propertySpec.getPathSet().add(PROP_ME_NAME);

		TraversalSpec ts = new TraversalSpec();
		ts.setName("view");
		ts.setPath("view");
		ts.setSkip(false);
		ts.setType("ContainerView");

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(containerView);
		objectSpec.setSkip(Boolean.TRUE);
		objectSpec.getSelectSet().add(ts);

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);

		List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<PropertyFilterSpec>();
		propertyFilterSpecs.add(propertyFilterSpec);

		RetrieveResult rslts = VsphereManager.getVimPort().retrievePropertiesEx(VsphereManager.getServiceContent().getPropertyCollector(), propertyFilterSpecs, new RetrieveOptions());
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
		if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
			listobjcontent.addAll(rslts.getObjects());
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = VsphereManager.getVimPort().continueRetrievePropertiesEx(VsphereManager.getServiceContent().getPropertyCollector(), token);
			token = null;
			if (rslts != null) {
				token = rslts.getToken();
				if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
					listobjcontent.addAll(rslts.getObjects());
				}
			}
		}
		for (ObjectContent oc : listobjcontent) {
			ManagedObjectReference mr = oc.getObj();
			String entityNm = null;
			List<DynamicProperty> dps = oc.getPropSet();
			if (dps != null) {
				for (DynamicProperty dp : dps) {
					entityNm = (String) dp.getVal();
				}
			}
			tgtMoref.put(entityNm, mr);
		}
		return tgtMoref;
	}

	/**
	 * Method to retrieve properties of a {@link ManagedObjectReference}
	 * 
	 * @param entityMor {@link ManagedObjectReference} of the entity
	 * @param props Array of properties to be looked up
	 * @return Map of the property name and its corresponding value
	 * @throws InvalidPropertyFaultMsg If a property does not exist
	 * @throws RuntimeFaultFaultMsg
	 */
	protected static Map<String, Object> getEntityProps(ManagedObjectReference entityMor, String[] props) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		HashMap<String, Object> retVal = new HashMap<String, Object>();

		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.setType(entityMor.getType());
		propertySpec.getPathSet().addAll(Arrays.asList(props));

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(entityMor);

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);

		List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<PropertyFilterSpec>();
		propertyFilterSpecs.add(propertyFilterSpec);

		RetrieveResult rslts = VsphereManager.getVimPort().retrievePropertiesEx(VsphereManager.getServiceContent().getPropertyCollector(), propertyFilterSpecs, new RetrieveOptions());
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
		if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
			listobjcontent.addAll(rslts.getObjects());
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = VsphereManager.getVimPort().continueRetrievePropertiesEx(VsphereManager.getServiceContent().getPropertyCollector(), token);
			token = null;
			if (rslts != null) {
				token = rslts.getToken();
				if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
					listobjcontent.addAll(rslts.getObjects());
				}
			}
		}
		for (ObjectContent oc : listobjcontent) {
			List<DynamicProperty> dps = oc.getPropSet();
			if (dps != null) {
				for (DynamicProperty dp : dps) {
					retVal.put(dp.getName(), dp.getVal());
				}
			}
		}
		return retVal;
	}

	/**
	 * This method returns the ConfigTarget for a HostSystem.
	 * 
	 * @param computeResMor A MoRef to the ComputeResource used by the HostSystem
	 * @param hostMor A MoRef to the HostSystem
	 * @return Instance of ConfigTarget for the supplied HostSystem/ComputeResource
	 * @throws Exception When no ConfigTarget can be found
	 */
	protected static ConfigTarget getConfigTargetForHost(ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws Exception {
		ManagedObjectReference envBrowseMor = (ManagedObjectReference) VsphereQuery.getEntityProps(computeResMor, new String[] { "environmentBrowser" }).get("environmentBrowser");
		ConfigTarget configTarget = VsphereManager.getVimPort().queryConfigTarget(envBrowseMor, hostMor);
		if (configTarget == null) {
			throw new RuntimeException("No ConfigTarget found in ComputeResource");
		}
		return configTarget;
	}

	/**
	 * The method returns the default devices from the HostSystem.
	 * 
	 * @param computeResMor A MoRef to the ComputeResource used by the HostSystem
	 * @param hostMor A MoRef to the HostSystem
	 * @return Array of VirtualDevice containing the default devices for the HostSystem
	 * @throws Exception
	 */
	protected static List<VirtualDevice> getDefaultDevices(ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws Exception {
		ManagedObjectReference envBrowseMor = (ManagedObjectReference) VsphereQuery.getEntityProps(computeResMor, new String[] { "environmentBrowser" }).get("environmentBrowser");
		VirtualMachineConfigOption cfgOpt = VsphereManager.getVimPort().queryConfigOption(envBrowseMor, null, hostMor);
		List<VirtualDevice> defaultDevs = null;
		if (cfgOpt == null) {
			throw new RuntimeException("No VirtualHardwareInfo found in ComputeResource");
		} else {
			List<VirtualDevice> lvds = cfgOpt.getDefaultDevice();
			if (lvds == null) {
				throw new RuntimeException("No Datastore found in ComputeResource");
			} else {
				defaultDevs = lvds;
			}
		}
		return defaultDevs;
	}

	protected static String getVolumeName(String volName) {
		String volumeName = null;
		if (volName != null && volName.length() > 0) {
			volumeName = "[" + volName + "]";
		} else {
			volumeName = "[Local]";
		}

		return volumeName;
	}

	/**
	 * Handle Updates for a single object. waits till expected values of properties to check are reached. Destroys the ObjectFilter when done.
	 * 
	 * @param objmor MOR of the Object to wait for
	 * @param filterProps Properties list to filter
	 * @param endWaitProps Properties list to check for expected values these be properties of a property in the filter properties list
	 * @param expectedVals values for properties to end the wait
	 * @return true indicating expected values were met, and false otherwise
	 * @throws RuntimeFaultFaultMsg
	 * @throws InvalidPropertyFaultMsg
	 * @throws InvalidCollectorVersionFaultMsg
	 */
	protected static Object[] waitForValues(ManagedObjectReference objmor, String[] filterProps, String[] endWaitProps, Object[][] expectedVals) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
		// version string is initially null
		String version = "";
		Object[] endVals = new Object[endWaitProps.length];
		Object[] filterVals = new Object[filterProps.length];

		PropertyFilterSpec spec = new PropertyFilterSpec();
		ObjectSpec oSpec = new ObjectSpec();
		oSpec.setObj(objmor);
		oSpec.setSkip(Boolean.FALSE);
		spec.getObjectSet().add(oSpec);

		PropertySpec pSpec = new PropertySpec();
		pSpec.getPathSet().addAll(Arrays.asList(filterProps));
		pSpec.setType(objmor.getType());
		spec.getPropSet().add(pSpec);

		ManagedObjectReference filterSpecRef = VsphereManager.getVimPort().createFilter(VsphereManager.getServiceContent().getPropertyCollector(), spec, true);

		boolean reached = false;

		UpdateSet updateset = null;
		List<PropertyFilterUpdate> filtupary = null;
		List<ObjectUpdate> objupary = null;
		List<PropertyChange> propchgary = null;
		while (!reached) {
			updateset = VsphereManager.getVimPort().waitForUpdates(VsphereManager.getServiceContent().getPropertyCollector(), version);
			if (updateset == null || updateset.getFilterSet() == null) {
				continue;
			}
			version = updateset.getVersion();

			// Make this code more general purpose when PropCol changes later.
			filtupary = updateset.getFilterSet();

			for (PropertyFilterUpdate filtup : filtupary) {
				objupary = filtup.getObjectSet();
				for (ObjectUpdate objup : objupary) {
					if (objup.getKind() == ObjectUpdateKind.MODIFY || objup.getKind() == ObjectUpdateKind.ENTER || objup.getKind() == ObjectUpdateKind.LEAVE) {
						propchgary = objup.getChangeSet();
						for (PropertyChange propchg : propchgary) {
							updateValues(endWaitProps, endVals, propchg);
							updateValues(filterProps, filterVals, propchg);
						}
					}
				}
			}

			Object expctdval = null;
			// Check if the expected values have been reached and exit the loop
			// if done.
			// Also exit the WaitForUpdates loop if this is the case.
			for (int chgi = 0; chgi < endVals.length && !reached; chgi++) {
				for (int vali = 0; vali < expectedVals[chgi].length && !reached; vali++) {
					expctdval = expectedVals[chgi][vali];

					reached = expctdval.equals(endVals[chgi]) || reached;
				}
			}
		}

		// Destroy the filter when we are done.
		VsphereManager.getVimPort().destroyPropertyFilter(filterSpecRef);
		return filterVals;
	}

	protected static void updateValues(String[] props, Object[] vals, PropertyChange propchg) {
		for (int findi = 0; findi < props.length; findi++) {
			if (propchg.getName().lastIndexOf(props[findi]) >= 0) {
				if (propchg.getOp() == PropertyChangeOp.REMOVE) {
					vals[findi] = "";
				} else {
					vals[findi] = propchg.getVal();
				}
			}
		}
	}

	/**
	 * This method returns a boolean value specifying whether the Task is succeeded or failed.
	 * 
	 * @param task ManagedObjectReference representing the Task.
	 * @return boolean value representing the Task result.
	 * @throws InvalidCollectorVersionFaultMsg
	 * @throws RuntimeFaultFaultMsg
	 * @throws InvalidPropertyFaultMsg
	 */
	protected static boolean getTaskResultAfterDone(ManagedObjectReference task) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
		boolean retVal = false;

		// info has a property - state for state of the task
		Object[] result = VsphereQuery.waitForValues(task, new String[] { "info.state", "info.error" }, new String[] { "state" }, new Object[][] { new Object[] { TaskInfoState.SUCCESS, TaskInfoState.ERROR } });

		if (result[0].equals(TaskInfoState.SUCCESS)) {
			retVal = true;
		}
		if (result[1] instanceof LocalizedMethodFault) {
			throw new RuntimeException(((LocalizedMethodFault) result[1]).getLocalizedMessage());
		}
		return retVal;
	}

	protected static List<NetworkSummary> getNetworksForConfigTarget(ConfigTarget config, String keyFilter) {
		List<NetworkSummary> output = new ArrayList<NetworkSummary>();
		for (VirtualMachineNetworkInfo netInfo : config.getNetwork()) {
			NetworkSummary netSummary = netInfo.getNetwork();
			System.out.println("Network: " + netSummary.getName());
			if (netSummary.isAccessible()) {
				if (keyFilter != null && !netSummary.getName().contains(keyFilter)) {
					continue;
				}
				output.add(netSummary);
			}
		}

		return output;
	}

	protected static List<DistributedVirtualPortgroupInfo> getVirtualPortgroupsForConfigTarget(ConfigTarget config, String keyFilter) {
		List<DistributedVirtualPortgroupInfo> output = new ArrayList<DistributedVirtualPortgroupInfo>();
		for (DistributedVirtualPortgroupInfo portGroup : config.getDistributedVirtualPortgroup()) {
			String pgName = portGroup.getPortgroupName();
			System.out.println("PortGroup: " + pgName);
			if (keyFilter != null && !pgName.contains(keyFilter)) {
				continue;
			} else {
				output.add(portGroup);
			}
		}

		return output;
	}

	protected static List<DistributedVirtualSwitchInfo> getVirtualSwitchesForConfigTarget(ConfigTarget config, String keyFilter) {
		List<DistributedVirtualSwitchInfo> output = new ArrayList<DistributedVirtualSwitchInfo>();
		for (DistributedVirtualSwitchInfo sw : config.getDistributedVirtualSwitch()) {
			String swName = sw.getSwitchName();
			System.out.println("Switch: " + swName);
			if (keyFilter != null && !swName.contains(keyFilter)) {
				continue;
			} else {
				output.add(sw);
			}
		}

		return output;
	}

	protected static List<DatastoreSummary> getDatastoresForConfigTarget(ConfigTarget config, String keyFilter) {
		List<DatastoreSummary> output = new ArrayList<DatastoreSummary>();
		for (VirtualMachineDatastoreInfo ds : config.getDatastore()) {
			DatastoreSummary dsSummary = ds.getDatastore();
			String dsName = dsSummary.getName();
			System.out.println("DataStore: " + dsName);
			if (keyFilter != null && !dsName.contains(keyFilter)) {
				continue;
			} else {
				output.add(dsSummary);
			}
		}

		return output;
	}

	protected static ManagedObjectReference getDatacenterReference(String name) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		return VsphereQuery.getMOREFsInContainerByType(VsphereManager.getServiceContent().getRootFolder(), "Datacenter").get(name);
	}

	protected static ManagedObjectReference getHostNodeReference(String name, String dc) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(dc);
		return VsphereQuery.getHostNodeReference(name, dcmor);
	}

	protected static ManagedObjectReference getHostNodeReference(String name, ManagedObjectReference dc) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		return VsphereQuery.getMOREFsInContainerByType(dc, "HostSystem").get(name);
	}

	protected static ManagedObjectReference getReferenceParent(ManagedObjectReference object) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		return (ManagedObjectReference) VsphereQuery.getEntityProps(object, new String[] { "parent" }).get("parent");
	}

	protected static ManagedObjectReference getResourcePoolReference(String esxnode, String dc) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(dc);
		return VsphereQuery.getResourcePoolReference(esxnode, dcmor);
	}

	protected static ManagedObjectReference getResourcePoolReference(String esxnode, ManagedObjectReference dc) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ManagedObjectReference hostmor = VsphereQuery.getHostNodeReference(esxnode, dc);
		return VsphereQuery.getResourcePoolReference(hostmor);
	}

	protected static ManagedObjectReference getResourcePoolReference(ManagedObjectReference esxnode) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ManagedObjectReference crmor = VsphereQuery.getReferenceParent(esxnode);
		return (ManagedObjectReference) VsphereQuery.getEntityProps(crmor, new String[] { "resourcePool" }).get("resourcePool");
	}

	protected static ManagedObjectReference getVMRootFolder(String dc) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(dc);
		return VsphereQuery.getVMRootFolder(dcmor);
	}

	protected static ManagedObjectReference getVMRootFolder(ManagedObjectReference dc) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		return (ManagedObjectReference) VsphereQuery.getEntityProps(dc, new String[] { "vmFolder" }).get("vmFolder");
	}

	protected static Map<String, ManagedObjectReference> findVirtualMachines(List<String> machines, ManagedObjectReference rootFolder) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		return VsphereQuery.findVirtualMachines(machines, rootFolder, 0);
	}

	private static Map<String, ManagedObjectReference> findVirtualMachines(List<String> machines, ManagedObjectReference rootFolder, int depth) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		Map<String, Object> objects = VsphereQuery.getEntityProps(rootFolder, new String[] { "name", "childEntity" });
		Map<String, ManagedObjectReference> out = new HashMap<String, ManagedObjectReference>();

		if (objects.containsKey("childEntity") && objects.get("childEntity") != null) {
			for (ManagedObjectReference ref : ((ArrayOfManagedObjectReference) objects.get("childEntity")).getManagedObjectReference()) {
				String name = (String) VsphereQuery.getEntityProps(ref, new String[] { "name" }).get("name");
				if (ref.getType().equals("Folder")) {
					System.out.println(StringUtils.repeat("\t", depth) + name);
					out.putAll(VsphereQuery.findVirtualMachines(machines, ref, depth + 1));
				} else if (ref.getType().equals("VirtualMachine")) {
					if (machines != null) {
						boolean flag = false;
						for (String machine : machines) {
							if (name.contains(machine)) {
								flag = true;
								break;
							}
						}
						if (!flag) {
							continue;
						}
					}
					System.out.println(StringUtils.repeat("\t", depth) + "- " + name);
					out.put(name, ref);
				}
			}
		}

		return out;
	}
}
