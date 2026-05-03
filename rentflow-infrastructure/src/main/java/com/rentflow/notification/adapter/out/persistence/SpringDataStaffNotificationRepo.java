package com.rentflow.notification.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataStaffNotificationRepo extends JpaRepository<StaffNotificationJpaEntity, UUID> {
    Page<StaffNotificationJpaEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    int countByRecipientIdAndReadFalse(UUID recipientId);

    @Modifying
    @Query("UPDATE StaffNotificationJpaEntity n SET n.read = true WHERE n.recipientId = :recipientId")
    void markAllRead(@Param("recipientId") UUID recipientId);
}
