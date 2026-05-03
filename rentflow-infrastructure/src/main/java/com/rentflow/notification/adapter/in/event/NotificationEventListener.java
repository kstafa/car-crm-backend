package com.rentflow.notification.adapter.in.event;

import com.rentflow.contract.ContractOpenedEvent;
import com.rentflow.contract.PickupRecordedEvent;
import com.rentflow.contract.ReturnRecordedEvent;
import com.rentflow.customer.Customer;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.command.SendNotificationCommand;
import com.rentflow.notification.port.in.SendNotificationUseCase;
import com.rentflow.payment.DepositReleasedEvent;
import com.rentflow.payment.InvoicePaidEvent;
import com.rentflow.payment.InvoiceSentEvent;
import com.rentflow.reservation.ReservationCancelledEvent;
import com.rentflow.reservation.ReservationConfirmedEvent;
import com.rentflow.reservation.port.out.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SendNotificationUseCase sendNotification;
    private final CustomerRepository customerRepository;
    @SuppressWarnings("unused")
    private final VehicleRepository vehicleRepository;
    @SuppressWarnings("unused")
    private final ReservationRepository reservationRepository;

    @EventListener
    @Async
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        Customer customer = customerRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            return;
        }
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.RESERVATION_CONFIRMED,
                Map.of(
                        "customerName", customer.getFirstName() + " " + customer.getLastName(),
                        "reservationNumber", event.reservationId().value().toString(),
                        "pickupDate", event.rentalPeriod().start().toLocalDate().toString(),
                        "returnDate", event.rentalPeriod().end().toLocalDate().toString()),
                customer.getEmail(), null, null,
                "Reservation", event.reservationId().value().toString()));
    }

    @EventListener
    @Async
    public void onReservationCancelled(ReservationCancelledEvent event) {
        Customer customer = customerRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            return;
        }
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.RESERVATION_CANCELLED,
                Map.of(
                        "customerName", customer.getFirstName() + " " + customer.getLastName(),
                        "reservationNumber", event.reservationId().value().toString(),
                        "reason", event.reason() != null ? event.reason() : ""),
                customer.getEmail(), null, null,
                "Reservation", event.reservationId().value().toString()));
    }

    @EventListener
    @Async
    public void onContractOpened(ContractOpenedEvent event) {
        Customer customer = customerRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            return;
        }
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.CONTRACT_OPENED,
                Map.of(
                        "customerName", customer.getFirstName() + " " + customer.getLastName(),
                        "contractId", event.contractId().value().toString()),
                customer.getEmail(), null, null,
                "Contract", event.contractId().value().toString()));
    }

    @EventListener
    @Async
    public void onPickupRecorded(PickupRecordedEvent event) {
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.PICKUP_RECORDED,
                Map.of("contractId", event.contractId().value().toString()),
                null, null, null,
                "Contract", event.contractId().value().toString()));
    }

    @EventListener
    @Async
    public void onReturnRecorded(ReturnRecordedEvent event) {
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.RETURN_RECORDED,
                Map.of(
                        "contractId", event.contractId().value().toString(),
                        "hasDamage", String.valueOf(event.hasDamage())),
                null, null, null,
                "Contract", event.contractId().value().toString()));
    }

    @EventListener
    @Async
    public void onInvoiceSent(InvoiceSentEvent event) {
        Customer customer = customerRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            return;
        }
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.INVOICE_SENT,
                Map.of(
                        "customerName", customer.getFirstName() + " " + customer.getLastName(),
                        "invoiceId", event.id().value().toString()),
                customer.getEmail(), null, null,
                "Invoice", event.id().value().toString()));
    }

    @EventListener
    @Async
    public void onInvoicePaid(InvoicePaidEvent event) {
        Customer customer = customerRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            return;
        }
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.INVOICE_PAID,
                Map.of(
                        "customerName", customer.getFirstName() + " " + customer.getLastName(),
                        "amount", event.totalAmount().amount().toPlainString(),
                        "currency", event.totalAmount().currency().getCurrencyCode()),
                customer.getEmail(), null, null,
                "Invoice", event.invoiceId().value().toString()));
    }

    @EventListener
    @Async
    public void onDepositReleased(DepositReleasedEvent event) {
        sendNotification.send(new SendNotificationCommand(
                NotificationTrigger.DEPOSIT_RELEASED,
                Map.of(
                        "amount", event.amount().amount().toPlainString(),
                        "reason", event.reason() != null ? event.reason() : ""),
                null, null, null,
                "Deposit", event.depositId().value().toString()));
    }
}
