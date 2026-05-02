package com.rentflow.bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContractLifecycleE2ETest {

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
        registry.add("rentflow.storage.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private JdbcTemplate jdbc;

    private static UUID vehicleId;
    private static UUID customerId;
    private static UUID reservationId;
    private static UUID contractId;

    @BeforeEach
    void configureHttpClient() {
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    @Order(1)
    void setup_createCustomerVehicleAndConfirmedReservation() {
        String token = loginAsAdmin();
        UUID categoryId = getCategoryIdByName(token, "Economy");
        customerId = createCustomer(token, "Test", "Driver", "driver@e2e.com");
        vehicleId = registerVehicle(token, categoryId, "E2E-001");
        reservationId = createReservation(token, customerId, vehicleId,
                "2026-11-01T09:00:00Z", "2026-11-05T09:00:00Z");
        confirmReservation(token, reservationId);

        assertThat(getVehicle(token, vehicleId).get("status")).isEqualTo("AVAILABLE");
    }

    @Test
    @Order(2)
    void openContract_fromConfirmedReservation_vehicleBecomesRented() {
        String token = loginAsAdmin();
        contractId = openContract(token, reservationId);

        assertReservationStatus(token, reservationId, "ACTIVE");
        assertThat(getVehicle(token, vehicleId).get("status")).isEqualTo("RENTED");
    }

    @Test
    @Order(3)
    void recordPickup_validChecklist_contractHasPreInspection() {
        String token = loginAsAdmin();
        recordPickup(token, contractId, "FULL", 42000);

        Map<String, Object> detail = getContract(token, contractId);
        assertThat(detail.get("preInspection")).isNotNull();
        assertThat(((Map<?, ?>) detail.get("preInspection")).get("fuelLevel")).isEqualTo("FULL");
    }

    @Test
    @Order(4)
    void recordReturn_noDamageOnTime_vehicleAvailableAndZeroSurcharges() {
        String token = loginAsAdmin();
        Map<String, Object> body = recordReturn(token, contractId, true, null, null, null);

        assertThat(body.get("damageDetected")).isEqualTo(false);
        assertThat(new BigDecimal(body.get("lateFee").toString())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(body.get("fuelSurcharge").toString())).isEqualByComparingTo("0.00");
        assertThat(getVehicle(token, vehicleId).get("status")).isEqualTo("AVAILABLE");
        assertReservationStatus(token, reservationId, "COMPLETED");
    }

    @Test
    @Order(5)
    void recordReturn_withDamage_createsDamageReport() {
        String token = loginAsAdmin();
        UUID categoryId = getCategoryIdByName(token, "Economy");
        UUID vid = registerVehicle(token, categoryId, "E2E-002");
        UUID cid = createCustomer(token, "Dmg", "Test", "dmg@e2e.com");
        UUID resId = createReservation(token, cid, vid, "2026-12-01T09:00:00Z", "2026-12-05T09:00:00Z");
        confirmReservation(token, resId);
        UUID ctrId = openContract(token, resId);
        recordPickup(token, ctrId, "FULL", 10000);

        Map<String, Object> body = recordReturn(token, ctrId, false, "Front bumper dented", "MINOR", "CUSTOMER");

        assertThat(body.get("damageDetected")).isEqualTo(true);
        assertThat(body.get("damageReportId")).isNotNull();
        assertThat(getVehicle(token, vid).get("status")).isEqualTo("AVAILABLE");
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
        assertThat(response.getBody()).isNotNull();
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private UUID createCustomer(String token, String firstName, String lastName, String email) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/customers",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("firstName", firstName, "lastName", lastName, "email", email,
                        "phone", "+33123456789"), authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private UUID createReservation(String token, UUID customerId, UUID vehicleId, String pickup, String returns) {
        Map<String, Object> body = Map.of(
                "customerId", customerId.toString(),
                "vehicleId", vehicleId.toString(),
                "pickupDatetime", pickup,
                "returnDatetime", returns,
                "extras", List.of(),
                "notes", "E2E reservation");
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/reservations",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private void confirmReservation(String token, UUID id) {
        ResponseEntity<Void> response = http.exchange(
                "/api/v1/reservations/" + id + "/confirm",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(token)),
                Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private UUID openContract(String token, UUID reservationId) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/contracts",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("reservationId", reservationId.toString()), authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return UUID.fromString(String.valueOf(response.getBody().get("id")));
    }

    private void recordPickup(String token, UUID id, String fuel, int mileage) {
        Map<String, Object> payload = Map.of(
                "preInspection", checklist(true, "All good"),
                "startFuelLevel", fuel,
                "startMileage", mileage,
                "photoKeys", List.of());
        ResponseEntity<Void> response = http.exchange(
                "/api/v1/contracts/" + id + "/pickup",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders(token)),
                Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private Map<String, Object> recordReturn(String token, UUID id, boolean allOk, String description, String severity,
                                             String liability) {
        Map<String, Object> payload = allOk
                ? Map.of("postInspection", checklist(true, "Returned clean"), "endFuelLevel", "FULL",
                "endMileage", 42350, "photoKeys", List.of())
                : Map.of("postInspection", checklist(false, "Front bumper dented"), "endFuelLevel", "FULL",
                "endMileage", 10200, "photoKeys", List.of(), "damageDescription", description,
                "damageSeverity", severity, "damageLiability", liability, "estimatedDamageCost", 250.00);
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/contracts/" + id + "/return",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private Map<String, Object> getVehicle(String token, UUID id) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/fleet/vehicles/" + id,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private Map<String, Object> getContract(String token, UUID id) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/contracts/" + id,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void assertReservationStatus(String token, UUID reservationId, String status) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/reservations/" + reservationId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(status);
    }

    private static Map<String, Object> checklist(boolean ok, String notes) {
        return Map.of("frontOk", ok, "rearOk", true, "leftSideOk", true, "rightSideOk", true,
                "interiorOk", true, "trunkOk", true, "tiresOk", true, "lightsOk", true, "notes", notes);
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
