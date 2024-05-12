package com.company.demo.rest.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Order(1)
public class SecurityConfig {

    protected static final String SELF_TEST_PATH = "/selfTest/**";
    protected static final String MONITORING_PATH = "/monitoring/**";
    protected static final String DOC_PATH = "/swagger-ui/**";
    protected static final String ENTRY_DOC_PATH = "/swagger-ui.html";
    protected static final String CMP_API_PATH = "/rest/**";

    @Bean
    @Autowired
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {
        http
                .authorizeHttpRequests(Customizer.withDefaults())
                .securityMatcher(SELF_TEST_PATH)
                .securityMatcher(MONITORING_PATH)
                .securityMatcher(ENTRY_DOC_PATH)
                .securityMatcher(DOC_PATH)
                .securityMatcher(CMP_API_PATH);

        return http.build();
    }
}
