package com.rentflow.customer.adapter.in.rest;

import java.util.UUID;

public record CustomerDetailResponse(UUID id, String firstName, String lastName, String email, String phone,
                                     String status, String blacklistReason) {
}
