package com.rentflow.reservation.adapter.out.persistence;

import com.rentflow.reservation.ReservationStatus;
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
}
