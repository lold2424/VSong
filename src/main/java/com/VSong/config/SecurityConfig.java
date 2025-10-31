package com.VSong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.annotation.PostConstruct;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;


@Configuration
public class SecurityConfig {

    @Value("${baseUrl:http://localhost:5173}")
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/main/**", "/api/v1/vtubers/search").permitAll()
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/main/**").permitAll()
                        .requestMatchers("/api/login/userinfo").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> {
                    e.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), 
                        new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON)
                    );
                    e.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google"), 
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                    );
                })
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            new DefaultRedirectStrategy().sendRedirect(request, response, getRedirectBaseUrl() + "?login=success");
                        })
                        .failureHandler((request, response, exception) -> {
                            new DefaultRedirectStrategy().sendRedirect(request, response, getRedirectBaseUrl() + "?login=failure");
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
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "https://vsong.site","https://www.vsong.site"));
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
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "https://vsong.site", "https://www.vsong.site"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
