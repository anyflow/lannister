package net.anyflow.lannister;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Do nothing
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