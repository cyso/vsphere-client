package nl.cyso.vsphere.client.tools;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import nl.nekoconeko.configmode.Formatter;

public class TrustAllTrustManager implements TrustManager, X509TrustManager {
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

	public static void trustAllHttpsCertificates() {
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
}