package com.bancoxyz.auth_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .oauth2AuthorizationServer(
                        authorizationServer -> {
                            http.securityMatcher(
                                    authorizationServer.getEndpointsMatcher()
                            );

                            authorizationServer
                                    .oidc(Customizer.withDefaults());
                        }
                )
                .authorizeHttpRequests(
                        authorize -> authorize
                                .anyRequest()
                                .authenticated()
                )
                .exceptionHandling(
                        exceptions -> exceptions
                                .defaultAuthenticationEntryPointFor(
                                        new LoginUrlAuthenticationEntryPoint(
                                                "/login"
                                        ),
                                        request -> true
                                )
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(
                        authorize -> authorize
                                .anyRequest()
                                .authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }
}