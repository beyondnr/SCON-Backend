package vibe.scon.scon_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.scon.scon_backend.dto.auth.LoginRequestDto;
import vibe.scon.scon_backend.dto.auth.RefreshTokenRequestDto;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.auth.TokenResponseDto;
import vibe.scon.scon_backend.entity.Owner;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.OwnerRepository;
import vibe.scon.scon_backend.util.JwtTokenProvider;

/**
 * 인증 서비스.
 * 
 * <p>회원가입, 로그인, 토큰 갱신 등 인증 관련 비즈니스 로직을 처리합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (회원가입, 로그인)</li>
 *   <li>{@code AC-001} - 사장님 회원가입 및 JWT 발급</li>
 *   <li>{@code AC-002} - 사장님 로그인</li>
 *   <li>{@code Issue-003 §7.2} - 비밀번호 해시 (BCrypt)</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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
        if (ownerRepository.existsByEmail(request.getEmail())) {
            log.warn("Signup failed - email already exists: {}", request.getEmail());
            throw new BadRequestException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        // Owner 엔티티 생성 (비밀번호 BCrypt 해시 - Issue-003 §7.2)
        Owner owner = Owner.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
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
    public TokenResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // 이메일로 Owner 조회
        Owner owner = ownerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - email not found: {}", request.getEmail());
                    return new ResourceNotFoundException("등록되지 않은 이메일입니다: " + request.getEmail());
                });

        // 비밀번호 검증 (TC-AUTH-005)
        if (!passwordEncoder.matches(request.getPassword(), owner.getPassword())) {
            log.warn("Login failed - invalid password for email: {}", request.getEmail());
            throw new BadRequestException("비밀번호가 일치하지 않습니다");
        }

        log.info("Login successful. ownerId: {}, email: {}", owner.getId(), owner.getEmail());

        // JWT 토큰 발급
        return generateTokenResponse(owner);
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

        log.info("Token refresh successful. ownerId: {}", ownerId);

        // 새 토큰 발급
        return generateTokenResponse(owner);
    }

    /**
     * JWT 토큰 응답 생성.
     * 
     * @param owner Owner 엔티티
     * @return 토큰 응답 DTO
     */
    private TokenResponseDto generateTokenResponse(Owner owner) {
        String accessToken = jwtTokenProvider.generateAccessToken(owner.getId(), owner.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(owner.getId());

        return TokenResponseDto.builder()
                .ownerId(owner.getId())
                .email(owner.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800L)  // 30분 (초 단위)
                .build();
    }
}
