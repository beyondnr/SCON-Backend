package vibe.scon.scon_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import vibe.scon.scon_backend.dto.auth.LoginRequestDto;
import vibe.scon.scon_backend.dto.auth.RefreshTokenRequestDto;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.auth.TokenResponseDto;
import vibe.scon.scon_backend.entity.Owner;
import vibe.scon.scon_backend.entity.RefreshToken;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.OwnerRepository;
import vibe.scon.scon_backend.repository.RefreshTokenRepository;
import vibe.scon.scon_backend.util.JwtTokenProvider;

/**
 * 인증 서비스.
 * 
 * <p>회원가입, 로그인, 토큰 갱신, 로그아웃 등 인증 관련 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입, 로그인)</li>
 *   <li>{@code AC-001} - 사장님 회원가입 및 JWT 발급</li>
 *   <li>{@code AC-002} - 사장님 로그인</li>
 *   <li>{@code POC-BE-FUNC-003} - 로그아웃 API 구현</li>
 *   <li>{@code Issue-003 §7.2} - 비밀번호 해시 (BCrypt)</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003</a>
 * @see <a href="../../SCON-Update-Plan/POC-BE-FUNC-003-logout.md">POC-BE-FUNC-003</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OwnerRepository ownerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 일반적인 비밀번호 목록 (POC-BE-SEC-002: 비밀번호 정책 강화).
     * 
     * <p>보안을 위해 일반적으로 사용되는 비밀번호를 차단합니다.
     * 실제 운영 환경에서는 파일이나 DB에서 로드하는 것을 권장합니다.</p>
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "12345678", "qwerty123", "admin123", "Password123!",
            "password123", "123456789", "1234567890", "qwerty", "abc123",
            "monkey", "1234567", "letmein", "trustno1", "dragon",
            "baseball", "iloveyou", "master", "sunshine", "ashley"
    );

    /**
     * 회원가입.
     * 
     * <p>새로운 사장님 계정을 생성하고 JWT 토큰을 발급합니다.</p>
     * 
     * <h4>AC-001 검증:</h4>
     * <ul>
     *   <li>Owner 엔티티가 생성되어야 한다</li>
     *   <li>비밀번호는 BCrypt로 해시되어 저장되어야 한다</li>
     *   <li>JWT Access Token과 Refresh Token이 발급되어야 한다</li>
     * </ul>
     * 
     * @param request 회원가입 요청 DTO
     * @return 토큰 응답 DTO
     * @throws BadRequestException 이메일이 이미 존재하는 경우 (TC-AUTH-003)
     */
    @Transactional
    public TokenResponseDto signup(SignupRequestDto request) {
        log.info("Signup attempt for email: {}", request.getEmail());

        // 이메일 중복 체크 (TC-AUTH-003)
        // POC-BE-SEC-002: 이메일 노출 방지 - 에러 메시지에서 이메일 제거
        if (ownerRepository.existsByEmail(request.getEmail())) {
            log.warn("Signup failed - email already exists: {}", request.getEmail());
            throw new BadRequestException("이미 사용 중인 이메일입니다");
        }

        // POC-BE-SEC-002: 비밀번호 강도 검증
        validatePasswordStrength(request.getPassword());

        // Owner 엔티티 생성 (비밀번호 BCrypt 해시 - Issue-003 §7.2)
        Owner owner = Owner.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .agreedToTerms(request.getIsAgreedToTerms())
                .agreedAt(LocalDateTime.now())
                .build();

        Owner savedOwner = ownerRepository.save(owner);
        log.info("Owner created successfully. ownerId: {}, email: {}", savedOwner.getId(), savedOwner.getEmail());

        // JWT 토큰 발급
        return generateTokenResponse(savedOwner);
    }

    /**
     * 로그인.
     * 
     * <p>이메일과 비밀번호를 검증하고 JWT 토큰을 발급합니다.</p>
     * 
     * <h4>AC-002 검증:</h4>
     * <ul>
     *   <li>JWT Access Token (30분 만료)이 발급되어야 한다</li>
     *   <li>JWT Refresh Token (7일 만료)이 발급되어야 한다</li>
     *   <li>응답에 ownerId가 포함되어야 한다</li>
     * </ul>
     * 
     * @param request 로그인 요청 DTO
     * @return 토큰 응답 DTO
     * @throws ResourceNotFoundException 이메일이 존재하지 않는 경우
     * @throws BadRequestException 비밀번호가 일치하지 않는 경우 (TC-AUTH-005)
     */
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            // 이메일로 Owner 조회
            // POC-BE-SEC-002: 이메일 존재 여부 노출 방지 - 동일한 메시지 반환
            Owner owner = ownerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Login failed - email not found: {}", request.getEmail());
                        // 이메일 존재 여부와 관계없이 동일한 메시지 반환
                        return new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
                    });

            log.debug("Owner found: id={}, email={}", owner.getId(), owner.getEmail());

            // POC-BE-SEC-002: 계정 잠금 확인
            if (owner.isLocked()) {
                long minutesRemaining = ChronoUnit.MINUTES.between(
                        LocalDateTime.now(),
                        owner.getLockedUntil()
                );
                log.warn("Login attempt blocked - account locked. email: {}, minutes remaining: {}", 
                        request.getEmail(), minutesRemaining);
                throw new BadRequestException(
                        String.format("계정이 잠겨있습니다. %d분 후 다시 시도해주세요.", minutesRemaining)
                );
            }

            // 비밀번호 검증 (TC-AUTH-005)
            // POC-BE-SEC-002: 이메일 존재 여부 노출 방지 - 동일한 메시지 반환
            if (!passwordEncoder.matches(request.getPassword(), owner.getPassword())) {
                owner.incrementFailedAttempts();
                
                // 5회 실패 시 30분 잠금
                if (owner.getFailedLoginAttempts() >= 5) {
                    owner.lockAccount(30);
                    log.warn("Account locked due to too many failed attempts. email: {}, ownerId: {}", 
                            request.getEmail(), owner.getId());
                }
                
                ownerRepository.save(owner);
                log.warn("Login failed - invalid password for email: {}", request.getEmail());
                throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다");
            }

            // 성공 시 실패 횟수 리셋
            owner.resetFailedAttempts();
            ownerRepository.save(owner);

            log.info("Login successful. ownerId: {}, email: {}", owner.getId(), owner.getEmail());

            // JWT 토큰 발급
            TokenResponseDto response = generateTokenResponse(owner);
            log.debug("Token response generated successfully for ownerId: {}", owner.getId());
            return response;
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for email: {}", request.getEmail(), e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 토큰 갱신.
     * 
     * <p>유효한 Refresh Token으로 새로운 Access Token을 발급합니다.</p>
     * 
     * <h4>TC-AUTH-006, TC-AUTH-009 검증:</h4>
     * <ul>
     *   <li>유효한 Refresh Token으로 토큰 갱신 성공</li>
     *   <li>만료된 Refresh Token은 401 반환</li>
     * </ul>
     * 
     * @param request 토큰 갱신 요청 DTO
     * @return 토큰 응답 DTO
     * @throws BadRequestException Refresh Token이 유효하지 않은 경우
     */
    @Transactional
    public TokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();
        log.debug("Token refresh attempt");

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Token refresh failed - invalid refresh token");
            throw new BadRequestException("유효하지 않은 Refresh Token입니다");
        }

        // Refresh Token 타입 검증
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("Token refresh failed - not a refresh token");
            throw new BadRequestException("Refresh Token이 아닙니다");
        }

        // Owner ID 추출 및 조회
        Long ownerId = jwtTokenProvider.getOwnerIdFromToken(refreshToken);
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.error("Token refresh failed - owner not found. ownerId: {}", ownerId);
                    return new ResourceNotFoundException("사용자를 찾을 수 없습니다");
                });

        // DB에서 Refresh Token 존재 여부 확인 (로그아웃된 토큰인지 확인)
        String tokenHash = generateTokenHash(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> {
                    // POC-BE-SEC-002: Refresh Token 재사용 탐지
                    // 토큰이 DB에 없으면 재사용 가능성 - 모든 Refresh Token 무효화
                    log.warn("Refresh token reuse detected - token not found in DB. ownerId: {}", ownerId);
                    refreshTokenRepository.deleteByOwnerId(ownerId);
                    return new BadRequestException("유효하지 않은 토큰입니다");
                });

        // POC-BE-SEC-002: 토큰이 이미 사용되었는지 확인 (재사용 탐지)
        if (storedToken.isUsed()) {
            log.warn("Refresh token reuse detected - token already used. ownerId: {}", ownerId);
            // 보안 이벤트: 모든 Refresh Token 무효화
            refreshTokenRepository.deleteByOwnerId(ownerId);
            throw new BadRequestException("유효하지 않은 토큰입니다");
        }

        // 만료된 토큰인지 확인
        if (storedToken.isExpired()) {
            log.warn("Token refresh failed - expired token. ownerId: {}", ownerId);
            refreshTokenRepository.delete(storedToken);
            throw new BadRequestException("만료된 Refresh Token입니다");
        }

        log.info("Token refresh successful. ownerId: {}", ownerId);

        // POC-BE-SEC-002: 토큰 사용 표시 (재사용 방지)
        storedToken.markAsUsed();
        refreshTokenRepository.save(storedToken);
        entityManager.flush(); // 저장을 즉시 반영

        // 새 토큰 발급
        return generateTokenResponse(owner);
    }

    /**
     * 로그아웃.
     * 
     * <p>Refresh Token을 무효화하여 로그아웃을 처리합니다.
     * DB에서 해당 Refresh Token을 삭제합니다.</p>
     * 
     * <h4>TC-AUTH-010 검증:</h4>
     * <ul>
     *   <li>유효한 Refresh Token으로 로그아웃 성공</li>
     *   <li>DB에서 Refresh Token 삭제 확인</li>
     * </ul>
     * 
     * <h4>TC-AUTH-011 검증:</h4>
     * <ul>
     *   <li>로그아웃 후 같은 Refresh Token으로 갱신 시도 실패</li>
     * </ul>
     * 
     * @param request 로그아웃 요청 DTO
     * @throws BadRequestException Refresh Token이 유효하지 않은 경우
     * @throws ResourceNotFoundException Refresh Token이 DB에 존재하지 않는 경우
     */
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();
        log.info("Logout attempt");

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Logout failed - invalid refresh token");
            throw new BadRequestException("유효하지 않은 Refresh Token입니다");
        }

        // Refresh Token 타입 검증
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("Logout failed - not a refresh token");
            throw new BadRequestException("Refresh Token이 아닙니다");
        }

        // 토큰 해시 생성 및 DB에서 조회
        String tokenHash = generateTokenHash(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Logout failed - token not found in DB (already logged out)");
                    return new ResourceNotFoundException("이미 로그아웃된 Refresh Token입니다");
                });

        // DB에서 토큰 삭제
        refreshTokenRepository.delete(storedToken);
        log.info("Logout successful. ownerId: {}, token deleted", storedToken.getOwnerId());
    }

    /**
     * JWT 토큰 응답 생성.
     * 
     * @param owner Owner 엔티티
     * @return 토큰 응답 DTO
     */
    private TokenResponseDto generateTokenResponse(Owner owner) {
        try {
            Long ownerId = owner.getId();
            String email = owner.getEmail();
            
            if (ownerId == null) {
                log.error("Owner ID is null for email: {}", email);
                throw new RuntimeException("Owner ID가 null입니다");
            }
            if (email == null || email.isEmpty()) {
                log.error("Owner email is null or empty for ownerId: {}", ownerId);
                throw new RuntimeException("Owner email이 null이거나 비어있습니다");
            }
            
            log.debug("Generating tokens for ownerId: {}, email: {}", ownerId, email);
            
            String accessToken = jwtTokenProvider.generateAccessToken(ownerId, email);
            String refreshToken = jwtTokenProvider.generateRefreshToken(ownerId);
            
            // Refresh Token 만료 시간 추출
            LocalDateTime expiresAt = jwtTokenProvider.getExpirationDateFromToken(refreshToken);
            
            // Refresh Token 해시 생성 및 DB 저장
            String tokenHash = generateTokenHash(refreshToken);
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .token(tokenHash)
                    .ownerId(ownerId)
                    .expiresAt(expiresAt)
                    .build();
            refreshTokenRepository.saveAndFlush(refreshTokenEntity);
            
            log.debug("Tokens generated and saved successfully for ownerId: {}", ownerId);

            return TokenResponseDto.builder()
                    .ownerId(ownerId)
                    .email(email)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(1800L)  // 30분 (초 단위)
                    .build();
        } catch (Exception e) {
            log.error("Error generating token response for ownerId: {}, email: {}", 
                    owner.getId(), owner.getEmail(), e);
            throw new RuntimeException("토큰 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 비밀번호 강도 검증.
     * 
     * <p>비밀번호가 정규식 패턴을 만족하고 일반적인 비밀번호가 아닌지 확인합니다.</p>
     * 
     * <h3>요구사항 추적 (Traceability):</h3>
     * <ul>
     *   <li>{@code POC-BE-SEC-002} - 백엔드 보안 강화 (비밀번호 정책 강화)</li>
     * </ul>
     * 
     * @param password 검증할 비밀번호
     * @throws BadRequestException 비밀번호가 정규식 패턴을 만족하지 않거나 일반적인 비밀번호인 경우
     * @see <a href="../../SCON-Update-Plan/POC-BE-SEC-002.md">POC-BE-SEC-002</a>
     */
    private void validatePasswordStrength(String password) {
        // 기존 정규식 검증 (SignupRequestDto의 @Pattern과 동일)
        Pattern pattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$");
        if (!pattern.matcher(password).matches()) {
            throw new BadRequestException("비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다");
        }
        
        // 일반적인 비밀번호 차단
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            throw new BadRequestException("너무 일반적인 비밀번호는 사용할 수 없습니다");
        }
    }

    /**
     * Refresh Token 해시 생성.
     * 
     * <p>보안을 위해 Refresh Token을 SHA-256으로 해시하여 저장합니다.
     * DB에 평문 토큰을 저장하지 않아 탈취 시에도 안전합니다.</p>
     * 
     * @param token Refresh Token (평문)
     * @return 토큰 해시값 (Base64 인코딩)
     */
    private String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
