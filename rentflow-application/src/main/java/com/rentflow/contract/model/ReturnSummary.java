package com.rentflow.contract.model;

import com.rentflow.contract.DamageReportId;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

public record ReturnSummary(
        ContractId contractId,
        boolean damageDetected,
        DamageReportId damageReportId,
        Money lateFee,
        Money fuelSurcharge,
        Money totalSurcharges,
        InvoiceId invoiceId
) {
}
