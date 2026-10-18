package com.maplehush.admin.modules.system.auth.config;

import com.maplehush.admin.modules.system.auth.service.ICurrentUserService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ICurrentUserService currentUsers,
            @Value("${app.docs.enabled:false}") boolean docsEnabled)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.addFilterBefore(new BearerAuthenticationFilter(currentUsers), UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                        BearerAuthenticationFilter.writeError(response, ApiException.invalidAccessToken()))
                .accessDeniedHandler((request, response, exception) ->
                        BearerAuthenticationFilter.writeError(response, ApiException.forbidden())));
        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers("/actuator/health", "/actuator/info").permitAll();
            for (RouteSecurityPolicy.Route route : RouteSecurityPolicy.ROUTES) {
                var matcher = authorize.requestMatchers(route.method(), route.path());
                if (route.access() == RouteSecurityPolicy.Access.PUBLIC) {
                    matcher.permitAll();
                } else {
                    matcher.authenticated();
                }
            }
            if (docsEnabled) {
                authorize.requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll();
            }
            authorize.anyRequest().denyAll();
        });
        return http.build();
    }
}
