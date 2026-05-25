package com.pkfrc.rdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RdvServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RdvServiceApplication.class, args);
    }
}
