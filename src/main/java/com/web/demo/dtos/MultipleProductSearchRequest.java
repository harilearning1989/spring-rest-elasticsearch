package com.web.demo.dtos;

import java.util.List;

public record MultipleProductSearchRequest(
        String keyword,
        List<String> brands,   // ✅ multi
        List<String> sizes,    // ✅ multi
        List<String> colors,   // ✅ multi
        Double minPrice,
        Double maxPrice,
        Integer page,
        Integer sizeLimit,
        String sortBy
) {
}
