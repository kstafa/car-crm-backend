package com.rentflow.shared.adapter.out.cache;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
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

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Optional<List<VehicleId>> get(VehicleCategoryId categoryId, DateRange period) {
        String raw = redisTemplate.opsForValue().get(buildKey(categoryId, period));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(raw.split(","))
                .filter(value -> !value.isBlank())
                .map(VehicleId::of)
                .toList());
    }

    @Override
    public void put(VehicleCategoryId categoryId, DateRange period, List<VehicleId> ids) {
        String value = ids.stream()
                .map(id -> id.value().toString())
                .collect(Collectors.joining(","));
        redisTemplate.opsForValue().set(buildKey(categoryId, period), value, TTL);
    }

    @Override
    public void invalidate(VehicleId vehicleId) {
        Set<String> keys = redisTemplate.keys("availability:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private static String buildKey(VehicleCategoryId categoryId, DateRange period) {
        return "availability:%s:%d:%d".formatted(categoryId.value(), period.start().toEpochSecond(),
                period.end().toEpochSecond());
    }
}
