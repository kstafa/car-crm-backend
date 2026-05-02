package com.rentflow.contract.port.in;

import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.model.DamageReportDetail;

public interface GetDamageReportUseCase {
    DamageReportDetail get(DamageReportId id);
}
