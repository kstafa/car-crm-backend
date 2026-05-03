package com.rentflow.shared.adapter.out.cache;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAvailabilityCacheAdapterTest {

    private static final DateRange PERIOD = new DateRange(
            ZonedDateTime.of(2026, 8, 1, 9, 0, 0, 0, ZoneOffset.UTC),
            ZonedDateTime.of(2026, 8, 5, 9, 0, 0, 0, ZoneOffset.UTC));

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisAvailabilityCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisAvailabilityCacheAdapter(redisTemplate);
    }

    @Test
    void get_redisUnavailable_returnsCacheMiss() {
        when(redisTemplate.opsForValue()).thenThrow(redisUnavailable());

        assertThat(adapter.get(VehicleCategoryId.generate(), PERIOD)).isEmpty();
    }

    @Test
    void put_redisUnavailable_doesNotFailCaller() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(redisUnavailable())
                .when(valueOperations).set(anyString(), anyString(), any());

        assertThatNoException().isThrownBy(() -> adapter.put(VehicleCategoryId.generate(), PERIOD,
                List.of(VehicleId.generate())));
    }

    @Test
    void invalidate_redisUnavailable_doesNotFailCaller() {
        when(redisTemplate.keys(anyString())).thenThrow(redisUnavailable());

        assertThatNoException().isThrownBy(() -> adapter.invalidate(VehicleId.generate()));
    }

    private static RedisConnectionFailureException redisUnavailable() {
        return new RedisConnectionFailureException("down");
    }
}
