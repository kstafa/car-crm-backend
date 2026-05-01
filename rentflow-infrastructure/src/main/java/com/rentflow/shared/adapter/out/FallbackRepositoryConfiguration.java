package com.rentflow.shared.adapter.out;

import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.reservation.port.out.ReservationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FallbackRepositoryConfiguration {
    @Bean
    @ConditionalOnMissingBean(ReservationRepository.class)
    ReservationRepository reservationRepository() {
        return new NoOpReservationRepository();
    }

    @Bean
    @ConditionalOnMissingBean(CustomerRepository.class)
    CustomerRepository customerRepository() {
        return new NoOpCustomerRepository();
    }

    @Bean
    @ConditionalOnMissingBean(VehicleRepository.class)
    VehicleRepository vehicleRepository() {
        return new NoOpVehicleRepository();
    }
}
