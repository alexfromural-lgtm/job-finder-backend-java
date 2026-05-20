package com.jobfinder.security;

import com.jobfinder.domain.User;
import com.jobfinder.exception.UnauthorizedException;
import com.jobfinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads a User from the database by email for Spring Security's
 * DaoAuthenticationProvider (used by the AuthenticationManager).
 *
 * The roles stored in user_roles are mapped to Spring GrantedAuthority
 * objects with the ROLE_ prefix so @PreAuthorize("hasRole('RECRUITER')")
 * works correctly.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)
            .build();
    }
}
