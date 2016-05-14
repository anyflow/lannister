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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public class NettyUtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Application.configureLog4j();
	}

	@Test
	public void testCopy() throws Exception {
		String original = "TestString";

		ByteBuf nettyBuf = Unpooled.copiedBuffer(original, CharsetUtil.UTF_8);

		byte[] byteArray = NettyUtil.copy(nettyBuf);

		String copied = new String(byteArray);

		Assert.assertThat(original, Matchers.equalTo(copied));
	}
}