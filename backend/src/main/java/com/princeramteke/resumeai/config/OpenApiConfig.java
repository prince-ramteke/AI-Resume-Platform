package com.princeramteke.resumeai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resumeAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Resume Intelligence Platform")
                        .description("RAG-powered resume scoring and skill-gap analysis")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Prince Ramteke")
                                .email("princeramteke575@gmail.com")));
    }
}
