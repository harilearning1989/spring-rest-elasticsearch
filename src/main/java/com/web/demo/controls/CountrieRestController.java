package com.web.demo.controls;

import com.web.demo.dtos.CountryResponse;
import com.web.demo.services.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
public class CountrieRestController {

    private final CountryService countryService;

    @GetMapping("asia")
    public List<CountryResponse> getOnlyAsiaCountries() {
        return countryService.getOnlyAsiaCountries();
    }

    @GetMapping("names")
    public List<String> getNames() {
        return countryService.getAllNames();
    }

    @GetMapping("multipleFields")
    public List<CountryResponse> getMultipleFields() {
        return countryService.getCountryData();
    }

}
