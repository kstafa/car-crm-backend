package com.rentflow.bootstrap;

import com.rentflow.security.JwtTokenService;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.id.StaffId;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationE2ETest {

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
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JwtTokenService jwtTokenService;

    private static UUID vehicleId;
    private static UUID customerId;
    private static UUID reservationId;

    @BeforeEach
    void configureHttpClient() {
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    @Order(1)
    void fullLifecycle_draftToConfirmed_vehicleBecomesUnavailable() {
        String token = loginAsAdmin();

        customerId = createCustomer(token, "Jean", "Dupont", "jean.dupont@test.com");
        UUID categoryId = getCategoryIdByName(token, "Economy");
        vehicleId = registerVehicle(token, categoryId, "RF-001-AA");

        reservationId = createReservation(token, customerId, vehicleId,
                "2026-08-01T09:00:00Z", "2026-08-05T09:00:00Z");
        assertReservationStatus(token, reservationId, "DRAFT");

        confirmReservation(token, reservationId);
        assertReservationStatus(token, reservationId, "CONFIRMED");

        List<Map<String, Object>> available = getAvailableVehicles(token, categoryId,
                "2026-08-02T09:00:00Z", "2026-08-04T09:00:00Z");
        assertThat(available)
                .noneMatch(vehicle -> vehicleId.toString().equals(String.valueOf(vehicle.get("id"))));

        List<Map<String, Object>> availableLater = getAvailableVehicles(token, categoryId,
                "2026-08-10T09:00:00Z", "2026-08-15T09:00:00Z");
        assertThat(availableLater)
                .anyMatch(vehicle -> vehicleId.toString().equals(String.valueOf(vehicle.get("id"))));

        List<Map<String, Object>> calendar = getCalendar(token, "2026-08-01", "2026-08-07", null);
        assertThat(calendar)
                .anyMatch(entry -> reservationId.toString().equals(String.valueOf(entry.get("reservationId"))));
    }

    @Test
    @Order(2)
    void doubleBooking_secondConfirmRejectedAtServiceLevel_returns409() {
        String token = loginAsAdmin();

        UUID secondReservationId = createReservation(token, customerId, vehicleId,
                "2026-08-03T09:00:00Z", "2026-08-08T09:00:00Z");

        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/reservations/" + secondReservationId + "/confirm",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("VEHICLE_NOT_AVAILABLE");
    }

    @Test
    @Order(3)
    void extendReservation_vehicleFreeForExtension_updatesReturnDate() {
        String token = loginAsAdmin();
        UUID categoryId = getCategoryIdByName(token, "Economy");
        UUID vid = registerVehicle(token, categoryId, "RF-002-BB");
        UUID cid = createCustomer(token, "Marie", "Martin", "marie.martin@test.com");
        UUID reservation = createAndActivateReservation(token, cid, vid,
                "2026-09-01T09:00:00Z", "2026-09-05T09:00:00Z");

        ResponseEntity<Void> extendResponse = http.exchange(
                "/api/v1/reservations/" + reservation + "/extend",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("newReturnDatetime", "2026-09-08T09:00:00Z"), authHeaders(token)),
                Void.class);
        assertThat(extendResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Map<String, Object> detail = getReservation(token, reservation);
        assertThat(String.valueOf(detail.get("returnDatetime"))).startsWith("2026-09-08");
    }

    @Test
    @Order(4)
    void discountEndpoint_aboveThreshold_withoutApprovalPermission_returns403() {
        String adminToken = loginAsAdmin();
        StaffPrincipal restrictedStaff = new StaffPrincipal(
                StaffId.generate(),
                "agent@test.com",
                "AGENT",
                Set.of("RESERVATION_EDIT", "RESERVATION_VIEW", "RESERVATION_CANCEL"));
        String restrictedToken = jwtTokenService.generateAccessToken(restrictedStaff);

        UUID categoryId = getCategoryIdByName(adminToken, "Economy");
        UUID vid = registerVehicle(adminToken, categoryId, "RF-003-CC");
        UUID cid = createCustomer(adminToken, "Paul", "Bernard", "paul.bernard@test.com");
        UUID reservation = createReservation(adminToken, cid, vid,
                "2026-10-01T09:00:00Z", "2026-10-05T09:00:00Z");

        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/reservations/" + reservation + "/discount",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("discountPercent", 0.15), authHeaders(restrictedToken)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
        Map<String, Object> body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "phone", "+33123456789");

        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/customers",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)),
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

    private UUID createAndActivateReservation(String token, UUID customerId, UUID vehicleId, String pickup,
                                              String returns) {
        UUID reservation = createReservation(token, customerId, vehicleId, pickup, returns);
        confirmReservation(token, reservation);
        jdbcTemplate.update("UPDATE reservations SET status = 'ACTIVE' WHERE id = ?", reservation);
        return reservation;
    }

    private void confirmReservation(String token, UUID reservationId) {
        ResponseEntity<Void> response = http.exchange(
                "/api/v1/reservations/" + reservationId + "/confirm",
                HttpMethod.PATCH,
                new HttpEntity<>(authHeaders(token)),
                Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void assertReservationStatus(String token, UUID reservationId, String status) {
        Map<String, Object> detail = getReservation(token, reservationId);
        assertThat(detail.get("status")).isEqualTo(status);
    }

    private Map<String, Object> getReservation(String token, UUID reservationId) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/reservations/" + reservationId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private List<Map<String, Object>> getAvailableVehicles(String token, UUID categoryId, String pickup,
                                                           String returns) {
        String uri = UriComponentsBuilder.fromPath("/api/v1/reservations/availability")
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

    private List<Map<String, Object>> getCalendar(String token, String from, String to, UUID categoryId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/v1/reservations/calendar")
                .queryParam("from", from)
                .queryParam("to", to);
        if (categoryId != null) {
            builder.queryParam("categoryId", categoryId);
        }

        ResponseEntity<List<Map<String, Object>>> response = http.exchange(
                builder.build().encode().toUriString(),
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
