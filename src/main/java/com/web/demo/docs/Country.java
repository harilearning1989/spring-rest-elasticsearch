package com.web.demo.docs;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "all_countries_data")
public record Country(
        @Id String id,
        String name,
        List<String> topLevelDomain,
        String alpha2Code,
        String alpha3Code,
        List<String> callingCodes,
        String capital,
        String region,
        String subregion,
        Long population,
        List<Double> latlng,
        String demonym,
        Double area,
        Double gini,
        List<String> timezones,
        List<String> borders,
        String nativeName,
        String numericCode,
        List<Currency> currencies,
        List<Language> languages
) {}
