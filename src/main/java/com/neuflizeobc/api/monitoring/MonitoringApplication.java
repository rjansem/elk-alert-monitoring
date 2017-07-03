package com.neuflizeobc.api.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application de monitoring
 *
 * @author rjansem
 */
@SpringBootApplication
@EnableScheduling
@EnableElasticsearchRepositories(basePackages = "com.neuflizeobc.api.monitoring.repository")
public class MonitoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitoringApplication.class, args);
	}
}
