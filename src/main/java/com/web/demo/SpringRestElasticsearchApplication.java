package com.web.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringRestElasticsearchApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SpringRestElasticsearchApplication.class, args);
    }

    //@Autowired
    //CountryService service;

    @Override
    public void run(String... args) throws Exception {
        //service.loadData();
        System.out.println("Run Only once");
    }
}
