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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.BindingProvider;

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.ConfigTarget;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPortgroupInfo;
import com.vmware.vim25.DistributedVirtualSwitchInfo;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualFloppy;
import com.vmware.vim25.VirtualFloppyDeviceBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDatastoreInfo;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualMachineNetworkInfo;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VirtualVmxnet3;

public class VsphereClient {

	private VimService vimService;
	private VimPortType vimPort;
	private ServiceContent serviceContent;

	private final ManagedObjectReference SVC_INST_REF = new ManagedObjectReference();
	private final String SVC_INST_NAME = "ServiceInstance";

	private boolean isConnected = false;

	private static class TrustAllTrustManager implements TrustManager, X509TrustManager {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
			return;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
			return;
		}
	}

	private void trustAllHttpsCertificates() {
		// Create a trust manager that does not validate certificate chains:
		TrustManager[] trustAllCerts = new TrustManager[1];
		TrustManager tm = new TrustAllTrustManager();
		trustAllCerts[0] = tm;

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			Formatter.printErrorLine("Error instantiating a SSL context, this should not happen.");
			Formatter.printStackTrace(e);
			System.exit(-1);
		}
		SSLSessionContext sslsc = sc.getServerSessionContext();
		sslsc.setSessionTimeout(0);
		try {
			sc.init(null, trustAllCerts, null);
		} catch (KeyManagementException e) {
			Formatter.printErrorLine("Failed to initialize SSL Security Context, this should not happen.");
			Formatter.printStackTrace(e);
			System.exit(-1);
		}
		javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	/**
	 * Establishes session with the virtual center server.
	 */
	private void connect(String url, String username, String password) {
		HostnameVerifier hv = new HostnameVerifier() {
			@Override
			public boolean verify(String urlHostName, SSLSession session) {
				return true;
			}
		};
		this.trustAllHttpsCertificates();
		HttpsURLConnection.setDefaultHostnameVerifier(hv);

		this.SVC_INST_REF.setType(this.SVC_INST_NAME);
		this.SVC_INST_REF.setValue(this.SVC_INST_NAME);

		this.vimService = new VimService();
		this.vimPort = this.vimService.getVimPort();
		Map<String, Object> ctxt = ((BindingProvider) this.vimPort).getRequestContext();

		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
		ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

		try {
			serviceContent = vimPort.retrieveServiceContent(this.SVC_INST_REF);
			vimPort.login(this.serviceContent.getSessionManager(), username, password, null);
		} catch (InvalidLoginFaultMsg e) {
			Formatter.printErrorLine("Failed to login to vSphere");
			Formatter.printStackTrace(e);
			System.exit(-1);
		} catch (Exception e) {
			Formatter.printErrorLine("Unknown error occurred while connecting to vSphere");
			Formatter.printStackTrace(e);
			System.exit(-1);
		}
		this.isConnected = true;
	}

	/**
	 * Disconnects the user session.
	 */
	private void disconnect() {
		if (this.isConnected) {
			try {
				this.vimPort.logout(this.serviceContent.getSessionManager());
			} catch (RuntimeFaultFaultMsg e) {
				Formatter.printErrorLine("Unknown error occurred while disconnecting from vSphere");
				Formatter.printStackTrace(e);
				System.exit(-1);
			}
		}
		isConnected = false;
	}

	/**
	 * Returns all the MOREFs of the specified type that are present under the container
	 * 
	 * @param folder {@link ManagedObjectReference} of the container to begin the search from
	 * @param morefType Type of the managed entity that needs to be searched
	 * @return Map of name and MOREF of the managed objects present. If none exist then empty Map is returned
	 * @throws InvalidPropertyFaultMsg
	 * @throws RuntimeFaultFaultMsg
	 */
	private Map<String, ManagedObjectReference> getMOREFsInContainerByType(ManagedObjectReference folder, String morefType) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		String PROP_ME_NAME = "name";
		ManagedObjectReference viewManager = this.serviceContent.getViewManager();
		ManagedObjectReference containerView = this.vimPort.createContainerView(viewManager, folder, Arrays.asList(morefType), true);

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

		RetrieveResult rslts = this.vimPort.retrievePropertiesEx(this.serviceContent.getPropertyCollector(), propertyFilterSpecs, new RetrieveOptions());
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
		if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
			listobjcontent.addAll(rslts.getObjects());
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = this.vimPort.continueRetrievePropertiesEx(this.serviceContent.getPropertyCollector(), token);
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
	private Map<String, Object> getEntityProps(ManagedObjectReference entityMor, String[] props) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
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

		RetrieveResult rslts = this.vimPort.retrievePropertiesEx(this.serviceContent.getPropertyCollector(), propertyFilterSpecs, new RetrieveOptions());
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();
		if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
			listobjcontent.addAll(rslts.getObjects());
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = this.vimPort.continueRetrievePropertiesEx(this.serviceContent.getPropertyCollector(), token);
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
	private ConfigTarget getConfigTargetForHost(ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws Exception {
		ManagedObjectReference envBrowseMor = (ManagedObjectReference) getEntityProps(computeResMor, new String[] { "environmentBrowser" }).get("environmentBrowser");
		ConfigTarget configTarget = this.vimPort.queryConfigTarget(envBrowseMor, hostMor);
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
	private List<VirtualDevice> getDefaultDevices(ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws Exception {
		ManagedObjectReference envBrowseMor = (ManagedObjectReference) getEntityProps(computeResMor, new String[] { "environmentBrowser" }).get("environmentBrowser");
		VirtualMachineConfigOption cfgOpt = this.vimPort.queryConfigOption(envBrowseMor, null, hostMor);
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

	private String getVolumeName(String volName) {
		String volumeName = null;
		if (volName != null && volName.length() > 0) {
			volumeName = "[" + volName + "]";
		} else {
			volumeName = "[Local]";
		}

		return volumeName;
	}

	/**
	 * Creates the virtual disk.
	 * 
	 * @param volName the vol name
	 * @param diskCtlrKey the disk ctlr key
	 * @param datastoreRef the datastore ref
	 * @param diskSizeMB the disk size in mb
	 * @return the virtual device config spec object
	 */
	private VirtualDeviceConfigSpec createVirtualDisk(String volName, int diskCtlrKey, ManagedObjectReference datastoreRef, int diskSizeMB) {
		// String volumeName = String.format("%stest.provisioning.cyso.net/test.provisioning.cyso.net.vmdk", this.getVolumeName(volName));
		VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();

		VirtualDisk disk = new VirtualDisk();
		VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();

		diskfileBacking.setFileName("");
		diskfileBacking.setDatastore(datastoreRef);
		diskfileBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());
		diskfileBacking.setThinProvisioned(false);
		diskfileBacking.setEagerlyScrub(false);

		disk.setKey(new Integer(0));
		disk.setControllerKey(new Integer(diskCtlrKey));
		disk.setUnitNumber(new Integer(0));
		disk.setCapacityInKB(1024 * diskSizeMB);
		disk.setBacking(diskfileBacking);

		diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
		diskSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
		diskSpec.setDevice(disk);

		return diskSpec;
	}

	/**
	 * Creates the vm config spec object.
	 * 
	 * @param vmName the vm name
	 * @param datastoreName the datastore name
	 * @param diskSizeMB the disk size in mb
	 * @param computeResMor the compute res moref
	 * @param hostMor the host mor
	 * @return the virtual machine config spec object
	 * @throws Exception the exception
	 */
	private VirtualMachineConfigSpec createVmConfigSpec(String vmName, String datastoreName, int diskSizeMB, String mac, String network, ManagedObjectReference computeResMor, ManagedObjectReference hostMor) throws Exception {
		ConfigTarget configTarget = this.getConfigTargetForHost(computeResMor, hostMor);
		List<VirtualDevice> defaultDevices = this.getDefaultDevices(computeResMor, hostMor);
		VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
		String networkName = null;
		String switchName = null;
		if (configTarget.getNetwork() != null) {
			boolean flag = false;
			for (VirtualMachineNetworkInfo netInfo : configTarget.getNetwork()) {
				NetworkSummary netSummary = netInfo.getNetwork();
				System.out.println("Network: " + netSummary.getName());
				if (netSummary.isAccessible() && netSummary.getName().contains(network)) {
					networkName = netSummary.getName();
					flag = true;
					break;
				}
			}
			if (!flag) {
				for (DistributedVirtualPortgroupInfo portGroup : configTarget.getDistributedVirtualPortgroup()) {
					String pgName = portGroup.getPortgroupName();
					System.out.println("PortGroup: " + pgName);
					if (pgName.contains(network)) {
						networkName = pgName;
						break;
					}
				}
				for (DistributedVirtualSwitchInfo switchI : configTarget.getDistributedVirtualSwitch()) {
					String swName = switchI.getSwitchName();
					System.out.println("Switch: " + swName);
					if (swName.contains("dvSwitch")) {
						switchName = switchI.getSwitchUuid();
						break;
					}
				}
			}
		}
		ManagedObjectReference datastoreRef = null;
		if (datastoreName != null) {
			boolean flag = false;
			for (VirtualMachineDatastoreInfo vdsInfo : configTarget.getDatastore()) {
				DatastoreSummary dsSummary = vdsInfo.getDatastore();
				System.out.println("Datastore: " + dsSummary.getName());
				if (dsSummary.getName().equals(datastoreName)) {
					flag = true;
					if (dsSummary.isAccessible()) {
						datastoreRef = dsSummary.getDatastore();
					} else {
						throw new RuntimeException("Specified Datastore is not accessible");
					}
					break;
				}
			}
			if (!flag) {
				throw new RuntimeException("Specified Datastore is not Found");
			}
		} else {
			boolean flag = false;
			for (VirtualMachineDatastoreInfo vdsInfo : configTarget.getDatastore()) {
				DatastoreSummary dsSummary = vdsInfo.getDatastore();
				System.out.println("Datastore: " + dsSummary.getName());
				if (dsSummary.isAccessible()) {
					datastoreName = dsSummary.getName();
					datastoreRef = dsSummary.getDatastore();
					flag = true;
					break;
				}
			}
			if (!flag) {
				throw new RuntimeException("No Datastore found on host");
			}
		}
		String datastoreVolume = this.getVolumeName(datastoreName);
		VirtualMachineFileInfo vmfi = new VirtualMachineFileInfo();
		vmfi.setVmPathName(String.format("%s", datastoreVolume));
		configSpec.setFiles(vmfi);
		// Add a scsi controller
		int diskCtlrKey = 1;
		VirtualDeviceConfigSpec scsiCtrlSpec = new VirtualDeviceConfigSpec();
		scsiCtrlSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
		VirtualSCSIController scsiCtrl = new ParaVirtualSCSIController();
		scsiCtrl.setBusNumber(0);
		scsiCtrlSpec.setDevice(scsiCtrl);
		scsiCtrl.setKey(diskCtlrKey);
		scsiCtrl.setSharedBus(VirtualSCSISharing.NO_SHARING);
		String ctlrType = scsiCtrl.getClass().getName();
		ctlrType = ctlrType.substring(ctlrType.lastIndexOf(".") + 1);

		// Find the IDE controller
		VirtualDevice ideCtlr = null;
		for (int di = 0; di < defaultDevices.size(); di++) {
			if (defaultDevices.get(di) instanceof VirtualIDEController) {
				ideCtlr = defaultDevices.get(di);
				break;
			}
		}

		// Add a floppy
		VirtualDeviceConfigSpec floppySpec = new VirtualDeviceConfigSpec();
		floppySpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
		VirtualFloppy floppy = new VirtualFloppy();
		VirtualFloppyDeviceBackingInfo flpBacking = new VirtualFloppyDeviceBackingInfo();
		flpBacking.setDeviceName("/dev/fd0");
		floppy.setBacking(flpBacking);
		floppy.setKey(3);
		floppySpec.setDevice(floppy);

		// Add a cdrom based on a physical device
		VirtualDeviceConfigSpec cdSpec = null;

		if (ideCtlr != null) {
			cdSpec = new VirtualDeviceConfigSpec();
			cdSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
			VirtualCdrom cdrom = new VirtualCdrom();
			VirtualCdromIsoBackingInfo cdDeviceBacking = new VirtualCdromIsoBackingInfo();
			cdDeviceBacking.setDatastore(datastoreRef);
			cdDeviceBacking.setFileName(datastoreVolume + "testcd.iso");
			cdrom.setBacking(cdDeviceBacking);
			cdrom.setKey(20);
			cdrom.setControllerKey(new Integer(ideCtlr.getKey()));
			cdrom.setUnitNumber(new Integer(0));
			cdSpec.setDevice(cdrom);
		}

		// Create a new disk - file based - for the vm
		VirtualDeviceConfigSpec diskSpec = null;
		diskSpec = this.createVirtualDisk(datastoreName, diskCtlrKey, datastoreRef, diskSizeMB);

		// Add a NIC. the network Name must be set as the device name to create the NIC.
		VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();
		if (networkName != null) {
			nicSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
			VirtualEthernetCard nic = new VirtualVmxnet3();
			// VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
			VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
			DistributedVirtualPortgroupInfo port = new DistributedVirtualPortgroupInfo();
			ManagedObjectReference netw = this.getMOREFsInContainerByType(this.serviceContent.getRootFolder(), "DistributedVirtualPortgroup").get(networkName);
			// ManagedObjectReference swtch = this.getMOREFsInContainerByType(this.serviceContent.getRootFolder(), "DistributedVirtualSwitch").get(switchName);
			port.setPortgroup(netw);
			DistributedVirtualSwitchPortConnection p = new DistributedVirtualSwitchPortConnection();
			p.setPortgroupKey(netw.getValue());

			// ManagedObjectReference switchmor = (ManagedObjectReference) this.getEntityProps(netw, new String[] { "parent" }).get("parent");
			p.setSwitchUuid(switchName);

			// nicBacking.setDeviceName("Primary NIC");
			// nicBacking.setNetwork(netw);
			nicBacking.setPort(p);
			nic.setBacking(nicBacking);
			nic.setKey(4);
			if (mac != null) {
				nic.setAddressType("manual");
				nic.setMacAddress(mac);
			} else {
				nic.setAddressType("generated");
			}
			nicSpec.setDevice(nic);
		}

		List<VirtualDeviceConfigSpec> deviceConfigSpec = new ArrayList<VirtualDeviceConfigSpec>();
		deviceConfigSpec.add(scsiCtrlSpec);
		// deviceConfigSpec.add(floppySpec);
		deviceConfigSpec.add(diskSpec);
		if (ideCtlr != null) {
			// deviceConfigSpec.add(cdSpec);
			deviceConfigSpec.add(nicSpec);
		} else {
			deviceConfigSpec = new ArrayList<VirtualDeviceConfigSpec>();
			deviceConfigSpec.add(nicSpec);
		}
		configSpec.getDeviceChange().addAll(deviceConfigSpec);
		return configSpec;
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
	private Object[] waitForValues(ManagedObjectReference objmor, String[] filterProps, String[] endWaitProps, Object[][] expectedVals) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
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

		ManagedObjectReference filterSpecRef = vimPort.createFilter(serviceContent.getPropertyCollector(), spec, true);

		boolean reached = false;

		UpdateSet updateset = null;
		List<PropertyFilterUpdate> filtupary = null;
		List<ObjectUpdate> objupary = null;
		List<PropertyChange> propchgary = null;
		while (!reached) {
			updateset = vimPort.waitForUpdates(serviceContent.getPropertyCollector(), version);
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
		vimPort.destroyPropertyFilter(filterSpecRef);
		return filterVals;
	}

	private static void updateValues(String[] props, Object[] vals, PropertyChange propchg) {
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
	private boolean getTaskResultAfterDone(ManagedObjectReference task) throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
		boolean retVal = false;

		// info has a property - state for state of the task
		Object[] result = this.waitForValues(task, new String[] { "info.state", "info.error" }, new String[] { "state" }, new Object[][] { new Object[] { TaskInfoState.SUCCESS, TaskInfoState.ERROR } });

		if (result[0].equals(TaskInfoState.SUCCESS)) {
			retVal = true;
		}
		if (result[1] instanceof LocalizedMethodFault) {
			throw new RuntimeException(((LocalizedMethodFault) result[1]).getLocalizedMessage());
		}
		return retVal;
	}

	public void createVirtualMachine() throws RemoteException, Exception {
		if (!this.isConnected) {
			this.connect(Configuration.getString("server"), Configuration.getString("username"), Configuration.getString("password"));
		}

		ManagedObjectReference dcmor = this.getMOREFsInContainerByType(this.serviceContent.getRootFolder(), "Datacenter").get(Configuration.get("dc"));

		if (dcmor == null) {
			Formatter.printErrorLine("Datacenter " + Configuration.get("dc") + " not found.");
			return;
		}
		ManagedObjectReference hostmor = this.getMOREFsInContainerByType(dcmor, "HostSystem").get(Configuration.get("esxnode"));
		if (hostmor == null) {
			Formatter.printErrorLine("Host " + Configuration.get("esxnode") + " not found");
			return;
		}

		ManagedObjectReference crmor = (ManagedObjectReference) this.getEntityProps(hostmor, new String[] { "parent" }).get("parent");
		if (crmor == null) {
			Formatter.printErrorLine("No Compute Resource Found On Specified Host");
			return;
		}

		ManagedObjectReference resourcepoolmor = (ManagedObjectReference) this.getEntityProps(crmor, new String[] { "resourcePool" }).get("resourcePool");
		ManagedObjectReference vmFolderMor = (ManagedObjectReference) this.getEntityProps(dcmor, new String[] { "vmFolder" }).get("vmFolder");
		VirtualMachineConfigSpec vmConfigSpec = this.createVmConfigSpec(Configuration.getString("esxnode"), Configuration.getString("storage"), Integer.valueOf(Configuration.getString("disk")), Configuration.getString("mac"), Configuration.getString("network"), crmor, hostmor);
		vmConfigSpec.setName(Configuration.getString("fqdn"));
		vmConfigSpec.setAnnotation("VirtualMachine Annotation");
		vmConfigSpec.setMemoryMB(Long.valueOf(Configuration.getString("memory")));
		vmConfigSpec.setNumCPUs(Integer.valueOf(Configuration.getString("cpu")));
		vmConfigSpec.setGuestId(VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST.value());
		ManagedObjectReference taskmor = vimPort.createVMTask(vmFolderMor, vmConfigSpec, resourcepoolmor, hostmor);
		if (this.getTaskResultAfterDone(taskmor)) {
			System.out.printf("Success: Creating VM  - [ %s ] %n", Configuration.get("fqdn"));
		} else {
			String msg = "Failure: Creating [ " + Configuration.get("fqdn") + "] VM";
			throw new RuntimeException(msg);
		}
		ManagedObjectReference vmMor = (ManagedObjectReference) this.getEntityProps(taskmor, new String[] { "info.result" }).get("info.result");
		System.out.println("Powering on the newly created VM " + Configuration.get("fqdn"));

		// Start the Newly Created VM.
		// this.powerOnVM(vmMor);

		this.disconnect();
	}

	/**
	 * Power on vm.
	 * 
	 * @param vmMor the vm moref
	 * @throws RemoteException the remote exception
	 * @throws Exception the exception
	 */
	public void powerOnVM(ManagedObjectReference vmMor) throws RemoteException, Exception {
		ManagedObjectReference cssTask = this.vimPort.powerOnVMTask(vmMor, null);
		if (this.getTaskResultAfterDone(cssTask)) {
			System.out.println("Success: VM started Successfully");
		} else {
			String msg = "Failure: starting [ " + vmMor.getValue() + "] VM";
			throw new RuntimeException(msg);
		}
	}

}
