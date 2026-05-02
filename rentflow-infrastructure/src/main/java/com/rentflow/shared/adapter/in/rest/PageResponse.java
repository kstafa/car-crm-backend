package com.rentflow.shared.adapter.in.rest;

import java.util.List;

public record PageResponse<T>(List<T> data, PageMeta meta) {
}
