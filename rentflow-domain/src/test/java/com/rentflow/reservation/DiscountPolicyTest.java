package com.rentflow.reservation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscountPolicyTest {

    @Test
    void constructor_validValues_creates() {
        assertDoesNotThrow(() -> new DiscountPolicy(new BigDecimal("0.10"), new BigDecimal("0.30")));
    }

    @Test
    void constructor_agentPercentNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiscountPolicy(new BigDecimal("-0.01"), new BigDecimal("0.30")));
    }

    @Test
    void constructor_agentPercentAboveOne_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiscountPolicy(new BigDecimal("1.01"), new BigDecimal("1.00")));
    }

    @Test
    void constructor_managerPercentBelowAgent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiscountPolicy(new BigDecimal("0.20"), new BigDecimal("0.10")));
    }

    @Test
    void requiresManagerApproval_belowAgentThreshold_returnsFalse() {
        DiscountPolicy policy = new DiscountPolicy(new BigDecimal("0.10"), new BigDecimal("0.30"));

        assertFalse(policy.requiresManagerApproval(new BigDecimal("0.05")));
    }

    @Test
    void requiresManagerApproval_exactlyAtAgentThreshold_returnsFalse() {
        DiscountPolicy policy = new DiscountPolicy(new BigDecimal("0.10"), new BigDecimal("0.30"));

        assertFalse(policy.requiresManagerApproval(new BigDecimal("0.10")));
    }

    @Test
    void requiresManagerApproval_aboveAgentThreshold_returnsTrue() {
        DiscountPolicy policy = new DiscountPolicy(new BigDecimal("0.10"), new BigDecimal("0.30"));

        assertTrue(policy.requiresManagerApproval(new BigDecimal("0.11")));
    }

    @Test
    void requiresManagerApproval_aboveManagerThreshold_returnsTrue() {
        DiscountPolicy policy = new DiscountPolicy(new BigDecimal("0.10"), new BigDecimal("0.30"));

        assertTrue(policy.requiresManagerApproval(new BigDecimal("0.31")));
    }

    @Test
    void defaultPolicy_agentThresholdIsTenPercent() {
        assertEquals(new BigDecimal("0.10"), DiscountPolicy.defaultPolicy().maxAgentDiscountPercent());
    }

    @Test
    void defaultPolicy_managerThresholdIsThirtyPercent() {
        assertEquals(new BigDecimal("0.30"), DiscountPolicy.defaultPolicy().maxManagerDiscountPercent());
    }
}
