package com.example.thuviensach01.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment env;

    public SecurityConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");



        if (isDev) {
            // DEV: bỏ CSRF cho API để Postman/FE test POST/PUT/DELETE không cần token
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        } else {
            // PROD: bật CSRF với cookie XSRF-TOKEN (FE đọc được -> gửi header X-XSRF-TOKEN)
            http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        }

        http
                // Dùng session (cookie) như hiện tại
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ===== Phân quyền =====
                .authorizeHttpRequests(auth -> auth
                        // Ai cũng xem được danh sách sách (phục vụ trang chủ)
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()

                        // CRUD sách chỉ ADMIN
                        .requestMatchers("/api/books/**").hasRole("ADMIN")

                        // Mượn – trả: yêu cầu đăng nhập
                        .requestMatchers("/api/loans/**").authenticated()

                        // Auth endpoints (login/logout/me/register… do bạn định nghĩa)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Trang & static public
                        .requestMatchers("/", "/login", "/register", "/change-password", "/loans",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // Còn lại: yêu cầu đăng nhập
                        .anyRequest().authenticated()
                )

                // Không dùng formLogin mặc định (vì bạn có /api/auth/login JSON)
                .formLogin(form -> form.disable())

                // Logout: nếu dùng /api/auth/logout tự xử lý ở controller
                .logout(l -> {});

        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // Lưu SecurityContext vào HttpSession (đúng mô hình session)
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // BCrypt cho mật khẩu
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}