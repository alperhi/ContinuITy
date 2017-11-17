package org.continuity.jmeter.config;

import org.continuity.jmeter.io.TestPlanWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Henning Schulz
 *
 */
@Component
public class JMeterConfig {

	@Bean
	public TestPlanWriter testPlanWriter(@Value("${jmeter.configuration.path:configuration/jmeter}") String jmeterHome) {
		return new TestPlanWriter(jmeterHome);
	}

}
