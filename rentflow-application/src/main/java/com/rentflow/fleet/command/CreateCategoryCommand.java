package com.rentflow.fleet.command;

import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;

import java.math.BigDecimal;

public record CreateCategoryCommand(String name, String description, Money baseDailyRate, Money depositAmount,
                                    BigDecimal taxRate, StaffId createdBy) {
}
