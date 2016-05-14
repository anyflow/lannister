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

public class PluginFactory {

	private static Authorization authorization;
	private static EventListener eventListener;
	private static ServiceStatus serviceStatus;

	static {
		load();
	}

	private static void load() {

		// Reflections r = new Reflections();
		// Reflections.collect(arg0, arg1, arg2)
		// URLClassLoader child = new URLClassLoader(myJar.toURL(),
		// PluginFactory.class.getClassLoader());
		// Class classToLoad = Class.forName("com.MyClass", true, child);
		// Method method = classToLoad.getDeclaredMethod("myMethod");
		// Object instance = classToLoad.newInstance();
		// Object result = method.invoke(instance);

		if (authorization == null) {
			authorization = new DefaultAuthorization();
		}
		if (eventListener == null) {
			eventListener = new DefaultEventListener();
		}
		if (serviceStatus == null) {
			serviceStatus = new DefaultServiceStatus();
		}
	}

	public static Authorization authorization() {
		return authorization == null ? null : (Authorization) authorization.clone();
	}

	public static EventListener eventListener() {
		return eventListener == null ? null : (EventListener) eventListener.clone();
	}

	public static ServiceStatus serviceStatus() {
		return serviceStatus == null ? null : (ServiceStatus) serviceStatus.clone();
	}

	public static Authorization authorization(Authorization authorization) {
		if (authorization == null) { return null; }

		Authorization ret = PluginFactory.authorization;
		PluginFactory.authorization = authorization;

		return ret;
	}

	public static EventListener eventListener(EventListener eventListener) {
		if (eventListener == null) { return null; }

		EventListener ret = PluginFactory.eventListener;
		PluginFactory.eventListener = eventListener;

		return ret;
	}

	public static ServiceStatus serviceStatus(ServiceStatus serviceStatus) {
		if (serviceStatus == null) { return null; }

		ServiceStatus ret = PluginFactory.serviceStatus;
		PluginFactory.serviceStatus = serviceStatus;

		return ret;
	}
}
