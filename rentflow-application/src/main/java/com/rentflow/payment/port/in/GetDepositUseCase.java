package com.rentflow.payment.port.in;

import com.rentflow.payment.DepositId;
import com.rentflow.payment.model.DepositDetail;

public interface GetDepositUseCase {
    DepositDetail get(DepositId id);
}
