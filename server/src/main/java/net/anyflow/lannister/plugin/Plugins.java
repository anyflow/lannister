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
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.anyflow.lannister.Application;

public class Plugins {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Plugins.class);

	public static Plugins SELF;

	private Authorization authorization;
	private EventListener eventListener;
	private ServiceStatus serviceStatus;

	static {
		SELF = new Plugins();
	}

	private Plugins() {
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

	private void load() {
		URLClassLoader classLoader = null;
		try {
			classLoader = new URLClassLoader(pluginUrls(), Plugins.class.getClassLoader());
		}
		catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		}

		if (classLoader == null) {
			loadDefaults();
			return;
		}

		Reflections reflections = new Reflections(classLoader, new SubTypesScanner(false));

		Set<Class<? extends Authorization>> authorizations = reflections.getSubTypesOf(Authorization.class);
		Set<Class<? extends EventListener>> eventListeners = reflections.getSubTypesOf(EventListener.class);
		Set<Class<? extends ServiceStatus>> serviceStatuses = reflections.getSubTypesOf(ServiceStatus.class);

		Set<Class<? extends Plugin>> plugins = Sets.newHashSet();
		plugins.addAll(authorizations);
		plugins.addAll(eventListeners);
		plugins.addAll(serviceStatuses);

		plugins.stream().forEach(p -> {
			try {
				final int mods = p.getModifiers();
				if (Modifier.isAbstract(mods) || Modifier.isInterface(mods)) { return; }
				if (p.equals(DefaultAuthorization.class) || p.equals(DefaultEventListener.class)
						|| p.equals(DefaultServiceStatus.class)) { return; }

				if (Authorization.class.isAssignableFrom(p)) {
					authorization = (Authorization) p.newInstance();
				}
				if (EventListener.class.isAssignableFrom(p)) {
					eventListener = (EventListener) p.newInstance();
				}
				if (ServiceStatus.class.isAssignableFrom(p)) {
					serviceStatus = (ServiceStatus) p.newInstance();
				}

				logger.debug("{} plugin loaded", p.getName());
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
				return;
			}
			;
		});

		loadDefaults();
	}

	private void loadDefaults() {
		if (authorization == null) {
			authorization = new DefaultAuthorization();
			logger.debug("{} plugin loaded", authorization.getClass().getName());
		}
		if (eventListener == null) {
			eventListener = new DefaultEventListener();
			logger.debug("{} plugin loaded", eventListener.getClass().getName());

		}
		if (serviceStatus == null) {
			serviceStatus = new DefaultServiceStatus();
			logger.debug("{} plugin loaded", serviceStatus.getClass().getName());

		}
	}

	public Authorization authorization() {
		return authorization == null ? null : (Authorization) authorization.clone();
	}

	public EventListener eventListener() {
		return eventListener == null ? null : (EventListener) eventListener.clone();
	}

	public ServiceStatus serviceStatus() {
		return serviceStatus == null ? null : (ServiceStatus) serviceStatus.clone();
	}

	public Authorization authorization(Authorization authorization) {
		if (authorization == null) { return null; }

		Authorization ret = this.authorization;
		this.authorization = authorization;

		return ret;
	}

	public EventListener eventListener(EventListener eventListener) {
		if (eventListener == null) { return null; }

		EventListener ret = this.eventListener;
		this.eventListener = eventListener;

		return ret;
	}

	public ServiceStatus serviceStatus(ServiceStatus serviceStatus) {
		if (serviceStatus == null) { return null; }

		ServiceStatus ret = this.serviceStatus;
		this.serviceStatus = serviceStatus;

		return ret;
	}
}