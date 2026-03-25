package com.web.demo.dtos;

import java.util.List;

public record CountryResponse(
        String name,
        String capital,
        String region,
        List<String> currencyCodes
) {}
