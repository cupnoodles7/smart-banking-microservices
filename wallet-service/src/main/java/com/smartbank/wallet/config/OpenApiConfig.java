package com.smartbank.wallet.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

// Sets the Swagger UI title/blurb, points "Try it out" at the API Gateway,
// and wires up the JWT Bearer "Authorize" button
@Configuration
public class OpenApiConfig {

    @Value("${gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Bean
    public OpenAPI walletServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Service API")
                        .description("Create wallets and move money - top up, transfer, and pay bills. "
                                + "Every call needs the X-Customer-Id header the API Gateway adds after login.")
                        .version("v1"))
                .servers(List.of(new Server()
                        .url(gatewayUrl)
                        .description("API Gateway")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
