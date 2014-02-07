package nl.cyso.vsphere.client.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class ResourceHelper {
	public static String readResourceIntoString(Class cls, String path, String charset) throws IOException {
		StringBuilder text = new StringBuilder();
		char[] buffer = new char[256];
		InputStream in = cls.getClassLoader().getResourceAsStream(path);
		Reader r = new InputStreamReader(in, charset);

		try {
			while (true) {
				int rsz = r.read(buffer, 0, buffer.length);
				if (rsz < 0)
					break;
				text.append(buffer, 0, rsz);
			}
		} finally {
			r.close();
		}

		return text.toString();
	}

	public static BufferedReader textToReader(String text) {
		return new BufferedReader(new StringReader(text));
	}
}
