package vibe.scon.scon_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vibe.scon.scon_backend.dto.owner.OwnerProfileResponse;
import vibe.scon.scon_backend.dto.owner.UpdateOwnerProfileRequest;
import vibe.scon.scon_backend.entity.Owner;
import vibe.scon.scon_backend.exception.BadRequestException;
import vibe.scon.scon_backend.exception.ResourceNotFoundException;
import vibe.scon.scon_backend.repository.OwnerRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OwnerService 단위 테스트.
 * 
 * <h3>테스트 케이스 추적:</h3>
 * <ul>
 *   <li>TC-OWNER-001: 프로필 조회 성공</li>
 *   <li>TC-OWNER-002: 프로필 수정 성공 (name만)</li>
 *   <li>TC-OWNER-003: 프로필 수정 성공 (phone만)</li>
 *   <li>TC-OWNER-004: 프로필 수정 성공 (name과 phone 모두)</li>
 *   <li>TC-OWNER-005: name을 빈 문자열로 설정 시도 (400 BadRequest)</li>
 *   <li>TC-OWNER-006: phone을 null로 설정 (빈 문자열 전달)</li>
 *   <li>TC-OWNER-007: 존재하지 않는 사용자 조회 (404)</li>
 *   <li>TC-OWNER-008: 존재하지 않는 사용자 수정 (404)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerService 단위 테스트")
class OwnerServiceTest {

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private OwnerService ownerService;

    private Owner testOwner;
    private LocalDateTime testCreatedAt;
    private LocalDateTime testUpdatedAt;

    @BeforeEach
    void setUp() {
        testCreatedAt = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        testUpdatedAt = LocalDateTime.of(2026, 1, 2, 10, 0, 0);

        testOwner = Owner.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .name("홍길동")
                .phone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(testOwner, "id", 1L);
        ReflectionTestUtils.setField(testOwner, "createdAt", testCreatedAt);
        ReflectionTestUtils.setField(testOwner, "updatedAt", testUpdatedAt);
    }

    @Test
    @DisplayName("TC-OWNER-001: 프로필 조회 성공")
    void getCurrentOwnerProfile_success() {
        // Given
        Long ownerId = 1L;
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));

        // When
        OwnerProfileResponse response = ownerService.getCurrentOwnerProfile(ownerId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getPhone()).isEqualTo("010-1234-5678");
        assertThat(response.getCreatedAt()).isEqualTo(testCreatedAt);
        assertThat(response.getUpdatedAt()).isEqualTo(testUpdatedAt);

        verify(ownerRepository, times(1)).findById(ownerId);
    }

    @Test
    @DisplayName("TC-OWNER-007: 존재하지 않는 사용자 조회 (404)")
    void getCurrentOwnerProfile_notFound() {
        // Given
        Long ownerId = 999L;
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ownerService.getCurrentOwnerProfile(ownerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Owner")
                .hasMessageContaining("999");

        verify(ownerRepository, times(1)).findById(ownerId);
    }

    @Test
    @DisplayName("TC-OWNER-002: 프로필 수정 성공 (name만)")
    void updateOwnerProfile_nameOnly_success() {
        // Given
        Long ownerId = 1L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("홍길동 수정", null);
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));
        when(ownerRepository.save(any(Owner.class))).thenReturn(testOwner);

        // When
        OwnerProfileResponse response = ownerService.updateOwnerProfile(ownerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("홍길동 수정");
        assertThat(response.getPhone()).isEqualTo("010-1234-5678"); // 변경되지 않음

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, times(1)).save(testOwner);
    }

    @Test
    @DisplayName("TC-OWNER-003: 프로필 수정 성공 (phone만)")
    void updateOwnerProfile_phoneOnly_success() {
        // Given
        Long ownerId = 1L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest(null, "010-9876-5432");
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));
        when(ownerRepository.save(any(Owner.class))).thenReturn(testOwner);

        // When
        OwnerProfileResponse response = ownerService.updateOwnerProfile(ownerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("홍길동"); // 변경되지 않음
        assertThat(response.getPhone()).isEqualTo("010-9876-5432");

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, times(1)).save(testOwner);
    }

    @Test
    @DisplayName("TC-OWNER-004: 프로필 수정 성공 (name과 phone 모두)")
    void updateOwnerProfile_bothFields_success() {
        // Given
        Long ownerId = 1L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("홍길동 수정", "010-9876-5432");
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));
        when(ownerRepository.save(any(Owner.class))).thenReturn(testOwner);

        // When
        OwnerProfileResponse response = ownerService.updateOwnerProfile(ownerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("홍길동 수정");
        assertThat(response.getPhone()).isEqualTo("010-9876-5432");

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, times(1)).save(testOwner);
    }

    @Test
    @DisplayName("TC-OWNER-005: name을 빈 문자열로 설정 시도 (400 BadRequest)")
    void updateOwnerProfile_emptyName_throwsBadRequest() {
        // Given
        Long ownerId = 1L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("", null);
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));

        // When & Then
        assertThatThrownBy(() -> ownerService.updateOwnerProfile(ownerId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이름은 빈 문자열일 수 없습니다");

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, never()).save(any(Owner.class));
    }

    @Test
    @DisplayName("TC-OWNER-005: name을 공백만으로 설정 시도 (400 BadRequest)")
    void updateOwnerProfile_whitespaceName_throwsBadRequest() {
        // Given
        Long ownerId = 1L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("   ", null);
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));

        // When & Then
        assertThatThrownBy(() -> ownerService.updateOwnerProfile(ownerId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("이름은 빈 문자열일 수 없습니다");

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, never()).save(any(Owner.class));
    }

    @Test
    @DisplayName("TC-OWNER-006: phone을 null로 설정 (빈 문자열 전달)")
    void updateOwnerProfile_emptyPhone_setsNull() {
        // Given
        Long ownerId = 1L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest(null, "");
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.of(testOwner));
        when(ownerRepository.save(any(Owner.class))).thenReturn(testOwner);

        // When
        OwnerProfileResponse response = ownerService.updateOwnerProfile(ownerId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPhone()).isNull();

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, times(1)).save(testOwner);
    }

    @Test
    @DisplayName("TC-OWNER-008: 존재하지 않는 사용자 수정 (404)")
    void updateOwnerProfile_notFound() {
        // Given
        Long ownerId = 999L;
        UpdateOwnerProfileRequest request = new UpdateOwnerProfileRequest("홍길동 수정", null);
        when(ownerRepository.findById(ownerId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ownerService.updateOwnerProfile(ownerId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Owner")
                .hasMessageContaining("999");

        verify(ownerRepository, times(1)).findById(ownerId);
        verify(ownerRepository, never()).save(any(Owner.class));
    }
}

