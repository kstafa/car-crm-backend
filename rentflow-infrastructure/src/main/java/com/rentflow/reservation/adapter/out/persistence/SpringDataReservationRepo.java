package com.rentflow.reservation.adapter.out.persistence;

import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.model.ConflictRow;
import com.rentflow.reservation.model.ReservationCalendarRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringDataReservationRepo extends JpaRepository<ReservationJpaEntity, UUID> {

    @Query("""
            SELECT r FROM ReservationJpaEntity r
            WHERE r.vehicleId = :vehicleId
              AND r.status NOT IN (com.rentflow.reservation.ReservationStatus.CANCELLED,
                                   com.rentflow.reservation.ReservationStatus.DRAFT)
              AND r.pickupDatetime < :returnDate
              AND r.returnDatetime > :pickupDate
            """)
    List<ReservationJpaEntity> findConflicting(
            @Param("vehicleId") UUID vehicleId,
            @Param("pickupDate") ZonedDateTime pickupDate,
            @Param("returnDate") ZonedDateTime returnDate);

    @Query("""
            SELECT DISTINCT r.vehicleId FROM ReservationJpaEntity r
            WHERE r.vehicleId IN :vehicleIds
              AND r.status NOT IN (com.rentflow.reservation.ReservationStatus.CANCELLED,
                                   com.rentflow.reservation.ReservationStatus.DRAFT)
              AND r.pickupDatetime < :returnDate
              AND r.returnDatetime > :pickupDate
            """)
    List<UUID> findConflictingVehicleIds(
            @Param("vehicleIds") List<UUID> vehicleIds,
            @Param("pickupDate") ZonedDateTime pickupDate,
            @Param("returnDate") ZonedDateTime returnDate);

    @Query("""
            SELECT r FROM ReservationJpaEntity r
            WHERE r.pickupDatetime >= :start
              AND r.pickupDatetime < :end
              AND r.status = com.rentflow.reservation.ReservationStatus.CONFIRMED
            ORDER BY r.pickupDatetime ASC
            """)
    List<ReservationJpaEntity> findTodayPickups(@Param("start") ZonedDateTime start,
                                                @Param("end") ZonedDateTime end);

    @Query("""
            SELECT r FROM ReservationJpaEntity r
            WHERE r.returnDatetime >= :start
              AND r.returnDatetime < :end
              AND r.status = com.rentflow.reservation.ReservationStatus.ACTIVE
            ORDER BY r.returnDatetime ASC
            """)
    List<ReservationJpaEntity> findTodayReturns(@Param("start") ZonedDateTime start,
                                                @Param("end") ZonedDateTime end);

    @Query("""
            SELECT r FROM ReservationJpaEntity r
            WHERE r.returnDatetime < :now
              AND r.status = com.rentflow.reservation.ReservationStatus.ACTIVE
            ORDER BY r.returnDatetime ASC
            """)
    List<ReservationJpaEntity> findOverdue(@Param("now") ZonedDateTime now);

    @Query("""
            SELECT r FROM ReservationJpaEntity r
            WHERE (:status IS NULL OR r.status = :status)
              AND r.pickupDatetime >= :from
              AND r.pickupDatetime < :toExclusive
            ORDER BY r.pickupDatetime DESC
            """)
    Page<ReservationJpaEntity> findFiltered(
            @Param("status") ReservationStatus status,
            @Param("from") ZonedDateTime from,
            @Param("toExclusive") ZonedDateTime toExclusive,
            Pageable pageable);

    @Query("""
            SELECT new com.rentflow.reservation.model.ReservationCalendarRow(
                r.id, r.reservationNumber,
                r.vehicleId, v.licensePlate, v.brand, v.model,
                r.customerId, c.firstName, c.lastName,
                r.pickupDatetime, r.returnDatetime, r.status
            )
            FROM ReservationJpaEntity r
            JOIN VehicleJpaEntity v ON v.id = r.vehicleId
            JOIN CustomerJpaEntity c ON c.id = r.customerId
            WHERE r.status <> com.rentflow.reservation.ReservationStatus.CANCELLED
              AND r.pickupDatetime < :toInstant
              AND r.returnDatetime > :fromInstant
              AND (:categoryId IS NULL OR v.categoryId = :categoryId)
            ORDER BY r.pickupDatetime ASC
            """)
    List<ReservationCalendarRow> findForCalendar(
            @Param("fromInstant") ZonedDateTime fromInstant,
            @Param("toInstant") ZonedDateTime toInstant,
            @Param("categoryId") UUID categoryId);

    @Query("""
            SELECT new com.rentflow.reservation.model.ConflictRow(
                draft.id, draft.reservationNumber,
                draft.vehicleId,
                draft.pickupDatetime, draft.returnDatetime,
                conflict.id, conflict.reservationNumber, conflict.status
            )
            FROM ReservationJpaEntity draft
            JOIN ReservationJpaEntity conflict
                 ON conflict.vehicleId = draft.vehicleId
                AND conflict.status IN (com.rentflow.reservation.ReservationStatus.CONFIRMED,
                                        com.rentflow.reservation.ReservationStatus.ACTIVE)
                AND conflict.pickupDatetime < draft.returnDatetime
                AND conflict.returnDatetime > draft.pickupDatetime
            WHERE draft.status = com.rentflow.reservation.ReservationStatus.DRAFT
            ORDER BY draft.pickupDatetime ASC
            """)
    List<ConflictRow> findDraftConflicts();
}
