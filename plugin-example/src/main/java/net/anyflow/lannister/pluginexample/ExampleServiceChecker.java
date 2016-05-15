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

package net.anyflow.lannister.pluginexample;

import net.anyflow.lannister.plugin.Plugin;
import net.anyflow.lannister.plugin.ServiceChecker;

public class ExampleServiceChecker implements ServiceChecker {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExampleServiceChecker.class);

	@Override
	public Plugin clone() {
		return new ExampleServiceChecker();
	}

	@Override
	public boolean isServiceAvailable() {
		logger.debug("ExampleServiceStatus.isServiceAvailable() called");
		return true;
	}
}