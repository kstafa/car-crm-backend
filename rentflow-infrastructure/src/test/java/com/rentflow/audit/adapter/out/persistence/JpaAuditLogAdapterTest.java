package com.rentflow.audit.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.id.StaffId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaAuditLogAdapterTest extends AbstractJpaAdapterTest {

    @Autowired SpringDataAuditLogRepo repo;
    JpaAuditLogAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAuditLogAdapter(repo);
        repo.deleteAll();
    }

    @Test
    void log_validEntry_persistsToDatabase() {
        AuditEntry entry = AuditEntry.of("CREATE_CUSTOMER", "Customer", UUID.randomUUID().toString(), "SYSTEM");

        adapter.log(entry);

        AuditLogJpaEntity saved = repo.findAll().getFirst();
        assertEquals("CREATE_CUSTOMER", saved.actionType);
        assertEquals("Customer", saved.entityType);
    }

    @Test
    void log_nullBeforeAndAfterValues_persistsNulls() {
        adapter.log(AuditEntry.of("ACTION", "Entity", "1", "SYSTEM"));

        AuditLogJpaEntity saved = repo.findAll().getFirst();
        assertNull(saved.beforeValue);
        assertNull(saved.afterValue);
    }

    @Test
    void log_systemActor_persistsSystemString() {
        adapter.log(AuditEntry.of("ACTION", UUID.randomUUID(), null));

        assertEquals("SYSTEM", repo.findAll().getFirst().actor);
    }

    @Test
    void findFiltered_byActionType_returnsMatchingEntries() {
        adapter.log(AuditEntry.of("A", "Entity", "1", "SYSTEM"));
        adapter.log(AuditEntry.of("B", "Entity", "2", "SYSTEM"));

        var result = repo.findFiltered("A", null, null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findFiltered_byActor_returnsMatchingEntries() {
        StaffId actor = StaffId.generate();
        adapter.log(AuditEntry.of("A", UUID.randomUUID(), actor));
        adapter.log(AuditEntry.of("A", UUID.randomUUID(), null));

        var result = repo.findFiltered(null, actor.value().toString(), null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findFiltered_byDateRange_returnsEntriesInRange() {
        Instant now = Instant.now();
        adapter.log(new AuditEntry("A", "Entity", "1", "SYSTEM", now, null, null, null));

        var result = repo.findFiltered(null, null, null, now.minusSeconds(1), now.plusSeconds(1),
                PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findFiltered_noMatches_returnsEmptyPage() {
        adapter.log(AuditEntry.of("A", "Entity", "1", "SYSTEM"));

        var result = repo.findFiltered("MISSING", null, null, null, null, PageRequest.of(0, 10));

        assertTrue(result.isEmpty());
    }
}
