package com.longdnh.donot_get_left_behind.tenancy.web.dto;

import java.util.List;
import java.util.function.Function;

import com.longdnh.donot_get_left_behind.common.pagination.PageResult;

public record PageResponse<T>(List<T> items, int page, int size, long totalElements) {

    public static <S, T> PageResponse<T> from(PageResult<S> result, Function<? super S, ? extends T> mapper) {
        PageResult<T> mapped = result.map(mapper);
        return new PageResponse<>(mapped.items(), mapped.page(), mapped.size(), mapped.totalElements());
    }
}
