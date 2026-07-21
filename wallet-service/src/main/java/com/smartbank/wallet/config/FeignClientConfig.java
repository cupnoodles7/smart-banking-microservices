package com.smartbank.wallet.config;

import com.smartbank.wallet.constants.WalletConstants;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

// Carries the caller's login token and identity headers along whenever the wallet calls another service.
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
