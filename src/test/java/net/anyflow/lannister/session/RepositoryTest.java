package net.anyflow.lannister.session;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hazelcast.core.IMap;

public class RepositoryTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testSynchronize() throws Exception {
		IMap<String, Session> map = Repository.SELF.clientIdSessionMap();
		Session session = new Session(null, "test1", 0, false);

		map.put("test1", session);

		session.setWill(new Will(null, null, null, false));

		Session retrieved = map.get("test1");

		Assert.assertNotNull(retrieved.will());
	}
}
