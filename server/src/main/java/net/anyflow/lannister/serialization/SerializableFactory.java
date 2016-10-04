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

package net.anyflow.lannister.serialization;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Notification;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.TopicSubscription;

public class SerializableFactory implements DataSerializableFactory {
	public static final int ID = 1;

	@Override
	public IdentifiedDataSerializable create(int classId) {
		switch (classId) {
		case Message.ID:
			return new Message();

		case InboundMessageStatus.ID:
			return new InboundMessageStatus();

		case OutboundMessageStatus.ID:
			return new OutboundMessageStatus();

		case Session.ID:
			return new Session();

		case Notification.ID:
			return new Notification();

		case Topic.ID:
			return new Topic();

		case TopicSubscriber.ID:
			return new TopicSubscriber();

		case TopicSubscription.ID:
			return new TopicSubscription();

		default:
			throw new RuntimeException("Invalid class ID of Hazelcast Serialization [ID=" + classId + "]");
		}
	}
}
