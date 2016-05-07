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

import io.netty.channel.ChannelId;

public class TestUtil {

	private static int clientIdNo = 0;

	public static String newClientId() {
		String clientIdPrefix = "testClientId";

		synchronized (clientIdPrefix) {
			return clientIdPrefix + Integer.toString(clientIdNo++);
		}
	}

	static public ChannelId newChannelId(String clientId, boolean newChannelId) {
		return new ChannelId() {
			private static final long serialVersionUID = 3931333967922160660L;

			Long idPostfix = Hazelcast.SELF.generator().getIdGenerator("unittest_embeddedchannel").newId();

			@Override
			public int compareTo(ChannelId o) {
				return this.asLongText().equals(o.asLongText()) ? 0 : 1;
			}

			@Override
			public String asShortText() {
				return asLongText();
			}

			@Override
			public String asLongText() {
				if (newChannelId) {
					return clientId + idPostfix.toString();
				}
				else {
					return clientId;
				}
			}

			@Override
			public int hashCode() {
				if (newChannelId) {
					return (clientId + idPostfix.toString()).hashCode();
				}
				else {
					return clientId.hashCode();
				}
			}

			@Override
			public String toString() {
				return asLongText();
			}
		};
	}
}
