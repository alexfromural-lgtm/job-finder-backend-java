package com.jobfinder.service;

import com.jobfinder.domain.User;
import com.jobfinder.dto.request.RecruiterSignupRequest;
import com.jobfinder.dto.response.AuthResponse;
import com.jobfinder.dto.response.UserResponse;
import com.jobfinder.enums.Role;
import com.jobfinder.exception.ConflictException;
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
import com.jobfinder.exception.UnauthorizedException;
import com.jobfinder.repository.RecruiterProfileRepository;
import com.jobfinder.repository.UserRepository;
import com.jobfinder.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobfinder.domain.JobSeekerProfile;
import com.jobfinder.domain.RecruiterProfile;
import com.jobfinder.repository.JobSeekerProfileRepository;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/auth.service.ts.
 * All methods preserve the same business logic as the Node.js version.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository            userRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder           passwordEncoder;
    private final JwtService                jwtService;

    // -------------------------------------------------------------------
    // signupJobSeeker
    // -------------------------------------------------------------------

    @Transactional
    public AuthResponse signupJobSeeker(String name, String email, String password,
                                        HttpServletResponse response) {
        validateEmailPassword(email, password);
        checkEmailNotTaken(email);

        User user = User.builder()
            .name(name)
            .email(email)
            .password(passwordEncoder.encode(password))
            .roles(new ArrayList<>(List.of(Role.JOB_SEEKER)))
            .isActive(true)
            .build();
        user = userRepository.save(user);

        // Create empty JobSeekerProfile (matches Prisma jobSeeker: { create: {} })
        JobSeekerProfile profile = JobSeekerProfile.builder().user(user).build();
        jobSeekerProfileRepository.save(profile);

        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // signupRecruiter
    // -------------------------------------------------------------------

    @Transactional
    public AuthResponse signupRecruiter(RecruiterSignupRequest req, HttpServletResponse response) {
        validateEmailPassword(req.email(), req.password());
        checkEmailNotTaken(req.email());

        User user = User.builder()
            .name(req.name())
            .email(req.email())
            .password(passwordEncoder.encode(req.password()))
            .roles(new ArrayList<>(List.of(Role.RECRUITER)))
            .isActive(true)
            .build();
        user = userRepository.save(user);

        RecruiterProfile profile = RecruiterProfile.builder()
            .user(user)
            .companyName(req.companyName())
            .companyWebsite(req.companyWebsite())
            .description(req.description())
            .industry(req.industry())
            .build();
        recruiterProfileRepository.save(profile);

        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // upgradeToRecruiter
    // -------------------------------------------------------------------

    @Transactional
    public AuthResponse upgradeToRecruiter(UUID userId, RecruiterSignupRequest req,
                                            HttpServletResponse response) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getRoles().contains(Role.JOB_SEEKER)) {
            throw new ForbiddenException("Only Job Seekers can upgrade to Recruiter");
        }
        if (recruiterProfileRepository.findByUser_Id(userId).isPresent()) {
            throw new ConflictException("User already has a recruiter profile");
        }

        user.getRoles().add(Role.RECRUITER);
        user = userRepository.save(user);

        RecruiterProfile profile = RecruiterProfile.builder()
            .user(user)
            .companyName(req.companyName())
            .companyWebsite(req.companyWebsite())
            .description(req.description())
            .industry(req.industry())
            .build();
        recruiterProfileRepository.save(profile);

        // Re-issue tokens so the new RECRUITER role is reflected immediately
        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------

    public AuthResponse login(String email, String password, HttpServletResponse response) {
        validateEmailPassword(email, password);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new ForbiddenException("Account is deactivated");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password"); // same msg — avoids leaking
        }

        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // refreshTokens
    // -------------------------------------------------------------------

    public AuthResponse refreshTokens(String refreshToken, HttpServletResponse response) {
        Claims claims = jwtService.verifyRefreshToken(refreshToken);
        UUID userId = UUID.fromString(claims.getSubject());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new ForbiddenException("Account is deactivated");
        }

        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // getCurrentUser
    // -------------------------------------------------------------------

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRoles(),
            user.isActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getRoles());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        // response is null when called from gRPC (no cookie jar available)
        if (response != null) {
            jwtService.setTokenCookies(response, accessToken, refreshToken);
        }
        return new AuthResponse(user.getId(), user.getRoles());
    }

    private void validateEmailPassword(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Email is required");
        }
        if (password == null || password.length() < 6) {
            throw new UnauthorizedException("Password must be at least 6 characters");
        }
    }

    private void checkEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }
    }
}
