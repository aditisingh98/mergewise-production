package com.mergewise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mergeWiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MergeWise AI Review API")
                        .description("Enterprise AI-powered pull request review platform")
                        .version("1.0.0")
                        .contact(new Contact().name("MergeWise").email("support@mergewise.dev")));
    }
}
