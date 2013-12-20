package nl.cyso.vsphere.client.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.bind.DatatypeConverter;

public class HttpHelper {
	private URL url = null;
	private String username = null;
	private String password = null;

	public HttpHelper(URL url, String username, String password, boolean ignoreCert) {
		this(url, username, password);
		if (ignoreCert) {
			TrustAllTrustManager.trustAllHttpsCertificates();
		}
	}

	public HttpHelper(URL url, String username, String password) {
		this(url);
		this.username = username;
		this.password = password;
	}

	public HttpHelper(URL url, boolean ignoreCert) {
		this(url);
		if (ignoreCert) {
			TrustAllTrustManager.trustAllHttpsCertificates();
		}
	}

	public HttpHelper(URL url) {
		this.url = url;
	}

	private String getBasicAuthenticationHeader() {
		if (this.username != null && this.password != null) {
			String userpass = String.format("%s:%s", this.username, this.password);
			return "Basic " + new String(DatatypeConverter.printBase64Binary(userpass.getBytes()));
		} else {
			return null;
		}
	}

	public int putFile(File file) throws RuntimeException {
		if (!file.isFile()) {
			throw new IllegalArgumentException("Illegal file specified: " + file.getAbsolutePath());
		}

		if (!file.canRead()) {
			throw new IllegalAccessError("Can not read file: " + file.getAbsolutePath());
		}

		HttpURLConnection conn = null;
		OutputStream out = null;
		InputStream in = null;
		int returnCode = 0;
		try {
			conn = (HttpURLConnection) this.url.openConnection();

			if (this.getBasicAuthenticationHeader() != null) {
				conn.setRequestProperty("Authorization", this.getBasicAuthenticationHeader());
			}

			conn.setDoInput(true);
			conn.setDoOutput(true);

			conn.setRequestProperty("Content-Type", "application/octet-stream");
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Length", Long.toString(file.length()));
			out = conn.getOutputStream();
			in = new FileInputStream(file);
			byte[] buf = new byte[102400];
			int len = 0;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();

			returnCode = conn.getResponseCode();
		} catch (Exception e) {
			throw new RuntimeException("File upload failed", e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return returnCode;
	}
}
