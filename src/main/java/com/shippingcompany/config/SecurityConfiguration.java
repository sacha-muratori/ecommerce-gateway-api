package com.shippingcompany.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    private final Logger log = LogManager.getLogger(getClass());

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) throws Exception {
        http
                // NOTE, disabled as Docker Image should accept http requests
                .headers(headers -> headers.hsts(hsts -> hsts.disable()))

                // NOTE, Potentially have a csrf mechanism on the caller side to strengthen security
                .csrf((csrf) -> csrf.disable())

                // NOTE, Potentially have more front-end security with XSS, CSP, Frame Option DENY and Cache Control

                // NOTE, Potentially add CORS in case of particular origins
                .cors(cors -> cors.disable())

                .authorizeExchange(exchanges -> exchanges
                        // NOTE: opened for Rest Controller
                        .pathMatchers("/gateway**").permitAll()
                        .pathMatchers("/internal/**").permitAll()

                        // NOTE: opened for Internal and Prometheus Metrics
                        .pathMatchers("/actuator/**").permitAll()

                        // NOTE: Any other request is a deny all
                        .anyExchange().denyAll()
                );

        log.info("Initialized Spring Security Configuration");
        return http.build();
    }
}