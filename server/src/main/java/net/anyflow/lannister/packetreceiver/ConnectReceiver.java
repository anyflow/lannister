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

package net.anyflow.lannister.packetreceiver;

import com.google.common.base.Strings;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.plugin.Authenticator;
import net.anyflow.lannister.plugin.Authorizer;
import net.anyflow.lannister.plugin.ConnectEventArgs;
import net.anyflow.lannister.plugin.ConnectEventListener;
import net.anyflow.lannister.plugin.IMessage;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.plugin.ServiceChecker;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class ConnectReceiver extends SimpleChannelInboundHandler<MqttConnectMessage> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectReceiver.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttConnectMessage msg) throws Exception {
        logger.debug("packet incoming [message={}]", msg.toString());

        Session session = Session.NEXUS.get(ctx.channel().id());
        if (session != null) {
            session.dispose(true); // [MQTT-3.1.0-2]
            return;
        }

        boolean cleanSession = msg.variableHeader().isCleanSession();
        String clientId = msg.payload().clientIdentifier();

        if (Strings.isNullOrEmpty(clientId)) {
            clientId = generateClientId(ctx, cleanSession);

            if (clientId == null) { return; }
        }

        if (!filterPlugins(ctx, msg)) { return; }

        session = Session.NEXUS.get(clientId); // [MQTT-3.1.2-4]
        boolean sessionPresent = !cleanSession && session != null; // [MQTT-3.2.2-1],[MQTT-3.2.2-2],[MQTT-3.2.2-3]

        if (cleanSession) {
            if (session != null) {
                session.dispose(false); // [MQTT-3.1.4-2]
            }
            session = newSession(msg, cleanSession, clientId); // [MQTT-3.1.2-6]
        }
        else if (session == null) { // [MQTT-3.1.2-4]
            session = newSession(msg, cleanSession, clientId);
        }

        Session.NEXUS.put(session, ctx);

        processRetainedWill(session);

        final Session sessionFinal = session;
        final MqttConnAckMessage acceptMsg = MessageFactory.connack(MqttConnectReturnCode.CONNECTION_ACCEPTED,
                sessionPresent); // [MQTT-3.1.4-4]

        session.send(acceptMsg).addListener(f -> {
            Plugins.SELF.get(ConnectEventListener.class).connectHandled(new ConnectEventArgs() {
                @Override
                public String clientId() {
                    return sessionFinal.clientId();
                }

                @Override
                public IMessage will() {
                    return sessionFinal.will();
                }

                @Override
                public Boolean cleanSession() {
                    return sessionFinal.cleanSession();
                }

                @Override
                public MqttConnectReturnCode returnCode() {
                    return MqttConnectReturnCode.CONNECTION_ACCEPTED;
                }
            });

            if (!sessionFinal.cleanSession()) {
                sessionFinal.completeRemainedMessages(); // [MQTT-4.4.0-1]
            }
        });
    }

    private void processRetainedWill(Session session) {
        if (session.will() == null || !session.will().isRetain()) { return; }

        // [MQTT-3.1.2-16],[MQTT-3.1.2-17]
        Topic topic = Topic.NEXUS.get(session.will().topicName());
        if (topic == null) {
            topic = new Topic(session.will().topicName());
            Topic.NEXUS.insert(topic);
        }

        topic.setRetainedMessage(session.will());
    }

    private String generateClientId(ChannelHandlerContext ctx, boolean cleanSession) {
        if (cleanSession) {
            if (Settings.SELF.getBoolean("mqtt.acceptEmptyClientId", true)) {
                return "Lannister_"
                        + Long.toString(Hazelcast.SELF.generator().getIdGenerator("clientIdGenerator").newId()); // [MQTT-3.1.3-6],[MQTT-3.1.3-7]
            }
            else {
                sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
                return null;
            }
        }
        else {
            sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED); // [MQTT-3.1.3-8]
            return null;
        }
    }

    private Session newSession(MqttConnectMessage msg, boolean cleanSession, String clientId) {
        return new Session(clientId, msg.variableHeader().keepAliveTimeSeconds(), cleanSession, newWill(clientId, msg));
    }

    private Message newWill(String clientId, MqttConnectMessage conn) {
        if (conn.variableHeader().isWillFlag() == false) { return null; } // [MQTT-3.1.2-12]

        return new Message(-1, conn.payload().willTopic(), clientId, conn.payload().willMessage().getBytes(),
                MqttQoS.valueOf(conn.variableHeader().willQos()), conn.variableHeader().isWillRetain());
    }

    private boolean filterPlugins(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String clientId = msg.payload().clientIdentifier();
        String userName = msg.variableHeader().hasUserName() ? msg.payload().userName() : null;
        String password = msg.variableHeader().hasPassword() ? msg.payload().password() : null;

        if (Plugins.SELF.get(ServiceChecker.class).isServiceAvailable() == false) {
            sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
            return false;
        }

        if (Plugins.SELF.get(Authenticator.class).isValid(clientId) == false) {
            sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED); // [MQTT-3.1.3-9]
            return false;
        }
        
        if (Plugins.SELF.get(Authenticator.class).isValid(clientId, userName, password) == false) {
            sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
            return false;
        }
        
        if (Plugins.SELF.get(Authorizer.class).isAuthorized(clientId, userName) == false) {
            sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);
            return false;
        }

        return true;
    }

    private void sendNoneAcceptMessage(ChannelHandlerContext ctx, MqttConnectReturnCode returnCode) {
        assert returnCode != MqttConnectReturnCode.CONNECTION_ACCEPTED;

        MqttConnAckMessage msg = MessageFactory.connack(returnCode, false); // [MQTT-3.2.2-4]

        ctx.channel().writeAndFlush(msg).addListener(f -> {
            logger.debug("packet outgoing [{}]", msg);

            Plugins.SELF.get(ConnectEventListener.class).connectHandled(new ConnectEventArgs() {
                @Override
                public String clientId() {
                    return null;
                }

                @Override
                public IMessage will() {
                    return null;
                }

                @Override
                public Boolean cleanSession() {
                    return null;
                }

                @Override
                public MqttConnectReturnCode returnCode() {
                    return returnCode;
                }
            });
            ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-3.2.2-5],[MQTT-3.1.4-5]
            });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);

        MqttConnectReturnCode returnCode;

        if (cause instanceof MqttIdentifierRejectedException) {
            returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
        }
        else if (cause instanceof IllegalArgumentException && cause.getMessage().contains("invalid QoS")) {
            returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION; // [MQTT-3.1.2-2]
        }
        else if (cause instanceof IllegalArgumentException && cause.getMessage().contains(" is unknown mqtt version")) {
            returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION; // [MQTT-3.1.2-2]
        }
        else if (cause instanceof MqttUnacceptableProtocolVersionException) {
            returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION; // [MQTT-3.1.2-2]
        }
        else {
            super.exceptionCaught(ctx, cause);
            return;
        }

        sendNoneAcceptMessage(ctx, returnCode); // [MQTT-3.2.2-4]
    }
}