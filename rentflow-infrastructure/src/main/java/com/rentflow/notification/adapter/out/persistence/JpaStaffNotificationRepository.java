package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.NotificationId;
import com.rentflow.notification.StaffNotification;
import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.notification.port.out.StaffNotificationRepository;
import com.rentflow.shared.id.StaffId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
class JpaStaffNotificationRepository implements StaffNotificationRepository {
    private final SpringDataStaffNotificationRepo repo;
    private final NotificationJpaMapper mapper;

    @Override
    public void save(StaffNotification notification) {
        repo.save(mapper.toJpa(notification));
    }

    @Override
    public void saveAll(List<StaffNotification> notifications) {
        repo.saveAll(notifications.stream().map(mapper::toJpa).toList());
    }

    @Override
    public Optional<StaffNotification> findById(NotificationId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Page<StaffNotificationSummary> findByRecipient(StaffId staffId, Pageable pageable) {
        return repo.findByRecipientIdOrderByCreatedAtDesc(staffId.value(), pageable).map(mapper::toSummary);
    }

    @Override
    public int countUnread(StaffId staffId) {
        return repo.countByRecipientIdAndReadFalse(staffId.value());
    }

    @Override
    @Transactional
    public void markAllRead(StaffId staffId) {
        repo.markAllRead(staffId.value());
    }
}
