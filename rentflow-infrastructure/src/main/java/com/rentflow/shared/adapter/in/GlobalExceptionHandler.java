package com.rentflow.shared.adapter.in;

import com.rentflow.shared.BlacklistedCustomerException;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> resourceNotFound(ResourceNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(VehicleNotAvailableException.class)
    ResponseEntity<ApiErrorResponse> vehicleNotAvailable(VehicleNotAvailableException ex) {
        return response(HttpStatus.CONFLICT, "VEHICLE_NOT_AVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ResponseEntity<ApiErrorResponse> invalidStateTransition(InvalidStateTransitionException ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATE_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(BlacklistedCustomerException.class)
    ResponseEntity<ApiErrorResponse> blacklistedCustomer(BlacklistedCustomerException ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "CUSTOMER_BLACKLISTED", ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiErrorResponse> domainException(DomainException ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_RULE_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse("VALIDATION_ERROR", "Validation failed", fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> accessDenied(AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage());
    }

    private static ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String error) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, error, Instant.now()));
    }

    record ApiErrorResponse(String code, String error, Instant timestamp) {
    }

    record ValidationErrorResponse(String code, String error, Map<String, String> fieldErrors) {
    }
}
