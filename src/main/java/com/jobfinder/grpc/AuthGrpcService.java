package com.jobfinder.grpc;

// Import our authentication response DTO
import com.jobfinder.dto.response.AuthResponse;
// Import the generated gRPC classes from proto specifications
import com.jobfinder.grpc.proto.*;
// Import security helpers
import com.jobfinder.security.JwtService;
import com.jobfinder.service.AuthService;
// Import gRPC status codes and StreamObserver callback interfaces
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
// Lombok constructor generation and logger injection
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Annotation declaring this class as a gRPC global service bean
import net.devh.boot.grpc.server.service.GrpcService;

// Standard Java collections and unique identifiers
import java.util.List;
import java.util.UUID;

/**
 * gRPC implementation of AuthService (auth.proto).
 *
 * RPCs:
 *   Login           — public (no auth required)
 *   GetCurrentUser  — requires Authorization Bearer JWT in metadata
 *
 * Note: gRPC has no cookie jar, so Login returns the access token
 * directly in the response body.  The caller must send it as:
 *   metadata key:   authorization
 *   metadata value: Bearer <access_token>
 */
// Registers this class as a gRPC service bean
@GrpcService
// Generates standard constructor injecting dependencies
@RequiredArgsConstructor
// Auto-injects log instance
@Slf4j
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    // Inject AuthService bean
    private final AuthService authService;
    // Inject JwtService bean
    private final JwtService  jwtService;

    // -----------------------------------------------------------------------
    // Login — public
    // -----------------------------------------------------------------------

    // Handles the login RPC request by validating credentials and returning the user data with a raw JWT
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            // Delegate to existing service — pass null for HttpServletResponse
            // because gRPC doesn't use cookies; we return the token in the body instead.
            AuthResponse auth = authService.login(request.getEmail(), request.getPassword(), null);

            // Generate a fresh access token to include in gRPC response body
            // (AuthService already generated one for cookies; we generate it again here
            //  so we can return it — the JwtService is stateless so this is safe)
            String accessToken = jwtService.generateAccessToken(
                auth.userId(),
                auth.roles()
            );

            // Extract role string names list
            List<String> roleNames = auth.roles().stream()
                .map(Enum::name)
                .toList();

            // Construct the protobuf LoginResponse containing user credentials and JWT
            LoginResponse response = LoginResponse.newBuilder()
                .setUserId(auth.userId().toString())
                .addAllRoles(roleNames)
                .setAccessToken(accessToken)
                .build();

            // Send response back to the client
            responseObserver.onNext(response);
            // Complete the stream pipeline
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.UnauthorizedException e) {
            // Return gRPC UNAUTHENTICATED error status
            responseObserver.onError(
                Status.UNAUTHENTICATED.withDescription(e.getMessage()).asRuntimeException());
        } catch (com.jobfinder.exception.ForbiddenException e) {
            // Return gRPC PERMISSION_DENIED error status
            responseObserver.onError(
                Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC Login error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // GetCurrentUser — requires JWT
    // -----------------------------------------------------------------------

    // Resolves and returns details of the currently authenticated gRPC caller
    @Override
    public void getCurrentUser(Empty request, StreamObserver<UserResponse> responseObserver) {
        // Retrieve the authenticated user ID from the gRPC Context (populated by interceptor)
        UUID userId = JwtServerInterceptor.USER_ID_CTX_KEY.get();
        // Throw UNAUTHENTICATED error status if no user is found in the current context thread
        if (userId == null) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Authorization Bearer token required")
                    .asRuntimeException());
            return;
        }

        try {
            // Query the core AuthService for user information
            com.jobfinder.dto.response.UserResponse svcUser = authService.getCurrentUser(userId);

            // Map roles to string values
            List<String> roleNames = svcUser.roles().stream()
                .map(Enum::name)
                .toList();

            // Construct the protobuf UserResponse metadata
            UserResponse response = UserResponse.newBuilder()
                .setUserId(svcUser.id().toString())
                .setName(svcUser.name())
                .setEmail(svcUser.email())
                .addAllRoles(roleNames)
                .setIsActive(svcUser.isActive())
                .setCreatedAt(svcUser.createdAt().toString())
                .setUpdatedAt(svcUser.updatedAt().toString())
                .build();

            // Return user details to gRPC client
            responseObserver.onNext(response);
            // Complete the stream pipeline
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            // Return gRPC NOT_FOUND error status
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetCurrentUser error", e);
            // Return generic gRPC INTERNAL error status
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }
}
