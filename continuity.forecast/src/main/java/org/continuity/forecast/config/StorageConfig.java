package org.continuity.forecast.config;

import java.nio.file.Paths;

import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.commons.storage.MixedStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MixedStorage<ForecastBundle> forecastStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(Paths.get(storagePath), new ForecastBundle());
	}

}
