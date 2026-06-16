package com.jobfinder.grpc;

// Import custom system roles
import com.jobfinder.enums.Role;
// Import the stateless JWT utility helper service
import com.jobfinder.security.JwtService;
// Import gRPC interceptor, metadata, contexts, and callback components
import io.grpc.*;
import io.jsonwebtoken.Claims;
// Lombok constructor generation and logger injection
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Annotation declaring this class as a gRPC global server interceptor bean
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
// Spring Security authentication and authority models
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

// Standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

/**
 * gRPC server interceptor that validates JWT access tokens sent in
 * the "authorization" metadata header (standard Bearer scheme).
 *
 * Usage from a gRPC client (Postman / grpcurl):
 *   metadata key:   authorization
 *   metadata value: Bearer <access_token>
 *
 * The interceptor is registered globally via @GrpcGlobalServerInterceptor —
 * it runs for every RPC.  Unauthenticated RPCs (Login, ListJobs, GetJob,
 * GetJobStatus) simply proceed with no SecurityContext set; the service
 * implementations can choose to check it or not.
 *
 * Authenticated RPCs (GetCurrentUser, CreateJob, ApplyToJob) will throw
 * UNAUTHENTICATED / PERMISSION_DENIED Status if the token is missing/invalid
 * or the role is wrong — enforced inside the service implementations.
 */
// Registers this class as a global gRPC server interceptor bean
@GrpcGlobalServerInterceptor
// Generates standard constructor injecting dependencies
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class JwtServerInterceptor implements ServerInterceptor {

    /** Metadata key for the Authorization header (lowercase, as per HTTP/2 spec). */
    // Configures the key to lookup in request metadata headers (ASCII format)
    static final Metadata.Key<String> AUTHORIZATION_KEY =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /** Context key to propagate the authenticated UUID to service methods. */
    // Configures a gRPC context key to bind the parsed user ID to the request thread
    static final Context.Key<UUID> USER_ID_CTX_KEY =
        Context.key("userId");

    /** Context key to propagate roles to service methods. */
    // Configures a gRPC context key to bind the parsed user roles to the request thread
    static final Context.Key<List<Role>> ROLES_CTX_KEY =
        Context.key("roles");

    // Inject JwtService bean
    private final JwtService jwtService;

    // Intercepts inbound gRPC calls to extract and validate bearer tokens from metadata
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // Query metadata for the authorization key value
        String authHeader = headers.get(AUTHORIZATION_KEY);

        // Process token parsing if header starts with "Bearer " prefix
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Verify access token signature and get claims payload
                Claims claims = jwtService.verifyAccessToken(token);
                // Extract subject as user UUID
                UUID userId = UUID.fromString(claims.getSubject());

                // Parse user roles list from JWT claim
                @SuppressWarnings("unchecked")
                List<String> roleNames = claims.get("roles", List.class);
                List<Role> roles = roleNames == null
                    ? List.of()
                    : roleNames.stream().map(Role::valueOf).toList();

                // Populate Spring SecurityContext (consistent with REST filter)
                List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .toList();
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Also attach to gRPC Context for service-layer access
                Context ctx = Context.current()
                    .withValue(USER_ID_CTX_KEY, userId)
                    .withValue(ROLES_CTX_KEY, roles);

                log.debug("gRPC authenticated userId={} roles={}", userId, roles);
                // Execute downstream request using the populated gRPC Context
                return Contexts.interceptCall(ctx, call, headers, next);

            } catch (Exception e) {
                log.debug("gRPC JWT validation failed: {}", e.getMessage());
                // Don't abort immediately — let the service decide if auth is required
            }
        }

        // Proceed with request dispatching normally if no bearer token is present
        return next.startCall(call, headers);
    }
}
