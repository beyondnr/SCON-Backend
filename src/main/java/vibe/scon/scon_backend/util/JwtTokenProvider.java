package vibe.scon.scon_backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티.
 * 
 * <p>Access Token과 Refresh Token을 생성하고 검증하는 기능을 제공합니다.</p>
 * 
 * <h3>요구사항 추적 (Traceability):</h3>
 * <ul>
 *   <li>{@code REQ-FUNC-001} - 3단계 온보딩 마법사 (JWT 토큰 발급)</li>
 *   <li>{@code Issue-003 §7.3} - JWT 스펙 (HS256, 30분/7일 만료)</li>
 * </ul>
 * 
 * <h3>JWT 스펙:</h3>
 * <ul>
 *   <li>알고리즘: HS256 (대칭키)</li>
 *   <li>Access Token 만료: 30분</li>
 *   <li>Refresh Token 만료: 7일</li>
 *   <li>Payload: sub (ownerId), email, iat, exp</li>
 * </ul>
 * 
 * @see <a href="tasks/github-issues/issue-003-REQ-FUNC-001-003.md">Issue-003 §7.3</a>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    /**
     * JwtTokenProvider 생성자.
     * 
     * <p>입력된 키를 SHA-256으로 해시하여 항상 32바이트(256비트) 키를 생성합니다.
     * 이렇게 하면 어떤 길이의 키를 입력해도 HS256에 적합한 키가 됩니다.</p>
     * 
     * @param secretKey 환경변수에서 주입받은 JWT 비밀키 (임의 길이)
     * @param accessTokenExpiration Access Token 만료 시간 (밀리초)
     * @param refreshTokenExpiration Refresh Token 만료 시간 (밀리초)
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secretKey,
            @Value("${app.jwt.access-expiration:1800000}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-expiration:604800000}") long refreshTokenExpiration) {
        
        try {
            // SHA-256 해시를 사용하여 항상 32바이트 키 생성
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
            this.accessTokenExpiration = accessTokenExpiration;
            this.refreshTokenExpiration = refreshTokenExpiration;
            
            log.info("JwtTokenProvider initialized (key derived via SHA-256). Access expiration: {}ms, Refresh expiration: {}ms",
                    accessTokenExpiration, refreshTokenExpiration);
                    
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Access Token 생성.
     * 
     * <p>사용자 인증 성공 시 발급되는 단기 토큰입니다.
     * 기본 만료 시간은 30분입니다.</p>
     * 
     * @param ownerId Owner의 고유 ID
     * @param email Owner의 이메일
     * @return 생성된 Access Token 문자열
     */
    public String generateAccessToken(Long ownerId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        String token = Jwts.builder()
                .subject(String.valueOf(ownerId))
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();

        log.debug("Access token generated for ownerId: {}, expires at: {}", ownerId, expiryDate);
        return token;
    }

    /**
     * Refresh Token 생성.
     * 
     * <p>Access Token 갱신을 위한 장기 토큰입니다.
     * 기본 만료 시간은 7일입니다.</p>
     * 
     * @param ownerId Owner의 고유 ID
     * @return 생성된 Refresh Token 문자열
     */
    public String generateRefreshToken(Long ownerId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        String token = Jwts.builder()
                .subject(String.valueOf(ownerId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();

        log.debug("Refresh token generated for ownerId: {}, expires at: {}", ownerId, expiryDate);
        return token;
    }

    /**
     * 토큰에서 Owner ID 추출.
     * 
     * @param token JWT 토큰
     * @return Owner ID
     */
    public Long getOwnerIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 토큰에서 이메일 추출.
     * 
     * @param token JWT 토큰
     * @return 이메일 (Access Token에만 포함)
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * 토큰 유효성 검증.
     * 
     * <p>토큰의 서명, 만료 시간, 형식을 검증합니다.</p>
     * 
     * @param token 검증할 JWT 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 타입 확인 (access/refresh).
     * 
     * @param token JWT 토큰
     * @return 토큰 타입 ("access" 또는 "refresh")
     */
    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * 토큰이 Access Token인지 확인.
     * 
     * @param token JWT 토큰
     * @return Access Token이면 true
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    /**
     * 토큰이 Refresh Token인지 확인.
     * 
     * @param token JWT 토큰
     * @return Refresh Token이면 true
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    /**
     * 토큰에서 Claims 파싱.
     * 
     * @param token JWT 토큰
     * @return 파싱된 Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
