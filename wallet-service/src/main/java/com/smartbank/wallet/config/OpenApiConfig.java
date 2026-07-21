package com.smartbank.wallet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Sets the title, blurb and version shown at the top of the Swagger UI page.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI walletServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Wallet Service API")
                .description("Create wallets and move money - top up, transfer, and pay bills. "
                        + "Every call needs the X-Customer-Id header the API Gateway adds after login.")
                .version("v1"));
    }
}
