package pe.edu.vg.API09.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Configuración de seguridad
// v1 sin protección, v2 con protección
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**").permitAll()      // v1 sin autenticación
                .requestMatchers("/api/v2/**").authenticated()  // v2 requiere autenticación
                .anyRequest().permitAll()
            )
            .addFilterBefore(new SimpleTokenAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
