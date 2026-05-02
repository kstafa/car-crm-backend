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
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceBillingE2ETest {

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
    private static UUID invoiceId;
    private static UUID depositId;

    @BeforeEach
    void configureHttpClient() {
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    @Order(1)
    void setup_fullRentalCycleUntilReturn_invoiceAutoCreated() {
        String token = loginAsAdmin();
        UUID categoryId = getCategoryIdByName(token, "Economy");
        customerId = createCustomer(token, "Bill", "Payer", "bill.payer@billing.com");
        vehicleId = registerVehicle(token, categoryId, "BILL-001");
        reservationId = createAndConfirmReservation(token, customerId, vehicleId,
                "2027-01-10T09:00:00Z", "2027-01-14T09:00:00Z");

        contractId = openContract(token, reservationId);
        recordPickup(token, contractId, "FULL", 30000);

        ResponseEntity<Map<String, Object>> response = recordReturn(token, contractId, "FULL", 30400);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("damageDetected")).isEqualTo(false);

        invoiceId = UUID.fromString(String.valueOf(response.getBody().get("invoiceId")));
        assertThat(invoiceId).isNotNull();
    }

    @Test
    @Order(2)
    void getInvoice_autoCreatedInvoice_hasSentStatusAndCorrectLineItems() {
        String token = loginAsAdmin();
        Map<String, Object> invoice = getInvoice(token, invoiceId);

        assertThat(invoice.get("status")).isEqualTo("SENT");
        assertThat(invoice.get("outstandingAmount")).isNotNull();

        List<?> items = (List<?>) invoice.get("lineItems");
        assertThat(items)
                .anyMatch(item -> "RENTAL_BASE".equals(((Map<?, ?>) item).get("type")))
                .anyMatch(item -> "TAX".equals(((Map<?, ?>) item).get("type")));
        assertThat(items).noneMatch(item -> "LATE_FEE".equals(((Map<?, ?>) item).get("type")));
    }

    @Test
    @Order(3)
    void getDeposit_autoCreatedDeposit_hasHeldStatus() {
        String token = loginAsAdmin();
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/deposits?status=HELD&customerId=" + customerId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<?> content = (List<?>) response.getBody().get("data");
        assertThat(content).isNotEmpty();

        Map<?, ?> deposit = (Map<?, ?>) content.get(0);
        depositId = UUID.fromString(String.valueOf(deposit.get("id")));
        assertThat(deposit.get("status")).isEqualTo("HELD");
    }

    @Test
    @Order(4)
    void recordPayment_fullAmount_invoiceBecomesFullyPaid() {
        String token = loginAsAdmin();

        Map<String, Object> invoice = getInvoice(token, invoiceId);
        BigDecimal outstanding = new BigDecimal(String.valueOf(invoice.get("outstandingAmount")));
        assertThat(outstanding).isGreaterThan(BigDecimal.ZERO);

        ResponseEntity<Void> response = http.exchange(
                "/api/v1/invoices/" + invoiceId + "/payments",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("amount", outstanding, "method", "CARD"), authHeaders(token)),
                Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Map<String, Object> paid = getInvoice(token, invoiceId);
        assertThat(paid.get("status")).isEqualTo("PAID");
        assertThat(new BigDecimal(String.valueOf(paid.get("outstandingAmount")))).isEqualByComparingTo("0.00");
    }

    @Test
    @Order(5)
    void releaseDeposit_afterFullPayment_depositBecomesReleased() {
        String token = loginAsAdmin();

        ResponseEntity<Void> response = http.exchange(
                "/api/v1/deposits/" + depositId + "/release",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "Rental completed without incident"), authHeaders(token)),
                Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map<String, Object>> detail = http.exchange(
                "/api/v1/deposits/" + depositId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().get("status")).isEqualTo("RELEASED");
    }

    @Test
    @Order(6)
    void downloadInvoicePdf_paidInvoice_returnsBinaryPdf() {
        String token = loginAsAdmin();

        ResponseEntity<byte[]> response = http.exchange(
                "/api/v1/invoices/" + invoiceId + "/pdf",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(new String(response.getBody(), 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @Order(7)
    void requestRefund_paidInvoice_refundCreatedInPendingStatus() {
        String token = loginAsAdmin();

        Map<String, Object> invoice = getInvoice(token, invoiceId);
        BigDecimal paid = new BigDecimal(String.valueOf(invoice.get("paidAmount")));

        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/invoices/" + invoiceId + "/refunds",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "amount", paid.divide(BigDecimal.valueOf(2), RoundingMode.DOWN),
                        "reason", "GOODWILL",
                        "method", "CARD",
                        "notes", "Partial goodwill refund"), authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("refundId")).isNotNull();
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

    private UUID createAndConfirmReservation(String token, UUID customerId, UUID vehicleId, String pickup,
                                             String returns) {
        UUID id = createReservation(token, customerId, vehicleId, pickup, returns);
        confirmReservation(token, id);
        return id;
    }

    private UUID createReservation(String token, UUID customerId, UUID vehicleId, String pickup, String returns) {
        Map<String, Object> body = Map.of(
                "customerId", customerId.toString(),
                "vehicleId", vehicleId.toString(),
                "pickupDatetime", pickup,
                "returnDatetime", returns,
                "extras", List.of(),
                "notes", "Billing E2E reservation");
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

    private ResponseEntity<Map<String, Object>> recordReturn(String token, UUID id, String fuel, int mileage) {
        Map<String, Object> payload = Map.of(
                "postInspection", checklist(true, "Returned clean"),
                "endFuelLevel", fuel,
                "endMileage", mileage,
                "photoKeys", List.of());
        return http.exchange(
                "/api/v1/contracts/" + id + "/return",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
    }

    private Map<String, Object> getInvoice(String token, UUID id) {
        ResponseEntity<Map<String, Object>> response = http.exchange(
                "/api/v1/invoices/" + id,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
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
