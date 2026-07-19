package com.smartbank.wallet.config;

import com.smartbank.wallet.constants.WalletConstants;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Propagates the caller's identity onto outbound Feign calls.
 *
 * <p>Service-to-service calls (wallet → account, wallet → transaction) go direct via
 * Eureka and bypass the API Gateway, so they carry no identity by default. This
 * interceptor forwards the incoming {@code Authorization} JWT plus the gateway-issued
 * {@code X-Customer-Id}/{@code X-User-Email} headers so downstream services can
 * authenticate/authorize the same principal (PRD §6.2, §6.8).
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor identityPropagationInterceptor() {
        return template -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
                return;
            }
            HttpServletRequest request = servletAttrs.getRequest();
            forward(template, request, HttpHeaders.AUTHORIZATION);
            forward(template, request, WalletConstants.HEADER_CUSTOMER_ID);
            forward(template, request, WalletConstants.HEADER_USER_EMAIL);
        };
    }

    private void forward(feign.RequestTemplate template, HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value != null && !value.isBlank()) {
            template.header(header, value);
        }
    }
}
