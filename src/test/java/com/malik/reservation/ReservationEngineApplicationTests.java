package com.malik.reservation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.flywaydb.core.Flyway;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class ReservationEngineApplicationTests {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("DB_URL", POSTGRES::getJdbcUrl);
		registry.add("DB_USERNAME", POSTGRES::getUsername);
		registry.add("DB_PASSWORD", POSTGRES::getPassword);

		registry.add("JWT_SECRET", () -> "test-secret-key-that-is-at-least-32-bytes-long");
		registry.add("JWT_ACCESS_TOKEN_TTL", () -> "15m");
	}

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads() {
		assertNotNull(flyway);
	}

	@Test
	void flywayMigrationsShouldBeApplied() {
		assertNotNull(flyway.info().current());
		assertEquals("1", flyway.info().current().getVersion().getVersion());
	}
}
