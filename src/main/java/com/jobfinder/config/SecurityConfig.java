package com.jobfinder.config;

// Import our custom security components
import com.jobfinder.security.CookieAuthFilter;
import com.jobfinder.security.UserDetailsServiceImpl;
// Lombok annotation to automatically generate a constructor for all final fields
import lombok.RequiredArgsConstructor;
// Spring configuration, values, and security imports
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
// Spring Web CORS configuration classes
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Standard Java collection utilities
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration.
 *
 * Replaces helmet + cors + auth middleware from the Node.js backend:
 *  - Stateless JWT via HTTP-only cookies (no session)
 *  - CORS configured per CORS_ORIGIN env var
 *  - Public endpoints: GET /api/jobs/all, GET /api/jobs/{id}, POST /api/auth/**
 *  - All others require authentication; fine-grained roles enforced via @PreAuthorize
 */
// Marks this as a configuration class
@Configuration
// Enables Spring Security web security support
@EnableWebSecurity
// Enables annotation-based security (like @PreAuthorize / @PostAuthorize) at the method level
@EnableMethodSecurity(prePostEnabled = true)
// Generates a constructor for final fields, allowing constructor-based dependency injection
@RequiredArgsConstructor
public class SecurityConfig {

    // Injecting the custom CookieAuthFilter that checks for JWTs in cookies on incoming requests
    private final CookieAuthFilter cookieAuthFilter;
    // Injecting our custom user details service implementation to load user records from DB
    private final UserDetailsServiceImpl userDetailsService;

    // Injects the allowed CORS origins configured in application properties (e.g. localhost, domain)
    @Value("${app.cors.origin}")
    private String corsOrigin;

    // Configures the security filter chain which processes all incoming HTTP requests
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disables CSRF (Cross-Site Request Forgery) protection as we are using custom stateless cookie authorization
            .csrf(AbstractHttpConfigurer::disable)
            // Configures CORS support using our custom configuration source defined below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Sets the session creation policy to STATELESS, so no session is created or maintained on the server
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Establishes URL-based request matching and authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints: allow all users to retrieve jobs list
                .requestMatchers(HttpMethod.GET,  "/api/jobs/all").permitAll()
                // Public endpoints: allow all users to retrieve a specific job detail
                .requestMatchers(HttpMethod.GET,  "/api/jobs/{id}").permitAll()
                // Public endpoints: allow signups, login, and authentication-related endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                // Public endpoints: allow tracking queue jobs status publicly
                .requestMatchers(HttpMethod.GET,  "/api/queue/job/**").permitAll()
                // Actuator — health & info are public (for Docker / load-balancer probes)
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                // Require successful authentication for all other API endpoints not explicitly matched above
                .anyRequest().authenticated()
            )
            // Registers our custom cookie-based authentication filter before the standard username/password authentication filter
            .addFilterBefore(cookieAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Builds and returns the configured security filter chain
        return http.build();
    }

    // Bean definition for configuring Cross-Origin Resource Sharing (CORS) rules
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Creates a new CorsConfiguration object to specify allowed parameters
        CorsConfiguration config = new CorsConfiguration();
        // If one or more origins are configured, splits by comma and adds them as allowed origins
        if (corsOrigin != null && !corsOrigin.isBlank()) {
            config.setAllowedOrigins(Arrays.stream(corsOrigin.split(","))
                .map(String::trim)
                .toList());
        }
        // Configures list of HTTP methods that are allowed for cross-origin requests
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Allows all request headers in cross-origin requests
        config.setAllowedHeaders(List.of("*"));
        // Allows including user credentials (such as cookies, authorization headers) in requests
        config.setAllowCredentials(true); // Required for cookies
        // Sets how long (in seconds) the preflight response should be cached by the browser
        config.setMaxAge(86400L);

        // Maps our CORS configuration rules to all application paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // Returns the configured CORS source
        return source;
    }

    // Configures the password encoder using BCrypt algorithm with a cost factor (strength) of 10
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // Configures the DaoAuthenticationProvider to use our custom userDetailsService and passwordEncoder
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Sets the service used to fetch user account details from the database
        provider.setUserDetailsService(userDetailsService);
        // Sets the encoder used to verify passwords during authentication
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Exposes the AuthenticationManager bean so it can be injected and used for manually authenticating users
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
