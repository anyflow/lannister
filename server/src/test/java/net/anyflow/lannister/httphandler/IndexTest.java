package net.anyflow.lannister.httphandler;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.http.HttpClient;
import net.anyflow.lannister.http.HttpResponse;

public class IndexTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testService() throws Exception {
		HttpClient client = new HttpClient("http://localhost:" + Settings.INSTANCE.httpPort() + "/index");
		HttpResponse res = client.get();

		Assert.assertEquals(HttpResponseStatus.OK, res.status());
		Assert.assertTrue(res.content().toString(CharsetUtil.UTF_8).length() > 0);
	}
}