package com.jobfinder.security;

// Import our User domain entity
import com.jobfinder.domain.User;
// Import our custom exception for authorization failures
import com.jobfinder.exception.UnauthorizedException;
// Import our UserRepository to query credentials
import com.jobfinder.repository.UserRepository;
// Lombok annotation to generate dependency-injection constructor
import lombok.RequiredArgsConstructor;
// Spring Security classes to model authorities, user details, and exception handlers
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// Spring stereotype to register this class as a Service bean
import org.springframework.stereotype.Service;

// standard Java collections
import java.util.List;

/**
 * Loads a User from the database by email for Spring Security's
 * DaoAuthenticationProvider (used by the AuthenticationManager).
 *
 * The roles stored in user_roles are mapped to Spring GrantedAuthority
 * objects with the ROLE_ prefix so @PreAuthorize("hasRole('RECRUITER')")
 * works correctly.
 */
// Registers this class as a UserDetailsService provider bean
@Service
// Generates constructor injecting the UserRepository dependency
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    // Inject UserRepository bean
    private final UserRepository userRepository;

    // Resolves and maps database credentials/roles to Spring Security's UserDetails contract
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Retrieve the user from the database by email or throw UsernameNotFoundException if missing
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Throw UnauthorizedException if the user account has been deactivated
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        // Map the user's role list to SimpleGrantedAuthority instances with a "ROLE_" prefix
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();

        // Return a fully initialized Spring User object containing credentials and authorities
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)
            .build();
    }
}
