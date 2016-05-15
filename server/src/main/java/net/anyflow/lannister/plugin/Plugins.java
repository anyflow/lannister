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

package net.anyflow.lannister.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.anyflow.lannister.Application;

public class Plugins {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Plugins.class);

	public static Plugins SELF;

	private Map<Class<? extends Plugin>, Plugin> plugins;

	static {
		SELF = new Plugins();
	}

	private Plugins() {
		plugins = Maps.newHashMap();

		plugins.put(Authorization.class, new DefaultAuthorization());
		plugins.put(ServiceStatus.class, new DefaultServiceStatus());
		plugins.put(ConnectEventListener.class, new DefaultConnectEventListener());
		plugins.put(DisconnectEventListener.class, new DefaultDisconnectEventListener());

		load();
	}

	private void load() {
		URLClassLoader classLoader = null;
		try {
			classLoader = new URLClassLoader(pluginJarUrls(), Plugins.class.getClassLoader());
		}
		catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		}

		Reflections reflections = new Reflections(classLoader, new SubTypesScanner(false));

		load(Authorization.class, reflections.getSubTypesOf(Authorization.class).stream()
				.filter(p -> !p.equals(DefaultAuthorization.class)));
		load(ServiceStatus.class, reflections.getSubTypesOf(ServiceStatus.class).stream()
				.filter(p -> !p.equals(DefaultServiceStatus.class)));
		load(ConnectEventListener.class, reflections.getSubTypesOf(ConnectEventListener.class).stream()
				.filter(p -> !p.equals(DefaultConnectEventListener.class)));
		load(DisconnectEventListener.class, reflections.getSubTypesOf(DisconnectEventListener.class).stream()
				.filter(p -> !p.equals(DefaultDisconnectEventListener.class)));
	}

	@SuppressWarnings("unchecked")
	public <T extends Plugin> T get(Class<T> clazz) {
		return (T) plugins.get(clazz).clone();
	}

	@SuppressWarnings("unchecked")
	public <T extends Plugin> T put(Class<T> clazz, T source) {
		return (T) plugins.put(clazz, source);
	}

	private String appRootPath() {
		CodeSource codeSource = Application.class.getProtectionDomain().getCodeSource();

		try {
			return (new File(codeSource.getLocation().toURI().getPath())).getParentFile().getPath();
		}
		catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private URL[] pluginJarUrls() throws MalformedURLException {
		File dir = new File(appRootPath() + "/plugin/");
		File[] files = dir.listFiles();
		if (files != null) {
			List<URL> ret = Lists.newArrayList();

			for (File item : files) {
				ret.add(item.toURI().toURL());
			}

			return ret.toArray(new URL[0]);
		}
		else {
			return new URL[0];
		}
	}

	private <T extends Plugin> void load(Class<T> clazz, Stream<Class<? extends T>> source) {
		Class<? extends T> plugin = source.findAny().orElse(null);
		if (plugin == null) { return; }

		try {
			T instance = plugin.newInstance();
			plugins.put(clazz, instance);
			logger.debug("{} plugin loaded", instance.getClass().getName());
		}
		catch (InstantiationException | IllegalAccessException e) {
			logger.error(e.getMessage(), e);
		}
	}
}