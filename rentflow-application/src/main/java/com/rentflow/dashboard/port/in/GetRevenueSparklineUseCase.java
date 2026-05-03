package com.rentflow.dashboard.port.in;

import com.rentflow.dashboard.model.DailyRevenue;

import java.util.List;

public interface GetRevenueSparklineUseCase {
    List<DailyRevenue> getSparkline(int lastDays);
}
