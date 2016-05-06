package net.anyflow.lannister;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApplicationTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Application.configureLog4j();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Do nothing
	}

	@Test
	public void testMain() {
		Exception target = null;
		try {
			Application.main(null);
			Application.instance().shutdown();
		}
		catch (Exception e) {
			target = e;
		}

		Assert.assertNull(target);
	}
}
