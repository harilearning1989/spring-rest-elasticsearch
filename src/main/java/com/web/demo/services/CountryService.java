package com.web.demo.services;

import com.web.demo.dtos.CountryResponse;

import java.util.List;

public interface CountryService {
    void loadData() throws Exception;

    List<String> getAllNames();

    List<CountryResponse> getCountryData();

    List<CountryResponse> getOnlyAsiaCountries();
}
