package com.rentflow.shared.adapter.out.cache;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Primary
@RequiredArgsConstructor
public class RedisAvailabilityCacheAdapter implements AvailabilityCachePort {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final Logger LOG = LoggerFactory.getLogger(RedisAvailabilityCacheAdapter.class);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Optional<List<VehicleId>> get(VehicleCategoryId categoryId, DateRange period) {
        try {
            String raw = redisTemplate.opsForValue().get(buildKey(categoryId, period));
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Arrays.stream(raw.split(","))
                    .filter(value -> !value.isBlank())
                    .map(VehicleId::of)
                    .toList());
        } catch (DataAccessException ex) {
            logCacheFailure("read", "continuing without cached availability", ex);
            return Optional.empty();
        }
    }

    @Override
    public void put(VehicleCategoryId categoryId, DateRange period, List<VehicleId> ids) {
        try {
            String value = ids.stream()
                    .map(id -> id.value().toString())
                    .collect(Collectors.joining(","));
            redisTemplate.opsForValue().set(buildKey(categoryId, period), value, TTL);
        } catch (DataAccessException ex) {
            logCacheFailure("write", "continuing without cached availability", ex);
        }
    }

    @Override
    public void invalidate(VehicleId vehicleId) {
        try {
            Set<String> keys = redisTemplate.keys("availability:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (DataAccessException ex) {
            logCacheFailure("invalidation", "continuing for vehicle " + vehicleId.value(), ex);
        }
    }

    private static String buildKey(VehicleCategoryId categoryId, DateRange period) {
        return "availability:%s:%d:%d".formatted(categoryId.value(), period.start().toEpochSecond(),
                period.end().toEpochSecond());
    }

    private static void logCacheFailure(String operation, String outcome, DataAccessException ex) {
        LOG.warn("Availability cache {} failed; {}: {}", operation, outcome, ex.getMessage());
        LOG.debug("Availability cache {} failure", operation, ex);
    }
}
