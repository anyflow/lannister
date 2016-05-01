package net.anyflow.menton.http;

import io.netty.channel.ChannelOption;

public interface IHttpClient {

	HttpRequest httpRequest();

	HttpResponse get();

	HttpResponse get(MessageReceiver receiver);

	HttpResponse post();

	HttpResponse post(MessageReceiver receiver);

	HttpResponse put();

	HttpResponse put(MessageReceiver receiver);

	HttpResponse delete();

	HttpResponse delete(MessageReceiver receiver);

	<T> IHttpClient setOption(ChannelOption<T> option, T value);

}