package net.anyflow.lannister.httphandler.api;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Strings;
import com.jayway.jsonpath.JsonPath;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.http.HttpClient;
import net.anyflow.lannister.http.HttpResponse;

public class ClientsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testClients() throws Exception {
		HttpClient client = new HttpClient("http://localhost:8090/api/clients");
		HttpResponse res = client.post();

		Assert.assertEquals(HttpResponseStatus.OK, res.status());
		Assert.assertFalse(Strings.isNullOrEmpty(JsonPath.read(res.content().toString(CharsetUtil.UTF_8), "$.id")));
	}
}
