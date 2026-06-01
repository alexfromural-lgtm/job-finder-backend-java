package com.jobfinder.grpc;

import com.jobfinder.dto.response.AuthResponse;
import com.jobfinder.grpc.proto.*;
import com.jobfinder.security.JwtService;
import com.jobfinder.service.AuthService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

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
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;
    private final JwtService  jwtService;

    // -----------------------------------------------------------------------
    // Login — public
    // -----------------------------------------------------------------------

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

            List<String> roleNames = auth.roles().stream()
                .map(Enum::name)
                .toList();

            LoginResponse response = LoginResponse.newBuilder()
                .setUserId(auth.userId().toString())
                .addAllRoles(roleNames)
                .setAccessToken(accessToken)
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.UnauthorizedException e) {
            responseObserver.onError(
                Status.UNAUTHENTICATED.withDescription(e.getMessage()).asRuntimeException());
        } catch (com.jobfinder.exception.ForbiddenException e) {
            responseObserver.onError(
                Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC Login error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // -----------------------------------------------------------------------
    // GetCurrentUser — requires JWT
    // -----------------------------------------------------------------------

    @Override
    public void getCurrentUser(Empty request, StreamObserver<UserResponse> responseObserver) {
        UUID userId = JwtServerInterceptor.USER_ID_CTX_KEY.get();
        if (userId == null) {
            responseObserver.onError(
                Status.UNAUTHENTICATED
                    .withDescription("Authorization Bearer token required")
                    .asRuntimeException());
            return;
        }

        try {
            com.jobfinder.dto.response.UserResponse svcUser = authService.getCurrentUser(userId);

            List<String> roleNames = svcUser.roles().stream()
                .map(Enum::name)
                .toList();

            UserResponse response = UserResponse.newBuilder()
                .setUserId(svcUser.id().toString())
                .setName(svcUser.name())
                .setEmail(svcUser.email())
                .addAllRoles(roleNames)
                .setIsActive(svcUser.isActive())
                .setCreatedAt(svcUser.createdAt().toString())
                .setUpdatedAt(svcUser.updatedAt().toString())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (com.jobfinder.exception.ResourceNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC GetCurrentUser error", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }
}
