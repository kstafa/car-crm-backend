package com.rentflow.customer.model;

import com.rentflow.shared.id.CustomerId;

import java.time.LocalDate;

public record DocumentComplianceSummary(
        CustomerId customerId,
        String customerName,
        String customerEmail,
        String documentType,
        LocalDate expiryDate,
        long daysRemaining) {
}
