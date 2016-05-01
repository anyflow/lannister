package net.anyflow.lannister;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Maps;

import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Settings extends java.util.Properties {

	private static final long serialVersionUID = -3232325711649130464L;

	protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Settings.class);

	public final static Settings SELF;

	static {
		SELF = new Settings();
	}

	private Map<String, String> webResourceExtensionToMimes;
	private SelfSignedCertificate ssc;

	private Settings() {
		try {
			load(Application.class.getClassLoader().getResourceAsStream("META-INF/application.properties"));
			ssc = new SelfSignedCertificate();
		}
		catch (IOException e) {
			logger.error("Settings instantiation failed.", e);
		}
		catch (CertificateException e) {
			logger.error(e.getMessage(), e);
		}

		webResourceExtensionToMimes = Maps.newHashMap();

		try {
			JSONObject obj = new JSONObject(getProperty("menton.httpServer.MIME"));
			@SuppressWarnings("unchecked")
			Iterator<String> keys = obj.keys();

			while (keys.hasNext()) {
				String key = keys.next();
				webResourceExtensionToMimes.put(key, obj.get(key).toString());
			}
		}
		catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public int getInt(String key, int defaultValue) {
		String valueString = this.getProperty(key.trim());

		if (valueString == null) { return defaultValue; }

		try {
			return Integer.parseInt(valueString.trim());
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String valueString = this.getProperty(key.trim());

		if (valueString == null) { return defaultValue; }

		return "true".equalsIgnoreCase(valueString.trim());
	}

	public Map<String, String> webResourceExtensionToMimes() {
		return webResourceExtensionToMimes;
	}

	public static Integer tryParse(String text) {
		try {
			return Integer.parseInt(text);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * @return HTTP port. If empty, returns null(the channel will not be
	 *         established).
	 */
	public Integer httpPort() {
		return tryParse(getProperty("menton.httpServer.http.port", null));
	}

	/**
	 * @return HTTPS port. If empty, returns null(the channel will not be
	 *         established).
	 */
	public Integer httpsPort() {
		return tryParse(getProperty("menton.httpServer.https.port", null));
	}

	public File certChainFile() {
		String certFilePath = getProperty("menton.ssl.certChainFilePath", null);

		return "self".equalsIgnoreCase(certFilePath) ? ssc.certificate() : new File(certFilePath);
	}

	public File privateKeyFile() {
		String privateKeyFilePath = getProperty("menton.ssl.privateKeyFilePath", null);

		return "self".equalsIgnoreCase(privateKeyFilePath) ? ssc.privateKey() : new File(privateKeyFilePath);
	}

	/**
	 * @return context root path
	 */
	public String httpContextRoot() {
		String ret = getProperty("menton.httpServer.contextRoot", "/");

		if (ret.equalsIgnoreCase("") || ret.charAt(ret.length() - 1) != '/') {
			ret += "/";
		}

		return ret;
	}

	public String webResourcePhysicalRootPath() {
		return this.getProperty("menton.httpServer.webResourcePhysicalRootPath", null);
	}

	public void setWebResourcePhysicalRootPath(String physicalRootPath) {
		this.setProperty("menton.httpServer.webResourcePhysicalRootPath", physicalRootPath);
	}
}