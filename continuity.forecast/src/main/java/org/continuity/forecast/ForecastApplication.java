package org.continuity.forecast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @author Alper Hidiroglu
 *
 */
@SpringBootApplication
@EnableEurekaClient
public class ForecastApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForecastApplication.class, args);
	}
}
