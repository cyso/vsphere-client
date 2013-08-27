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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.cyso.vsphere.client.constants.VMFolderObjectType;

import org.apache.commons.lang.ArrayUtils;

import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineDatastoreInfo;
import com.vmware.vim25.VirtualMachineNetworkInfo;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.ContainerView;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.DistributedVirtualPortgroup;
import com.vmware.vim25.mo.EnvironmentBrowser;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.PropertyCollector;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.ViewManager;
import com.vmware.vim25.mo.VirtualMachine;

public class VsphereQuery {
	/**
	 * Returns all the MOREFs of the specified type that are present under the container
	 * 
	 * @param folder {@link ManagedObjectReference} of the container to begin the search from
	 * @param morefType Type of the managed entity that needs to be searched
	 * @return Map of name and MOREF of the managed objects present. If none exist then empty Map is returned
	 * @throws RemoteException
	 * @throws RuntimeFault
	 */
	private static Map<String, ManagedObjectReference> getMOREFsInContainerByType(ManagedObjectReference folder, String morefType) throws RuntimeFault, RemoteException {
		String PROP_ME_NAME = "name";
		// ManagedObjectReference viewManager = VsphereManager.getServiceContent().getViewManager();
		ViewManager viewManager = VsphereManager.getServiceInstance().getViewManager();
		ContainerView containerView = viewManager.createContainerView(new Folder(VsphereManager.getServiceInstance().getServerConnection(), folder), new String[] { morefType }, true);

		Map<String, ManagedObjectReference> tgtMoref = new HashMap<String, ManagedObjectReference>();

		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.setType(morefType);
		propertySpec.setPathSet((String[]) ArrayUtils.add(propertySpec.getPathSet(), PROP_ME_NAME));

		TraversalSpec ts = new TraversalSpec();
		ts.setName("view");
		ts.setPath("view");
		ts.setSkip(false);
		ts.setType("ContainerView");

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(containerView.getMOR());
		objectSpec.setSkip(Boolean.TRUE);
		objectSpec.setSelectSet((SelectionSpec[]) ArrayUtils.add(objectSpec.getSelectSet(), ts));

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.setPropSet((PropertySpec[]) ArrayUtils.add(propertyFilterSpec.getPropSet(), propertySpec));
		propertyFilterSpec.setObjectSet((ObjectSpec[]) ArrayUtils.add(propertyFilterSpec.getObjectSet(), objectSpec));

		PropertyFilterSpec[] propertyFilterSpecs = new PropertyFilterSpec[] { propertyFilterSpec };
		PropertyCollector propertyCollector = VsphereManager.getServiceInstance().getPropertyCollector();

		RetrieveResult rslts = propertyCollector.retrievePropertiesEx(propertyFilterSpecs, new RetrieveOptions());
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
		if (rslts != null && rslts.getObjects() != null && rslts.getObjects().length != 0) {
			listobjcontent.addAll(Arrays.asList(rslts.getObjects()));
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = propertyCollector.continueRetrievePropertiesEx(token);
			token = null;
			if (rslts != null) {
				token = rslts.getToken();
				if (rslts.getObjects() != null && rslts.getObjects().length != 0) {
					listobjcontent.addAll(Arrays.asList(rslts.getObjects()));
				}
			}
		}
		for (ObjectContent oc : listobjcontent) {
			ManagedObjectReference mr = oc.getObj();
			String entityNm = null;
			DynamicProperty[] dps = oc.getPropSet();
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
	 * @throws RemoteException
	 * @throws RuntimeFault
	 * @throws InvalidProperty
	 */
	private static Map<String, Object> getEntityProps(ManagedObjectReference entityMor, String[] props) throws InvalidProperty, RuntimeFault, RemoteException {
		HashMap<String, Object> retVal = new HashMap<String, Object>();

		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.setType(entityMor.getType());
		propertySpec.setPathSet(props);

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(entityMor);

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.setPropSet((PropertySpec[]) ArrayUtils.add(propertyFilterSpec.getPropSet(), propertySpec));
		propertyFilterSpec.setObjectSet((ObjectSpec[]) ArrayUtils.add(propertyFilterSpec.getObjectSet(), objectSpec));

		PropertyFilterSpec[] propertyFilterSpecs = new PropertyFilterSpec[] { propertyFilterSpec };
		PropertyCollector propertyCollector = VsphereManager.getServiceInstance().getPropertyCollector();

		RetrieveResult rslts = propertyCollector.retrievePropertiesEx(propertyFilterSpecs, new RetrieveOptions());
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
		if (rslts != null && rslts.getObjects() != null && rslts.getObjects().length != 0) {
			listobjcontent.addAll(Arrays.asList(rslts.getObjects()));
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = propertyCollector.continueRetrievePropertiesEx(token);
			token = null;
			if (rslts != null) {
				token = rslts.getToken();
				if (rslts.getObjects() != null && rslts.getObjects().length != 0) {
					listobjcontent.addAll(Arrays.asList(rslts.getObjects()));
				}
			}
		}
		for (ObjectContent oc : listobjcontent) {
			DynamicProperty[] dps = oc.getPropSet();
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
		EnvironmentBrowser envBrowser = new ComputeResource(VsphereManager.getServerConnection(), computeResMor).getEnvironmentBrowser();
		HostSystem host = new HostSystem(VsphereManager.getServerConnection(), hostMor);
		ConfigTarget configTarget = envBrowser.queryConfigTarget(host);
		if (configTarget == null) {
			throw new RuntimeException("No ConfigTarget found in ComputeResource");
		}
		return configTarget;
	}

	/**
	 * This method returns the ConfigTarget for a VirtualMachine.
	 * 
	 * @param vmMor A MoRef to the VirtualMachine
	 * @param hostMor A MoRef to the HostSystem
	 * @return Instance of ConfigTarget for the supplied VirtualMachine
	 * @throws Exception When no ConfigTarget can be found
	 */
	protected static ConfigTarget getConfigTargetForVirtualMachine(ManagedObjectReference vmMor) throws Exception {
		EnvironmentBrowser envBrowser = new VirtualMachine(VsphereManager.getServerConnection(), vmMor).getEnvironmentBrowser();
		if (envBrowser == null) {
			return null;
		}
		ConfigTarget configTarget = envBrowser.queryConfigTarget(null);
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
		EnvironmentBrowser envBrowser = new EnvironmentBrowser(VsphereManager.getServiceInstance().getServerConnection(), computeResMor);
		VirtualMachineConfigOption cfgOpt = envBrowser.queryConfigOption(null, new HostSystem(VsphereManager.getServerConnection(), hostMor));
		List<VirtualDevice> defaultDevs = null;
		if (cfgOpt == null) {
			throw new RuntimeException("No VirtualHardwareInfo found in ComputeResource");
		} else {
			VirtualDevice[] lvds = cfgOpt.getDefaultDevice();
			if (lvds == null) {
				throw new RuntimeException("No Datastore found in ComputeResource");
			} else {
				defaultDevs = Arrays.asList(lvds);
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

	protected static ManagedObjectReference getDatacenterReference(String name) throws RuntimeFault, RemoteException {
		return VsphereQuery.getMOREFsInContainerByType(VsphereManager.getServiceInstance().getRootFolder().getMOR(), "Datacenter").get(name);
	}

	protected static ManagedObjectReference getHostNodeReference(String name, String dc) throws RuntimeFault, RemoteException {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(dc);
		return VsphereQuery.getHostNodeReference(name, dcmor);
	}

	protected static ManagedObjectReference getHostNodeReference(String name, ManagedObjectReference dc) throws RuntimeFault, RemoteException {
		return VsphereQuery.getMOREFsInContainerByType(dc, "HostSystem").get(name);
	}

	protected static ManagedObjectReference getReferenceParent(ManagedObjectReference object) throws InvalidProperty, RuntimeFault, RemoteException {
		return (ManagedObjectReference) VsphereQuery.getEntityProps(object, new String[] { "parent" }).get("parent");
	}

	protected static ManagedObjectReference getResourcePoolReference(String esxnode, String dc) throws RuntimeFault, RemoteException {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(dc);
		return VsphereQuery.getResourcePoolReference(esxnode, dcmor);
	}

	protected static ManagedObjectReference getResourcePoolReference(String esxnode, ManagedObjectReference dc) throws InvalidProperty, RuntimeFault, RemoteException {
		ManagedObjectReference hostmor = VsphereQuery.getHostNodeReference(esxnode, dc);
		return VsphereQuery.getResourcePoolReference(hostmor);
	}

	protected static ManagedObjectReference getResourcePoolReference(ManagedObjectReference esxnode) throws InvalidProperty, RuntimeFault, RemoteException {
		ManagedObjectReference crmor = VsphereQuery.getReferenceParent(esxnode);
		return (ManagedObjectReference) VsphereQuery.getEntityProps(crmor, new String[] { "resourcePool" }).get("resourcePool");
	}

	protected static ManagedObjectReference getVMRootFolder(String dc) throws RuntimeFault, RemoteException {
		ManagedObjectReference dcmor = VsphereQuery.getDatacenterReference(dc);
		return VsphereQuery.getVMRootFolder(dcmor);
	}

	protected static ManagedObjectReference getVMRootFolder(ManagedObjectReference dc) throws InvalidProperty, RuntimeFault, RemoteException {
		return (ManagedObjectReference) VsphereQuery.getEntityProps(dc, new String[] { "vmFolder" }).get("vmFolder");
	}

	protected static ManagedObjectReference findVirtualMachine(String name, ManagedObjectReference rootFolder) throws InvalidProperty, RuntimeFault, RemoteException {
		Map<String, ManagedObjectReference> result = VsphereQuery.findVirtualMachines(Arrays.asList(name), rootFolder);

		if (result == null || result.isEmpty()) {
			return null;
		} else {
			return result.get("/" + name);
		}
	}

	protected static Map<String, ManagedObjectReference> findVirtualMachines(List<String> machines, ManagedObjectReference rootFolder) throws InvalidProperty, RuntimeFault, RemoteException {
		return VsphereQuery.findVMFolderObjects(machines, rootFolder, -1, 0, VMFolderObjectType.VirtualMachine);
	}

	protected static Map<String, ManagedObjectReference> findVirtualMachines(List<String> machines, ManagedObjectReference rootFolder, int maxDepth) throws InvalidProperty, RuntimeFault, RemoteException {
		return VsphereQuery.findVMFolderObjects(machines, rootFolder, maxDepth, 0, VMFolderObjectType.VirtualMachine);
	}

	private static Map<String, ManagedObjectReference> findVMFolderObjects(List<String> filters, ManagedObjectReference rootFolder, int maxDepth, int depth, VMFolderObjectType type) throws InvalidProperty, RuntimeFault, RemoteException {
		return VsphereQuery.findVMFolderObjects(filters, rootFolder, maxDepth, depth, type, null);
	}

	private static Map<String, ManagedObjectReference> findVMFolderObjects(List<String> filters, ManagedObjectReference rootFolder, int maxDepth, int depth, VMFolderObjectType type, String parentName) throws InvalidProperty, RuntimeFault, RemoteException {
		Map<String, ManagedObjectReference> out = new HashMap<String, ManagedObjectReference>();
		if (depth > maxDepth && maxDepth != -1) {
			return out;
		}

		if (rootFolder == null) {
			throw new NullPointerException("Passed Root Folder was null!");
		}

		Map<String, Object> objects = VsphereQuery.getEntityProps(rootFolder, new String[] { "name", "childEntity" });

		if (objects.containsKey("childEntity") && objects.get("childEntity") != null) {
			ArrayOfManagedObjectReference refs = (ArrayOfManagedObjectReference) objects.get("childEntity");
			if (refs.getManagedObjectReference() != null) {
				for (ManagedObjectReference ref : refs.getManagedObjectReference()) {
					String name = (String) VsphereQuery.getEntityProps(ref, new String[] { "name" }).get("name");
					if (ref.getType().equals("Folder")) {
						// System.out.println(StringUtils.repeat("\t", depth) + name);
						if (type == VMFolderObjectType.Folder && filters != null && !filters.isEmpty()) {
							boolean flag = false;
							for (String folder : filters) {
								if (name.equalsIgnoreCase(folder)) {
									flag = true;
									break;
								}
							}
							if (!flag) {
								continue;
							}
						}
						if (type == VMFolderObjectType.Folder) {
							out.put(String.format("%s/%s", (parentName == null) ? "" : parentName, name), ref);
						}
						out.putAll(VsphereQuery.findVMFolderObjects(filters, ref, maxDepth, depth + 1, type, String.format("%s/%s", (parentName == null) ? "" : parentName, name)));
					} else if (ref.getType().equals("VirtualMachine")) {
						if (filters != null && !filters.isEmpty()) {
							boolean flag = false;
							for (String machine : filters) {
								if (name.equalsIgnoreCase(machine)) {
									flag = true;
									break;
								}
							}
							if (!flag) {
								continue;
							}
						}
						// System.out.println(StringUtils.repeat("\t", depth) + "- " + name);
						if (type == VMFolderObjectType.VirtualMachine) {
							out.put(String.format("%s/%s", (parentName == null) ? "" : parentName, name), ref);
						}
					}
				}
			}
		}

		return out;
	}

	/**
	 * Walks the given Datacenter, and finds a Folder reference based on the given path. Paths should be passed as a Unix style folder, for instance: /Customers/E/Example
	 * 
	 * @param datacenter
	 * @param name
	 * @return Returns a {@link ManagedObjectReference} if found, null if not.
	 * @throws RemoteException
	 * @throws RuntimeFault
	 */
	protected static ManagedObjectReference findVirtualMachineFolder(String datacenter, String name, int maxDepth) throws RuntimeFault, RemoteException {
		ManagedObjectReference dc = VsphereQuery.getDatacenterReference(datacenter);
		return VsphereQuery.findVirtualMachineFolder(dc, name, maxDepth);
	}

	/**
	 * Walks the given Datacenter, and finds a Folder reference based on the given path. Paths should be passed as a Unix style folder, for instance: /Customers/E/Example
	 * 
	 * @param datacenter
	 * @param name
	 * @return Returns a {@link ManagedObjectReference} if found, null if not.
	 * @throws RemoteException
	 * @throws RuntimeFault
	 * @throws InvalidProperty
	 */
	protected static ManagedObjectReference findVirtualMachineFolder(ManagedObjectReference dc, String name, int maxDepth) throws InvalidProperty, RuntimeFault, RemoteException {
		String[] nameParts = name.split("/");
		int partCounter = 0;
		ManagedObjectReference vmRoot = VsphereQuery.getVMRootFolder(dc);

		for (String part : nameParts) {
			if (part.equals("") && partCounter == 0) {
				partCounter += 1;
				continue;
			}
			Map<String, ManagedObjectReference> found = VsphereQuery.findVMFolderObjects(Arrays.asList(part), vmRoot, maxDepth, 0, VMFolderObjectType.Folder);

			if (found.size() != 1) {
				break;
			}

			vmRoot = found.values().iterator().next();
			partCounter += 1;
		}

		if (partCounter == nameParts.length) {
			return vmRoot;
		} else {
			return null;
		}
	}

	/**
	 * Walks the given Datacenter, and finds all subfolders of the given path. Paths should be passed as a Unix style folder, for instance: /Customers/E/Example. This function is NOT recursive.
	 * 
	 * @return
	 * @throws RemoteException
	 * @throws RuntimeFault
	 */
	protected static Map<String, ManagedObjectReference> findVirtualMachineFolders(String datacenter, String path, int maxDepth) throws RuntimeFault, RemoteException {
		ManagedObjectReference dc = VsphereQuery.getDatacenterReference(datacenter);
		return VsphereQuery.findVirtualMachineFolders(dc, path, maxDepth);
	}

	/**
	 * Walks the given Datacenter, and finds all subfolders of the given path. Paths should be passed as a Unix style folder, for instance: /Customers/E/Example. This function is NOT recursive.
	 * 
	 * @return
	 * @throws RemoteException
	 * @throws RuntimeFault
	 */
	protected static Map<String, ManagedObjectReference> findVirtualMachineFolders(ManagedObjectReference dc, String path, int maxDepth) throws InvalidProperty, RuntimeFault, RemoteException {
		ManagedObjectReference rootFolder = VsphereQuery.findVirtualMachineFolder(dc, path, 0);

		if (rootFolder == null) {
			return null;
		}

		return VsphereQuery.findVMFolderObjects(null, rootFolder, maxDepth, 0, VMFolderObjectType.Folder);
	}

	protected static ManagedObjectReference getTaskInfoResult(Task task) throws InvalidProperty, RuntimeFault, RemoteException {
		return VsphereQuery.getTaskInfoResult(task.getMOR());
	}

	protected static ManagedObjectReference getTaskInfoResult(ManagedObjectReference taskMor) throws InvalidProperty, RuntimeFault, RemoteException {
		return (ManagedObjectReference) VsphereQuery.getEntityProps(taskMor, new String[] { "info.result" }).get("info.result");
	}

	protected static ManagedObjectReference getDistributedVirtualPortGroupForNetwork(String networkName) throws RuntimeFault, RemoteException {
		return VsphereQuery.getMOREFsInContainerByType(VsphereManager.getServiceInstance().getRootFolder().getMOR(), "DistributedVirtualPortgroup").get(networkName);
	}

	protected static Map<String, VirtualEthernetCard> getVirtualMachineNetworks(VirtualMachine vm) {
		Map<String, VirtualEthernetCard> networks = new HashMap<String, VirtualEthernetCard>();

		VirtualMachineConfigInfo info = vm.getConfig();
		VirtualDevice[] devs = info.getHardware().getDevice();

		for (VirtualDevice virtualDevice : devs) {
			VirtualDeviceBackingInfo back = virtualDevice.getBacking();
			if (back == null) {
				continue;
			}
			if (virtualDevice instanceof VirtualEthernetCard && back instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
				VirtualEthernetCardDistributedVirtualPortBackingInfo dvpbi = (VirtualEthernetCardDistributedVirtualPortBackingInfo) back;
				ManagedObjectReference ref = new ManagedObjectReference();
				ref.setType("DistributedVirtualPortgroup");
				ref.setVal(dvpbi.getPort().getPortgroupKey());
				DistributedVirtualPortgroup dvpg = new DistributedVirtualPortgroup(VsphereManager.getServerConnection(), ref);
				networks.put(dvpg.getName(), (VirtualEthernetCard) virtualDevice);
			}
		}

		return networks;
	}

	protected static Map<String, ClusterComputeResource> getClustersForDatacenter(String datacenter) throws RuntimeFault, RemoteException {
		Datacenter dc = new Datacenter(VsphereManager.getServerConnection(), VsphereQuery.getDatacenterReference(datacenter));
		Map<String, ClusterComputeResource> clusters = new HashMap<String, ClusterComputeResource>();

		List<ManagedObjectReference> folders = new ArrayList<ManagedObjectReference>();

		for (ManagedEntity child : dc.getHostFolder().getChildEntity()) {
			if (child.getMOR().getType().equals("ClusterComputeResource")) {
				clusters.put(child.getName(), new ClusterComputeResource(VsphereManager.getServerConnection(), child.getMOR()));
			} else if (child.getMOR().getType().equals("Folder")) {
				folders.add(child.getMOR());
			}
		}

		for (ManagedObjectReference folder : folders) {
			Map<String, Object> objects = VsphereQuery.getEntityProps(folder, new String[] { "name", "childEntity" });
			if (objects.containsKey("childEntity") && objects.get("childEntity") != null) {
				ArrayOfManagedObjectReference refs = (ArrayOfManagedObjectReference) objects.get("childEntity");
				if (refs.getManagedObjectReference() != null) {
					for (ManagedObjectReference ref : refs.getManagedObjectReference()) {
						if (ref.getType().equals("ClusterComputeResource")) {
							String name = (String) VsphereQuery.getEntityProps(ref, new String[] { "name" }).get("name");
							clusters.put(name, new ClusterComputeResource(VsphereManager.getServerConnection(), ref));
						}
					}
				}
			}
		}
		return clusters;
	}
}
