package com.rentflow.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectionChecklistTest {

    @Test
    void allOk_noFields_hasDamageFalse() {
        assertFalse(InspectionChecklist.allOk().hasDamage());
    }

    @Test
    void hasDamage_frontNotOk_returnsTrue() {
        assertTrue(new InspectionChecklist(false, true, true, true, true, true, true, true, null).hasDamage());
    }

    @Test
    void hasDamage_rearNotOk_returnsTrue() {
        assertTrue(new InspectionChecklist(true, false, true, true, true, true, true, true, null).hasDamage());
    }

    @Test
    void hasDamage_interiorNotOk_returnsTrue() {
        assertTrue(new InspectionChecklist(true, true, true, true, false, true, true, true, null).hasDamage());
    }

    @Test
    void hasDamage_allOk_returnsFalse() {
        assertFalse(new InspectionChecklist(true, true, true, true, true, true, true, true, null).hasDamage());
    }
}
