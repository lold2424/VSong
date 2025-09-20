package com.VSong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${REDIRECT_BASE_URL:http://localhost:3000}")
    private String redirectBaseUrl;

    @PostConstruct
    public void logBaseUrl() {
        String envBaseUrl = System.getenv("REDIRECT_BASE_URL");
        String sysPropBaseUrl = System.getProperty("REDIRECT_BASE_URL");
        System.out.println("Environment REDIRECT_BASE_URL: " + envBaseUrl);
        System.out.println("System Property REDIRECT_BASE_URL: " + sysPropBaseUrl);
        System.out.println("Configured REDIRECT_BASE_URL: " + redirectBaseUrl);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/", "/login/**", "/oauth2/**", "/main/**", "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            String redirectUrl = getRedirectBaseUrl() + "?login=success";
                            response.sendRedirect(redirectUrl);
                        })
                        .failureHandler((request, response, exception) -> {
                            String redirectUrl = getRedirectBaseUrl() + "?login=failure";
                            response.sendRedirect(redirectUrl);
                        })
                );

        return http.build();
    }

    private String getRedirectBaseUrl() {
        return redirectBaseUrl;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://vsong.art", "https://www.vsong.art"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://vsong.art", "https://www.vsong.art"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
