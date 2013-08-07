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

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import nl.cyso.vsphere.client.config.Configuration;
import nl.nekoconeko.configmode.Formatter;

import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;

public class VsphereManager {
	private static boolean isConnected = false;

	private static ServiceInstance serviceInstance;

	static {
		try {
			VsphereManager.connect();
		} catch (Exception e) {
			Formatter.printErrorLine("Failed to connect to vSphere");
			Formatter.printStackTrace(e);
			System.exit(-1);
		}
	}

	/**
	 * Establishes session with the virtual center server.
	 * 
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	private static void connect() throws RemoteException, MalformedURLException {
		if (VsphereManager.isConnected()) {
			return;
		}
		/*
		 * HostnameVerifier hv = new HostnameVerifier() {
		 * @Override public boolean verify(String urlHostName, SSLSession session) { return true; } }; TrustAllTrustManager.trustAllHttpsCertificates(); HttpsURLConnection.setDefaultHostnameVerifier(hv); VsphereManager.SVC_INST_REF.setType(VsphereManager.SVC_INST_NAME); VsphereManager.SVC_INST_REF.setValue(VsphereManager.SVC_INST_NAME); VsphereManager.vimService = new VimService(); VsphereManager.vimPort = VsphereManager.vimService.getVimPort(); Map<String, Object> ctxt = ((BindingProvider) VsphereManager.vimPort).getRequestContext(); ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
		 * Configuration.getString("server")); ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true); try { VsphereManager.serviceContent = vimPort.retrieveServiceContent(VsphereManager.SVC_INST_REF); VsphereManager.vimPort.login(VsphereManager.serviceContent.getSessionManager(), Configuration.getString("username"), Configuration.getString("password"), null); } catch (InvalidLoginFaultMsg e) { Formatter.printErrorLine("Failed to login to vSphere"); Formatter.printStackTrace(e); System.exit(-1); } catch (Exception e) {
		 * Formatter.printErrorLine("Unknown error occurred while connecting to vSphere"); Formatter.printStackTrace(e); System.exit(-1); }
		 */

		VsphereManager.serviceInstance = new ServiceInstance(new URL(Configuration.getString("server")), Configuration.getString("username"), Configuration.getString("password"), true);
		VsphereManager.isConnected = true;
	}

	/**
	 * Disconnects the user session.
	 */
	protected static void disconnect() {
		if (VsphereManager.isConnected()) {
			// TODO: Remove bogus connect() and disconnect()
		}
		isConnected = false;
	}

	protected static boolean isConnected() {
		return isConnected;
	}

	protected static ServiceInstance getServiceInstance() {
		return serviceInstance;
	}

	protected static ServerConnection getServerConnection() {
		return serviceInstance.getServerConnection();
	}
}
