package vibe.scon.scon_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vibe.scon.scon_backend.dto.auth.LoginRequestDto;
import vibe.scon.scon_backend.dto.auth.RefreshTokenRequestDto;
import vibe.scon.scon_backend.dto.auth.SignupRequestDto;
import vibe.scon.scon_backend.dto.auth.TokenResponseDto;
import vibe.scon.scon_backend.entity.Owner;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.OwnerRepository;
import vibe.scon.scon_backend.util.JwtTokenProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-AUTH-001: 회원가입 성공</li>
 *   <li>TC-AUTH-002: 비밀번호 BCrypt 해시 검증</li>
 *   <li>TC-AUTH-003: 중복 이메일 가입 실패 (409)</li>
 *   <li>TC-AUTH-004: 로그인 성공 + JWT 발급</li>
 *   <li>TC-AUTH-005: 잘못된 비밀번호 로그인 실패 (401)</li>
 *   <li>TC-AUTH-006: 토큰 갱신 API 성공</li>
 *   <li>TC-AUTH-009: 만료된 Refresh Token 처리 (401)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private Owner testOwner;
    private SignupRequestDto signupRequest;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        // Owner 객체 생성 후 Reflection으로 id 설정
        testOwner = Owner.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .name("홍길동")
                .phone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(testOwner, "id", 1L);

        signupRequest = SignupRequestDto.builder()
                .email("test@example.com")
                .password("Password123!")
                .name("홍길동")
                .phone("010-1234-5678")
                .build();

        loginRequest = LoginRequestDto.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();
    }

    @Test
    @DisplayName("TC-AUTH-001: 회원가입 성공")
    void signup_success() {
        // Given
        when(ownerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(ownerRepository.save(any(Owner.class))).thenReturn(testOwner);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");

        // When
        TokenResponseDto response = authService.signup(signupRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(ownerRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(ownerRepository).save(any(Owner.class));
    }

    @Test
    @DisplayName("TC-AUTH-002: 회원가입 시 비밀번호 BCrypt 해시 처리")
    void signup_passwordIsEncoded() {
        // Given
        when(ownerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$hashedPassword");
        when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> {
            Owner saved = invocation.getArgument(0);
            // 저장되는 Owner의 비밀번호가 인코딩되었는지 확인
            assertThat(saved.getPassword()).isEqualTo("$2a$12$hashedPassword");
            return testOwner;
        });
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");

        // When
        authService.signup(signupRequest);

        // Then
        verify(passwordEncoder).encode("Password123!");
    }

    @Test
    @DisplayName("TC-AUTH-003: 중복 이메일 가입 실패")
    void signup_duplicateEmail_throwsException() {
        // Given
        when(ownerRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이미 사용 중인 이메일");

        verify(ownerRepository).existsByEmail("test@example.com");
        verify(ownerRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-AUTH-004: 로그인 성공 + JWT 발급")
    void login_success() {
        // Given
        when(ownerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testOwner));
        when(passwordEncoder.matches("Password123!", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com")).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refreshToken");

        // When
        TokenResponseDto response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("TC-AUTH-005: 잘못된 비밀번호 로그인 실패")
    void login_wrongPassword_throwsException() {
        // Given
        when(ownerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testOwner));
        when(passwordEncoder.matches("Password123!", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_emailNotFound_throwsException() {
        // Given
        when(ownerRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("등록되지 않은 이메일");
    }

    @Test
    @DisplayName("TC-AUTH-006: 토큰 갱신 성공")
    void refreshToken_success() {
        // Given
        String validRefreshToken = "validRefreshToken";
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken(validRefreshToken)
                .build();

        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getOwnerIdFromToken(validRefreshToken)).thenReturn(1L);
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(testOwner));
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com")).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("newRefreshToken");

        // When
        TokenResponseDto response = authService.refreshToken(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("TC-AUTH-009: 유효하지 않은 Refresh Token으로 갱신 실패")
    void refreshToken_invalidToken_throwsException() {
        // Given
        String invalidToken = "invalidRefreshToken";
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken(invalidToken)
                .build();

        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token");
    }

    @Test
    @DisplayName("Access Token으로 갱신 시도 시 실패")
    void refreshToken_withAccessToken_throwsException() {
        // Given
        String accessToken = "accessToken";
        RefreshTokenRequestDto request = RefreshTokenRequestDto.builder()
                .refreshToken(accessToken)
                .build();

        when(jwtTokenProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(accessToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Refresh Token이 아닙니다");
    }
}
