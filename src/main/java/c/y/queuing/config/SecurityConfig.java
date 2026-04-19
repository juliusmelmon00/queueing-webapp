package c.y.queuing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth

                // PUBLIC
                .requestMatchers("/", "/error", "/login", "/h2-console/**").permitAll()
                .requestMatchers("/entry", "/entry-denied").permitAll()
                .requestMatchers("/client/**").permitAll()
                .requestMatchers("/status", "/status/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/specific-transactions").permitAll()

                // ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // STAFF / OPERATOR AREA
                .requestMatchers("/dept/**").authenticated()
                .requestMatchers("/lobby/**").authenticated()

                // APIs
                .requestMatchers("/api/**").authenticated()

                // everything else
                .anyRequest().authenticated()
        )

        .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)
                .permitAll()
        )

        .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
        )

        .sessionManagement(session -> session
                .invalidSessionUrl("/login?expired")
        )

        .exceptionHandling(ex -> ex
                .accessDeniedPage("/login?denied")
        )

        .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**", "/client/**", "/h2-console/**")
        );

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}