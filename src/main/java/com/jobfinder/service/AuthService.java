package com.jobfinder.service;

// Import our core domain entities
import com.jobfinder.domain.User;
import com.jobfinder.domain.JobSeekerProfile;
import com.jobfinder.domain.RecruiterProfile;
// Import our payload and response DTO records
import com.jobfinder.dto.request.RecruiterSignupRequest;
import com.jobfinder.dto.response.AuthResponse;
import com.jobfinder.dto.response.UserResponse;
// Import custom system roles
import com.jobfinder.enums.Role;
// Import custom exceptions mapping to client error shapes
import com.jobfinder.exception.ConflictException;
import com.jobfinder.exception.ForbiddenException;
import com.jobfinder.exception.ResourceNotFoundException;
import com.jobfinder.exception.UnauthorizedException;
// Import persistence repository interfaces
import com.jobfinder.repository.RecruiterProfileRepository;
import com.jobfinder.repository.UserRepository;
import com.jobfinder.repository.JobSeekerProfileRepository;
// Import helper service for token generation and cookie handling
import com.jobfinder.security.JwtService;
// Import standard JWT claims representation
import io.jsonwebtoken.Claims;
// Lombok annotations for generating constructors and loggers
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// BCrypt password encoder interface from Spring Security
import org.springframework.security.crypto.password.PasswordEncoder;
// Spring annotations to define service layer and transaction boundaries
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Servlet response class to write token cookies
import jakarta.servlet.http.HttpServletResponse;

// Standard Java collections and UUID types
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Replaces src/services/auth.service.ts.
 * All methods preserve the same business logic as the Node.js version.
 */
// Registers this class as a Spring Service bean
@Service
// Auto-generates constructor injecting final fields
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class AuthService {

    // Inject UserRepository bean
    private final UserRepository            userRepository;
    // Inject JobSeekerProfileRepository bean
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    // Inject RecruiterProfileRepository bean
    private final RecruiterProfileRepository recruiterProfileRepository;
    // Inject PasswordEncoder bean
    private final PasswordEncoder           passwordEncoder;
    // Inject JwtService bean
    private final JwtService                jwtService;

    // -------------------------------------------------------------------
    // signupJobSeeker
    // -------------------------------------------------------------------

    // Performs candidate registration in a database transaction context
    @Transactional
    public AuthResponse signupJobSeeker(String name, String email, String password,
                                        HttpServletResponse response) {
        // Run validations on the email structure and password length
        validateEmailPassword(email, password);
        // Ensure email isn't already registered
        checkEmailNotTaken(email);

        // Build User entity with candidate credentials and JOB_SEEKER role
        User user = User.builder()
            .name(name)
            .email(email)
            .password(passwordEncoder.encode(password))
            .roles(new ArrayList<>(List.of(Role.JOB_SEEKER)))
            .isActive(true)
            .build();
        // Persist the user to the database
        user = userRepository.save(user);

        // Create empty JobSeekerProfile associated with the user (matches Prisma jobSeeker: { create: {} })
        JobSeekerProfile profile = JobSeekerProfile.builder().user(user).build();
        // Persist the empty candidate profile to the database
        jobSeekerProfileRepository.save(profile);

        // Generate tokens, set cookie options on response, and return response DTO
        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // signupRecruiter
    // -------------------------------------------------------------------

    // Performs recruiter registration in a database transaction context
    @Transactional
    public AuthResponse signupRecruiter(RecruiterSignupRequest req, HttpServletResponse response) {
        // Run validations on email format and password length
        validateEmailPassword(req.email(), req.password());
        // Ensure email isn't already registered
        checkEmailNotTaken(req.email());

        // Build User entity with recruiter credentials and RECRUITER role
        User user = User.builder()
            .name(req.name())
            .email(req.email())
            .password(passwordEncoder.encode(req.password()))
            .roles(new ArrayList<>(List.of(Role.RECRUITER)))
            .isActive(true)
            .build();
        // Persist the user credentials to the database
        user = userRepository.save(user);

        // Build RecruiterProfile with company metadata and link it to the User entity
        RecruiterProfile profile = RecruiterProfile.builder()
            .user(user)
            .companyName(req.companyName())
            .companyWebsite(req.companyWebsite())
            .description(req.description())
            .industry(req.industry())
            .build();
        // Persist the recruiter profile to the database
        recruiterProfileRepository.save(profile);

        // Generate tokens, set cookie headers on response, and return response DTO
        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // upgradeToRecruiter
    // -------------------------------------------------------------------

    // Upgrades an existing job seeker account to recruiter status in a database transaction context
    @Transactional
    public AuthResponse upgradeToRecruiter(UUID userId, RecruiterSignupRequest req,
                                            HttpServletResponse response) {
        // Retrieve the user by ID or throw ResourceNotFoundException
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify that the user actually has the JOB_SEEKER role prior to upgrading
        if (!user.getRoles().contains(Role.JOB_SEEKER)) {
            throw new ForbiddenException("Only Job Seekers can upgrade to Recruiter");
        }
        // Throw ConflictException if a recruiter profile already exists for this user ID
        if (recruiterProfileRepository.findByUser_Id(userId).isPresent()) {
            throw new ConflictException("User already has a recruiter profile");
        }

        // Add the RECRUITER role to the user's role collection
        user.getRoles().add(Role.RECRUITER);
        // Persist the updated user roles list to the database
        user = userRepository.save(user);

        // Build the recruiter company profile linked to the user
        RecruiterProfile profile = RecruiterProfile.builder()
            .user(user)
            .companyName(req.companyName())
            .companyWebsite(req.companyWebsite())
            .description(req.description())
            .industry(req.industry())
            .build();
        // Persist the recruiter company profile to the database
        recruiterProfileRepository.save(profile);

        // Re-issue tokens so the new RECRUITER role is reflected immediately
        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------

    // Authenticates user credentials and issues session tokens
    public AuthResponse login(String email, String password, HttpServletResponse response) {
        // Validate login email format and password length requirements
        validateEmailPassword(email, password);

        // Retrieve the user from the database by email
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Verify that the user account is active/enabled
        if (!user.isActive()) {
            throw new ForbiddenException("Account is deactivated");
        }

        // Verify password match using the BCrypt encoder; throw generic 401 if validation fails
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password"); // same msg — avoids leaking
        }

        // Generate and set session tokens in HTTP cookies and return response DTO
        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // refreshTokens
    // -------------------------------------------------------------------

    // Refreshes the session access token using a valid refresh token cookie
    public AuthResponse refreshTokens(String refreshToken, HttpServletResponse response) {
        // Verify the refresh token signature and get claims payload
        Claims claims = jwtService.verifyRefreshToken(refreshToken);
        // Extract the user ID from the token subject
        UUID userId = UUID.fromString(claims.getSubject());

        // Retrieve user by ID or throw ResourceNotFoundException
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify that the user account is active/enabled
        if (!user.isActive()) {
            throw new ForbiddenException("Account is deactivated");
        }

        // Generate and set updated session tokens in cookies
        return issueTokens(user, response);
    }

    // -------------------------------------------------------------------
    // getCurrentUser
    // -------------------------------------------------------------------

    // Retrieves profile information of the currently authenticated user
    public UserResponse getCurrentUser(UUID userId) {
        // Find user by ID or throw ResourceNotFoundException
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Map domain user properties to the UserResponse record
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

    // Helper method generating access and refresh tokens, setting cookies on the response, and returning AuthResponse
    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getRoles());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        // response is null when called from gRPC (no cookie jar available)
        if (response != null) {
            jwtService.setTokenCookies(response, accessToken, refreshToken);
        }
        return new AuthResponse(user.getId(), user.getRoles());
    }

    // Validation helper for email presence and password length
    private void validateEmailPassword(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Email is required");
        }
        if (password == null || password.length() < 6) {
            throw new UnauthorizedException("Password must be at least 6 characters");
        }
    }

    // Validation helper to ensure email uniqueness on signup
    private void checkEmailNotTaken(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use");
        }
    }
}
