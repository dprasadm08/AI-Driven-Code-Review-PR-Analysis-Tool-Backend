package com.aiprreview.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Driven Code Review API")
                        .version("1.0.0")
                        .description("""
                                REST API for the AI-Driven Code Review & PR Analysis Tool.

                                **Features:**
                                - GitHub repository and pull-request management
                                - AI-powered code analysis (OpenAI / Claude)
                                - Webhook integration for automatic PR analysis
                                - JWT-based authentication

                                **Authentication:** All protected endpoints require a Bearer JWT token.
                                Obtain a token via `POST /api/auth/login` or `POST /api/auth/signup`.
                                """)
                        .contact(new Contact()
                                .name("dprasadm08")
                                .url("https://github.com/dprasadm08"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here (without the 'Bearer ' prefix)")));
    }
}
