package com.web.demo.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.demo.docs.Country;
import com.web.demo.dtos.CountryResponse;
import com.web.demo.repos.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository repository;
    private final ObjectMapper objectMapper;

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<CountryResponse> getOnlyAsiaCountries() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .match(m -> m
                                .field("region")
                                .query("Asia")
                        )
                )
                .withFields("name", "capital", "region")
                .withMaxResults(1000)
                .build();

        SearchHits<Country> hits = elasticsearchOperations.search(query, Country.class);

        return hits.getSearchHits()
                .stream()
                .map(hit -> {
                    Country c = hit.getContent();
                    return new CountryResponse(
                            c.name(),
                            c.capital(),
                            c.region()
                    );
                }).toList();
    }

    @Override
    public List<String> getAllNames() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))  // ✅ correct
                .withFields("name")
                .withMaxResults(1000)
                .build();

        SearchHits<Country> hits =
                elasticsearchOperations.search(query, Country.class);

        return hits.getSearchHits()
                .stream()
                .map(hit -> hit.getContent().name())
                .toList();
    }

    @Override
    public List<CountryResponse> getCountryData() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withFields("name", "capital", "region") // ✅ multiple fields
                .withMaxResults(1000)
                .build();

        SearchHits<Country> hits =
                elasticsearchOperations.search(query, Country.class);

        return hits.getSearchHits()
                .stream()
                .map(hit -> {
                    Country c = hit.getContent();
                    return new CountryResponse(
                            c.name(),
                            c.capital(),
                            c.region()
                    );
                }).toList();
    }

    @Override
    public void loadData() throws Exception {

        InputStream is = getClass()
                .getResourceAsStream("/allRegionCounties.json");

        List<Country> countries = objectMapper.readValue(
                is,
                new TypeReference<>() {
                }
        );

        // Assign IDs (important for Elasticsearch)
        List<Country> updated = countries.stream()
                .map(c -> new Country(
                        UUID.randomUUID().toString(),
                        c.name(),
                        c.topLevelDomain(),
                        c.alpha2Code(),
                        c.alpha3Code(),
                        c.callingCodes(),
                        c.capital(),
                        c.region(),
                        c.subregion(),
                        c.population(),
                        c.latlng(),
                        c.demonym(),
                        c.area(),
                        c.gini(),
                        c.timezones(),
                        c.borders(),
                        c.nativeName(),
                        c.numericCode(),
                        c.currencies(),
                        c.languages()
                ))
                .toList();

        String message = "Inserted rows are %s".formatted(updated.size());
        System.out.println(message);

        repository.saveAll(updated);
    }
}
