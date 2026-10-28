package com.ecommerce.userservice.security.config;

import com.ecommerce.userservice.security.config.jwt.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter){
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors( cors ->
                        cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //Definimos mensajes para excepciones 401 Y 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (
                                        req,res,e
                                ) -> {
                                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write(
                                        "{\"status\":401," +
                                                "\"code\":\"Unauthorized\"," +
                                                "\"message\":\"Token ausente o inválido\"}");
                                }
                        )
                        .accessDeniedHandler(
                                (req, res, e
                                ) -> {
                                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write(
                                        "{\"status\":403,\"code\":\"Forbidden\"" +
                                                ",\"message\":\"No tienes permisos para acceder a este recurso\"}");
                        })
                )

                //Definimos Endpoints que no requieren Autenticación
                .authorizeHttpRequests(auth -> auth
                        //Cualquiera se puede logear
                    .requestMatchers(HttpMethod.POST,"/api/auth/login").permitAll()
                        //Cualquiera puede solicitar un refresh, pero debe enviar un refreshToken
                    .requestMatchers(HttpMethod.POST,"/api/auth/refresh").permitAll()
                        //Cualquiera puede consultar si un usuario está Autenticado
                    .requestMatchers(HttpMethod.GET, "/api/auth/users/*/authenticated").permitAll()
                        //Cualquiera puede crear un nuevo usuario (Crear una cuenta)
                    .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        //Cualquiera puede acceder a la documentacion de los endpoints
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        //El resto de Endpoints sí requieren Autenticación
                    .anyRequest()
                    .authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
	* Metodo para comenzar la autenticación de nuestras credenciales usando el token authenticate
	* Debe ser llamado en el service para comenzar el login
	*/
    @Bean
    public AuthenticationManager authenticationManager (AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Le damos permiso exacto al puerto de nuestro frontend
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));

        // Permitimos los métodos HTTP que vas a usar
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Permitimos las cabeceras clave (muy importante dejar pasar el "Authorization" para JWT)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // Por si se necesitan credenciales o cookies
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Aplicamos esta configuración a todos los endpoints (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
