package com.rentflow.shared.adapter.in;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.shared.id.VehicleId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GlobalExceptionHandlerTest.ExceptionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class)
)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ExceptionController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resourceNotFound_returns404WithCorrectCode() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void vehicleNotAvailable_returns409WithCorrectCode() throws Exception {
        mockMvc.perform(get("/test/vehicle-not-available"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_AVAILABLE"));
    }

    @Test
    void invalidStateTransition_returns422WithCorrectCode() throws Exception {
        mockMvc.perform(get("/test/invalid-state"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void domainException_returns422WithCorrectCode() throws Exception {
        mockMvc.perform(get("/test/domain"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
    }

    @Test
    void validationError_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void accessDenied_returns403WithCorrectCode() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void illegalArgument_returns400WithCode() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @RestController
    static class ExceptionController {

        private static final ZonedDateTime START =
                ZonedDateTime.of(2026, 5, 1, 9, 0, 0, 0, ZoneOffset.UTC);

        @GetMapping("/test/resource-not-found")
        public void resourceNotFound() {
            throw new ResourceNotFoundException("missing");
        }

        @GetMapping("/test/vehicle-not-available")
        public void vehicleNotAvailable() {
            throw new VehicleNotAvailableException(VehicleId.generate(), new DateRange(START, START.plusDays(1)));
        }

        @GetMapping("/test/invalid-state")
        public void invalidState() {
            throw new InvalidStateTransitionException("invalid");
        }

        @GetMapping("/test/domain")
        public void domain() {
            throw new DomainException("domain");
        }

        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/test/access-denied")
        public void accessDenied() {
            throw new AccessDeniedException("forbidden");
        }

        @GetMapping("/test/illegal-argument")
        public void illegalArgument() {
            throw new IllegalArgumentException("invalid argument");
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }
}
