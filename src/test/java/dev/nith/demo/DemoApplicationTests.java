package dev.nith.demo;

import com.aegis.runtime.RuntimeIntelligenceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = RuntimeIntelligenceApplication.class)
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
