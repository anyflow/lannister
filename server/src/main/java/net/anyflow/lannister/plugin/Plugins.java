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
import java.util.Set;

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
		load();
	}

	private String getWorkingPath() {
		CodeSource codeSource = Application.class.getProtectionDomain().getCodeSource();

		try {
			return (new File(codeSource.getLocation().toURI().getPath())).getParentFile().getPath();
		}
		catch (URISyntaxException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private URL[] pluginUrls() throws MalformedURLException {
		File dir = new File(getWorkingPath() + "/plugin/");
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

	@SuppressWarnings("unchecked")
	public <T extends Plugin> T get(Class<T> clazz) {
		return (T) plugins.get(clazz);
	}

	@SuppressWarnings("unchecked")
	public <T extends Plugin> T put(Class<T> clazz, T source) {
		return (T) plugins.put(clazz, source);
	}

	private <T extends Plugin> void load(Class<T> clazz, Set<Class<? extends T>> source) {
		if (source == null || source.size() <= 0) {
			if (clazz.equals(Authorization.class)) {
				plugins.put(Authorization.class, new DefaultAuthorization());
				logger.debug("{} plugin loaded", DefaultAuthorization.class.getName());
			}
			else if (clazz.equals(ServiceStatus.class)) {
				plugins.put(ServiceStatus.class, new DefaultServiceStatus());
				logger.debug("{} plugin loaded", DefaultServiceStatus.class.getName());
			}
			else if (clazz.equals(ConnectEventListener.class)) {
				plugins.put(ConnectEventListener.class, new DefaultConnectEventListener());
				logger.debug("{} plugin loaded", DefaultConnectEventListener.class.getName());
			}
			else {
				return;
			}
		}
		else {
			Class<? extends T> plugin = source.stream().findAny().get();

			try {
				plugins.put(clazz, plugin.newInstance());
				logger.debug("{} plugin loaded", plugin.getClass().getName());
			}
			catch (InstantiationException | IllegalAccessException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void load() {
		URLClassLoader classLoader = null;
		try {
			classLoader = new URLClassLoader(pluginUrls(), Plugins.class.getClassLoader());
		}
		catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		}

		Reflections reflections = new Reflections(classLoader, new SubTypesScanner(false));

		load(Authorization.class, reflections.getSubTypesOf(Authorization.class));
		load(ServiceStatus.class, reflections.getSubTypesOf(ServiceStatus.class));
		load(ConnectEventListener.class, reflections.getSubTypesOf(ConnectEventListener.class));
	}
}