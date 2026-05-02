package com.rentflow.payment.adapter.in.rest;

import com.rentflow.payment.DepositStatus;
import com.rentflow.payment.command.ForfeitDepositCommand;
import com.rentflow.payment.command.ReleaseDepositCommand;
import com.rentflow.payment.model.DepositDetail;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.query.ListDepositsQuery;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DepositMapper {

    public ListDepositsQuery toQuery(DepositStatus status, UUID customerId, int page, int size) {
        return new ListDepositsQuery(status, customerId == null ? null : CustomerId.of(customerId), page, size);
    }

    public ReleaseDepositCommand toReleaseCommand(com.rentflow.payment.DepositId id, DepositActionRequest request,
                                                  StaffId staffId) {
        return new ReleaseDepositCommand(id, request.reason(), staffId);
    }

    public ForfeitDepositCommand toForfeitCommand(com.rentflow.payment.DepositId id, DepositActionRequest request,
                                                  StaffId staffId) {
        return new ForfeitDepositCommand(id, request.reason(), staffId);
    }

    public PageResponse<DepositSummaryResponse> toPageResponse(Page<DepositSummary> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toSummaryResponse).toList(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public DepositSummaryResponse toSummaryResponse(DepositSummary summary) {
        return new DepositSummaryResponse(summary.id().value(), summary.contractId().value(),
                summary.customerId().value(), summary.amount().amount(), summary.amount().currency().getCurrencyCode(),
                summary.status().name(), summary.heldAt(), summary.settledAt());
    }

    public DepositDetailResponse toDetailResponse(DepositDetail detail) {
        return new DepositDetailResponse(detail.id().value(), detail.contractId().value(), detail.customerId().value(),
                detail.invoiceId().value(), detail.amount().amount(), detail.amount().currency().getCurrencyCode(),
                detail.status().name(), detail.releaseReason(), detail.forfeitReason(), detail.heldAt(),
                detail.settledAt());
    }
}
