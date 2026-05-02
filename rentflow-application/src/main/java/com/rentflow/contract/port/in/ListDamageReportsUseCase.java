package com.rentflow.contract.port.in;

import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.contract.query.ListDamageReportsQuery;
import org.springframework.data.domain.Page;

public interface ListDamageReportsUseCase {
    Page<DamageReportSummary> list(ListDamageReportsQuery query);
}
