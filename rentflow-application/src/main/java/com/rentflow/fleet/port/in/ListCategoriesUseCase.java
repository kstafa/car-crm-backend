package com.rentflow.fleet.port.in;

import com.rentflow.fleet.model.CategorySummary;

import java.util.List;

public interface ListCategoriesUseCase {
    List<CategorySummary> list();
}
