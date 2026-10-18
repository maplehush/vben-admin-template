package com.maplehush.admin.common.api.vo;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {

    public PageResult {
        items = List.copyOf(items);
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
