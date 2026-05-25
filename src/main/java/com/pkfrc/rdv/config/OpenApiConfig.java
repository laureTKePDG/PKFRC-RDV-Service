package com.pkfrc.rdv.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rdvOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PKFRC RDV Service API")
                        .description("API REST de gestion de rendez-vous dans les services administratifs")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PK Fokam Research Center")
                                .email("contact@pkfrc.cm")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }
}
