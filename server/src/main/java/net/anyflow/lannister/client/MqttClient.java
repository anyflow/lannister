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

package net.anyflow.lannister.client;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.TrustManagerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.message.ConnectOptions;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;

public class MqttClient {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttClient.class);

	private final Bootstrap bootstrap;
	private final TrustManagerFactory trustManagerFactory;
	private final SharedObject sharedObject;

	private Channel channel;
	private EventLoopGroup group;
	private MessageReceiver receiver;

	private Integer currentMessageId;

	private URI uri;
	private ConnectOptions options;

	public MqttClient(String uri) throws URISyntaxException {
		this(uri, false);
	}

	public MqttClient(String uri, boolean useInsecureTrustManagerFactory) throws URISyntaxException {
		this.bootstrap = new Bootstrap();
		this.uri = new URI(uri);
		this.trustManagerFactory = useInsecureTrustManagerFactory ? InsecureTrustManagerFactory.INSTANCE : null;
		this.sharedObject = new SharedObject();
		this.options = new ConnectOptions();
		this.currentMessageId = 0;
	}

	public MqttConnectReturnCode connect() throws InterruptedException {

		Class<? extends SocketChannel> socketChannelClass;

		if (Literals.NETTY_EPOLL.equals(Settings.INSTANCE.nettyTransportMode())) {
			group = new EpollEventLoopGroup(1, new DefaultThreadFactory("client"));
			socketChannelClass = EpollSocketChannel.class;
		}
		else {
			group = new NioEventLoopGroup(1, new DefaultThreadFactory("client"));
			socketChannelClass = NioSocketChannel.class;
		}

		bootstrap.group(group).channel(socketChannelClass).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				if ("mqtts".equalsIgnoreCase(uri.getScheme())) {
					SslContext sslCtx = SslContextBuilder.forClient().trustManager(trustManagerFactory).build();

					ch.pipeline().addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
				}

				ch.pipeline().addLast(MqttDecoder.class.getName(), new MqttDecoder());
				ch.pipeline().addLast(MqttEncoder.class.getName(), MqttEncoder.INSTANCE);
				ch.pipeline().addLast(MqttPacketReceiver.class.getName(),
						new MqttPacketReceiver(MqttClient.this, receiver, sharedObject));
			}
		});

		channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();

		normalizeMessage(options.will());
		send(MessageFactory.connect(options));

		synchronized (sharedObject.locker()) {
			int timeout = Settings.INSTANCE.getInt("mqttclient.responseTimeoutSeconds", 15);

			sharedObject.locker().wait(timeout * 1000);
		}
		if (sharedObject.receivedMessage() == null) { return null; }

		return ((MqttConnAckMessage) sharedObject.receivedMessage()).variableHeader().connectReturnCode();
	}

	public boolean isConnected() {
		return channel != null && channel.isActive();
	}

	public void disconnect(boolean sendDisconnect) {
		if (!isConnected()) { return; }

		if (sendDisconnect) {
			send(MessageFactory.disconnect());
		}

		channel.disconnect().addListener(ChannelFutureListener.CLOSE);
		group.shutdownGracefully();

		channel = null;
		group = null;
	}

	protected ChannelFuture send(MqttMessage message) {
		if (!isConnected()) {
			logger.error("Channel is not active");
			return null;
		}

		return channel.writeAndFlush(message);
	}

	public MqttClient receiver(MessageReceiver receiver) {
		this.receiver = receiver;

		return this;
	}

	public MqttClient connectOptions(ConnectOptions connectOptions) {
		this.options = connectOptions;

		return this;
	}

	public void publish(Message message) {
		normalizeMessage(message);
		send(MessageFactory.publish(message, false));
	}

	public void subscribe(MqttTopicSubscription... topicSubscriptions) throws InterruptedException {
		send(MessageFactory.subscribe(nextMessageId(), topicSubscriptions));

		// TODO error handling,store subscription
	}

	public int nextMessageId() {
		currentMessageId = currentMessageId + 1;

		if (currentMessageId > Message.MAX_MESSAGE_ID_NUM) {
			currentMessageId = Message.MIN_MESSAGE_ID_NUM;
		}

		return currentMessageId;
	}

	private void normalizeMessage(Message message) {
		if (message == null) { return; }

		message.setId(nextMessageId());
		message.publisherId(this.options.clientId());
	}

	public String clientId() {
		return options.clientId();
	}
}