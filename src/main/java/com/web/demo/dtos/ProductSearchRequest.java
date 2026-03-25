package com.web.demo.dtos;

public record ProductSearchRequest(
        String keyword,
        String brand,
        String size,
        String color,
        Double minPrice,
        Double maxPrice,
        Integer page,
        Integer sizeLimit,
        String sortBy
) {}
