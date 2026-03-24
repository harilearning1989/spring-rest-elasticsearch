package com.web.demo.repos;

import com.web.demo.docs.Country;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends ElasticsearchRepository<Country, String> {
}
