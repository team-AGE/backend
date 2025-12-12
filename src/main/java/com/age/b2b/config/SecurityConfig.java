package com.age.b2b.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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
public class SecurityConfig {

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
                // 1. CSRF 설정 (개발 초기에는 disable, 운영 시 CookieCsrfTokenRepository 적용 권장)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 설정 (React 연동 필수)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/login").permitAll() // 로그인 경로는 누구나 접근
                        .requestMatchers("/api/admin/**").hasAnyRole("MASTER", "MANAGER") // 관리자 전용
                        .requestMatchers("/api/client/**").hasRole("CLIENT") // 고객사 전용
                        .anyRequest().authenticated() // 그 외는 로그인 필요
                )

                // 4. 로그인 설정 (React 친화적 JSON 응답)
                .formLogin(login -> login
                        .loginProcessingUrl("/api/login") // 프론트에서 이 주소로 POST 요청 (username, password)
                        .usernameParameter("username")
                        .passwordParameter("password")
                        // 성공 시 리다이렉트 대신 200 OK 리턴
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().print("{\"message\": \"Login Success\", \"role\": \"" + authentication.getAuthorities() + "\"}");
                        })
                        // 실패 시 리다이렉트 대신 401 Unauthorized 리턴
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().print("{\"message\": \"Login Failed\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8"); // 한글 처리를 위해 필수!

                            // Service에서 던진 예외 메시지 가져오기
                            String errorMessage = exception.getMessage();

                            // 만약 비밀번호가 틀린 경우 (BadCredentialsException) 메시지가 영어로 나올 수 있으므로 처리
                            if (errorMessage.equals("Bad credentials")) {
                                errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
                            }

                            // JSON 형태로 응답
                            response.getWriter().print("{\"message\": \"" + errorMessage + "\"}");
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
                        })
                )

                // 6. 예외 처리 (로그인 안 된 사용자가 접근 시 리다이렉트 막기)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                );

        return http.build();
    }

    // CORS 설정 빈 (React 포트 허용)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 주소 허용 (localhost:3000 등)
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 중요: 쿠키(세션)를 주고받으려면 true여야 함
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}