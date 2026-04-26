package net.ljcomputing.camelpoc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import net.ljcomputing.camelpoc.dao.DuckDbDAO;

@SpringBootTest
class CamelPocApplicationTests {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CamelPocApplicationTests.class);

	@Autowired
	private DuckDbDAO duckDbDAO;

	@Test
	void contextLoads() {
	}

	@Test
	void testDuckDbDAO() {
		logger.info("Testing DuckDbDAO...");
		assert duckDbDAO != null;
		assert "Alice".equals(duckDbDAO.getNameById(1));
		assert "Bob".equals(duckDbDAO.getNameById(2));
		logger.info("DuckDbDAO tests passed");
	}
}
