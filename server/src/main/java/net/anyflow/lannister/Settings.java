/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anyflow.lannister;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.cert.CertificateException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Settings extends java.util.Properties {

	private static final long serialVersionUID = -3232325711649130464L;

	protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Settings.class);

	private final static String CONFIG_NAME = "lannister.cfg";

	public final static Settings SELF;

	static {
		SELF = new Settings();
	}

	private Map<String, String> webResourceExtensionToMimes;
	private SelfSignedCertificate ssc;

	@SuppressWarnings("unchecked")
	private Settings() {
		try {
			load(Application.class.getClassLoader().getResourceAsStream(CONFIG_NAME));

			ssc = new SelfSignedCertificate();
		}
		catch (IOException e) {
			logger.error("Settings instantiation failed.", e);
		}
		catch (CertificateException e) {
			logger.error(e.getMessage(), e);
		}

		try {
			webResourceExtensionToMimes = (new ObjectMapper()).readValue(getProperty("menton.httpServer.MIME"),
					Map.class);
		}
		catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Integer getInt(String key, Integer defaultValue) {
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

	public static <T> String getWorkingPath(java.lang.Class<T> mainClass) {
		CodeSource codeSource = mainClass.getProtectionDomain().getCodeSource();

		try {
			return (new File(codeSource.getLocation().toURI().getPath())).getParentFile().getPath();
		}
		catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
}