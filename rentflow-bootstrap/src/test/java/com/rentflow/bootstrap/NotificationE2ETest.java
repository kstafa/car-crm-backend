package com.rentflow.bootstrap;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class NotificationE2ETest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("rentflow_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

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

    @Autowired TestRestTemplate http;

    @BeforeEach
    void configureHttpClient() {
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void confirmReservation_automationRuleActive_notificationFlowAndReportsWork() throws Exception {
        String token = loginAsAdmin();

        UUID templateId = createInAppTemplate(token);
        createAutomationRule(token, templateId);

        UUID categoryId = getCategoryIdByName(token, "Economy");
        UUID customerId = createCustomer(token, "Notif", "Test", "notif.e2e@example.com");
        UUID vehicleId = registerVehicle(token, categoryId, "NOTIF-001");
        UUID reservationId = createReservation(token, customerId, vehicleId,
                "2027-03-01T09:00:00Z", "2027-03-05T09:00:00Z");

        ResponseEntity<Void> confirm = http.exchange("/api/v1/reservations/" + reservationId + "/confirm",
                HttpMethod.PATCH, new HttpEntity<>(authHeaders(token)), Void.class);
        assertThat(confirm.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Thread.sleep(500);

        ResponseEntity<Map<String, Object>> count = http.exchange("/api/v1/notifications/unread-count",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(count.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(count.getBody()).containsKey("count");

        ResponseEntity<Map<String, Object>> kpis = http.exchange("/api/v1/dashboard/kpis",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(kpis.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(kpis.getBody()).containsKey("availableVehiclesNow");

        ResponseEntity<Map<String, Object>> utilization = http.exchange(
                "/api/v1/reports/fleet-utilization?from=2027-03-01&to=2027-03-31",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(utilization.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<byte[]> csv = http.exchange(
                "/api/v1/reports/revenue/export?from=2027-01-01&to=2027-03-31&format=CSV",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), byte[].class);
        assertThat(csv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csv.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat(new String(csv.getBody(), StandardCharsets.UTF_8)).startsWith("Month,Revenue,Rentals");
    }

    private String loginAsAdmin() {
        ResponseEntity<Map<String, Object>> response = http.exchange("/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "admin@rentflow.com", "password", "changeme"), jsonHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return String.valueOf(response.getBody().get("accessToken"));
    }

    private UUID createInAppTemplate(String token) {
        Map<String, Object> body = Map.of(
                "name", "Reservation Confirmed Staff Feed",
                "trigger", "RESERVATION_CONFIRMED",
                "channel", "IN_APP",
                "subjectTemplate", "Reservation {{reservationNumber}} confirmed",
                "bodyTemplate", "Reservation {{reservationNumber}} confirmed for {{customerName}}");
        ResponseEntity<Map<String, Object>> response = http.exchange("/api/v1/notifications/templates",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private void createAutomationRule(String token, UUID templateId) {
        Map<String, Object> body = Map.of(
                "name", "Reservation confirmation in-app",
                "trigger", "RESERVATION_CONFIRMED",
                "templateId", templateId.toString(),
                "delayMinutes", 0);
        ResponseEntity<Map<String, Object>> response = http.exchange("/api/v1/notifications/automation-rules",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private UUID getCategoryIdByName(String token, String name) {
        ResponseEntity<List<Map<String, Object>>> response = http.exchange("/api/v1/fleet/categories",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().stream()
                .filter(category -> name.equals(category.get("name")))
                .map(category -> UUID.fromString(String.valueOf(category.get("id"))))
                .findFirst()
                .orElseThrow();
    }

    private UUID createCustomer(String token, String firstName, String lastName, String email) {
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "phone", "+33123456789");
        ResponseEntity<Map<String, Object>> response = http.exchange("/api/v1/customers",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private UUID registerVehicle(String token, UUID categoryId, String licensePlate) {
        Map<String, Object> body = Map.of(
                "licensePlate", licensePlate,
                "brand", "Toyota",
                "model", "Yaris",
                "year", 2024,
                "categoryId", categoryId.toString(),
                "initialMileage", 1000,
                "description", "Notification E2E vehicle");
        ResponseEntity<Map<String, Object>> response = http.exchange("/api/v1/fleet/vehicles",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private UUID createReservation(String token, UUID customerId, UUID vehicleId, String pickup, String returns) {
        Map<String, Object> body = Map.of(
                "customerId", customerId.toString(),
                "vehicleId", vehicleId.toString(),
                "pickupDatetime", pickup,
                "returnDatetime", returns,
                "extras", List.of(),
                "notes", "Notification E2E");
        ResponseEntity<Map<String, Object>> response = http.exchange("/api/v1/reservations",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
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
