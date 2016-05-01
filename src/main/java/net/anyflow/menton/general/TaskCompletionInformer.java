/*
 * Copyright 2016 The Menton Project
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

package net.anyflow.menton.general;

/**
 * @author hyunjeong.park
 *
 */
public interface TaskCompletionInformer {

	/**
	 * @param taskCompletionListener
	 */
	void register(TaskCompletionListener taskCompletionListener);

	/**
	 * @param taskCompletionListener
	 */
	void deregister(TaskCompletionListener taskCompletionListener);

	/**
	 * inform task completion event to all registered object.
	 */
	void inform();
}