package com.example.kwizi.config;

import com.example.kwizi.security.JwtRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
/*Этот класс настраивает Spring Security.
Он определяет, какие endpoint-ы требуют аутентификации, а какие - нет.
Он настраивает JwtRequestFilter для перехвата запросов и аутентификации на основе JWT.
Он настраивает AuthenticationManager для аутентификации пользователей.
Он указывает, что приложение должно использовать STATELESS сессии (т.е., не использовать сессии на стороне сервера).*/

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private UserDetailsService userDetailsService;


    private JwtRequestFilter jwtRequestFilter;


    @Autowired
    public SecurityConfig(UserDetailsService userDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        logger.info("AuthenticationProvider configured with UserDetailsService and PasswordEncoder"); // Логируем конфигурацию

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        AuthenticationManager authenticationManager = config.getAuthenticationManager();
        logger.info("AuthenticationManager configured successfully"); // Логируем инициализацию
        return authenticationManager;
    }
    // Отключаем CSRF для API, так как используем JWT в заголовках.
    // CSRF-атаки не применимы, когда аутентификация не на основе cookie.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Building SecurityFilterChain"); // Логируем начало построения
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/kafka/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/email-verification/verify-email").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("http://localhost:8080/websocket-test.html").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("SecurityFilterChain built successfully"); // Логируем успешное построение
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        logger.info("PasswordEncoder (BCryptPasswordEncoder) configured"); // Логируем конфигурацию
        return passwordEncoder;
    }
}