package com.rentflow.shared.adapter.out.config;

import com.rentflow.reservation.ReservationPricingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DomainServiceConfiguration {

    @Bean
    ReservationPricingService reservationPricingService() {
        return new ReservationPricingService();
    }
}
