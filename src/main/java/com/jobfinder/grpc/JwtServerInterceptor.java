package com.jobfinder.grpc;

import com.jobfinder.enums.Role;
import com.jobfinder.security.JwtService;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
@GrpcGlobalServerInterceptor
@RequiredArgsConstructor
@Slf4j
public class JwtServerInterceptor implements ServerInterceptor {

    /** Metadata key for the Authorization header (lowercase, as per HTTP/2 spec). */
    static final Metadata.Key<String> AUTHORIZATION_KEY =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    /** Context key to propagate the authenticated UUID to service methods. */
    static final Context.Key<UUID> USER_ID_CTX_KEY =
        Context.key("userId");

    /** Context key to propagate roles to service methods. */
    static final Context.Key<List<Role>> ROLES_CTX_KEY =
        Context.key("roles");

    private final JwtService jwtService;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(AUTHORIZATION_KEY);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.verifyAccessToken(token);
                UUID userId = UUID.fromString(claims.getSubject());

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
                return Contexts.interceptCall(ctx, call, headers, next);

            } catch (Exception e) {
                log.debug("gRPC JWT validation failed: {}", e.getMessage());
                // Don't abort immediately — let the service decide if auth is required
            }
        }

        return next.startCall(call, headers);
    }
}
