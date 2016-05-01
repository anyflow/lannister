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