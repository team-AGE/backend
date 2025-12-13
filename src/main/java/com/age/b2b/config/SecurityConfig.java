package com.age.b2b.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper; // JSON 변환을 위해 주입 (Spring 기본 Bean)

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/login").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("MASTER", "MANAGER")
                        .requestMatchers("/api/client/**").hasRole("CLIENT")
                        .anyRequest().authenticated()
                )

                // 4. 로그인 설정 (React 연동 핵심)
                .formLogin(login -> login
                        .loginProcessingUrl("/api/login")
                        .usernameParameter("username")
                        .passwordParameter("password")

                        // 성공 핸들러
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json;charset=UTF-8");

                            // 직접 문자열을 만드는 것보다 ObjectMapper를 쓰는 게 안전함
                            String jsonResponse = objectMapper.writeValueAsString(java.util.Map.of(
                                    "message", "Login Success",
                                    "role", authentication.getAuthorities().toString()
                            ));
                            response.getWriter().print(jsonResponse);
                        })

                        // 실패 핸들러 (중복 제거됨, 상세 로직만 유지)
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");

                            String errorMessage = "로그인 실패";

                            // 1단계에서 false를 리턴하면 여기서 이 예외들이 잡힘
                            if (exception instanceof DisabledException) {
                                errorMessage = "아직 승인 대기 중인 계정입니다. 관리자 승인을 기다려주세요.";
                            } else if (exception instanceof LockedException) {
                                errorMessage = "가입이 거절된 계정입니다. 관리자에게 문의하세요.";
                            } else if (exception instanceof BadCredentialsException) {
                                errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
                            }

                            // 프론트엔드가 'error' 키를 기다리고 있으므로 키 값을 'error'로 전송
                            String jsonResponse = objectMapper.writeValueAsString(java.util.Map.of(
                                    "error", errorMessage
                            ));
                            response.getWriter().print(jsonResponse);
                        })
                        .permitAll()
                )

                // 5. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().print("{\"message\": \"Logout Success\"}");
                        })
                )

                // 6. 인증 예외 처리 (403 Forbidden 등)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 리다이렉트 없이 401 에러 리턴
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().print("{\"message\": \"Unauthorized - 로그인이 필요합니다.\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // React 주소
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}