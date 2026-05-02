package com.rentflow.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FleetRegistrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("rentflow_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private TestRestTemplate http;

    @Test
    void registerVehicleAndCheckAvailability_fullFlow() {
        String token = loginAsAdmin();
        UUID categoryId = getCategoryIdByName(token, "Economy");

        UUID vehicleId = registerVehicle(token, categoryId, "AB-123-CD");

        List<Map<String, Object>> list = getVehicles(token);
        assertThat(list).anyMatch(vehicle -> vehicleId.toString().equals(String.valueOf(vehicle.get("id"))));

        List<Map<String, Object>> available = getAvailableVehicles(token, categoryId,
                "2026-07-01T09:00:00Z", "2026-07-05T09:00:00Z");
        assertThat(available).anyMatch(vehicle -> vehicleId.toString().equals(String.valueOf(vehicle.get("id"))));

        Map<String, Object> detail = getVehicle(token, vehicleId);
        assertThat(detail.get("licensePlate")).isEqualTo("AB-123-CD");
        assertThat(detail.get("status")).isEqualTo("AVAILABLE");
    }

    private String loginAsAdmin() {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "admin@rentflow.com", "password", "changeme"), jsonHeaders()),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return String.valueOf(response.getBody().get("accessToken"));
    }

    private UUID getCategoryIdByName(String token, String name) {
        ResponseEntity<List<Map<String, Object>>> response = http.exchange(
                "/api/v1/fleet/categories",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().stream()
                .filter(category -> name.equals(category.get("name")))
                .map(category -> UUID.fromString(String.valueOf(category.get("id"))))
                .findFirst()
                .orElseThrow();
    }

    private UUID registerVehicle(String token, UUID categoryId, String licensePlate) {
        Map<String, Object> body = Map.of(
                "licensePlate", licensePlate,
                "brand", "Toyota",
                "model", "Yaris",
                "year", 2024,
                "categoryId", categoryId.toString(),
                "initialMileage", 1000,
                "description", "Compact city car");

        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/fleet/vehicles",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getVehicles(String token) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/fleet/vehicles",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return (List<Map<String, Object>>) response.getBody().get("content");
    }

    private List<Map<String, Object>> getAvailableVehicles(String token, UUID categoryId, String pickup,
                                                           String returns) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/fleet/availability")
                .queryParam("categoryId", categoryId)
                .queryParam("pickupDatetime", pickup)
                .queryParam("returnDatetime", returns)
                .build()
                .encode()
                .toUriString();

        ResponseEntity<List<Map<String, Object>>> response = http.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private Map<String, Object> getVehicle(String token, UUID vehicleId) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/fleet/vehicles/" + vehicleId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private static HttpHeaders authHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
