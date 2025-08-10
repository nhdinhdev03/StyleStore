package com.shopethethao.auth.security.jwt.util;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import com.shopethethao.auth.security.oauth2google.OAuth2UserPrincipal;
import com.shopethethao.auth.security.token.TokenManager;
import com.shopethethao.auth.security.user.entity.UserDetailsImpl;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${bezkoder.app.jwtSecret}")
    private String jwtSecret;

    @Value("${bezkoder.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${bezkoder.app.jwtClockSkew:60}")
    private long clockSkewSeconds;

    @Autowired
    private TokenManager tokenManager;

    @PostConstruct
    public void init() {
        logger.info("Khởi tạo JwtUtils và kiểm tra khóa bí mật");
        validateSecretKey(jwtSecret);
    }

    // Increase token lifetime - consider adding this to application.properties instead
    @PostConstruct
    private void setupLongerTokenLifetime() {
        // Setting token expiration to 7 days for better user experience during development
        jwtExpirationMs = Math.max(jwtExpirationMs, 7 * 24 * 60 * 60 * 1000);
        logger.info("JWT token lifetime set to {} days", jwtExpirationMs / (24 * 60 * 60 * 1000));
    }

    // Tạo JWT
    public String generateJwtToken(Authentication authentication) {
        String username;
        
        // Handle different types of Authentication principals
        if (authentication.getPrincipal() instanceof UserDetailsImpl userPrincipal) {
            username = userPrincipal.getUsername();
        } else if (authentication.getPrincipal() instanceof OAuth2UserPrincipal oauth2Principal) {
            username = oauth2Principal.getName();
        } else if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            // Handle standard OAuth2User objects including DefaultOidcUser
            Map<String, Object> attributes = oauth2User.getAttributes();
            // Extract sub claim which contains the unique ID
            username = (String) attributes.getOrDefault("sub", 
                       attributes.getOrDefault("email", 
                       oauth2User.getName()));
        } else {
            username = authentication.getName();
        }

        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    // Mã hóa
    private Key key() {
        try {
            logger.debug("Đang tạo khóa từ JWT secret");
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            logger.debug("Độ dài khóa (byte): {}", keyBytes.length);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            logger.error("Không thể tạo khóa từ JWT secret: {}", e.getMessage());
            throw new SecurityException("Tạo khóa thất bại", e);
        }
    }

    private void validateSecretKey(String secret) {
        if (secret == null || secret.length() < 32) {
            logger.error("Khóa bí mật JWT không đủ độ dài (tối thiểu 256 bits / 32 bytes)");
            throw new SecurityException("Khóa bí mật JWT không đảm bảo an toàn");
        }

        try {
            Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            logger.error("Khóa bí mật JWT không phải định dạng Base64 hợp lệ");
            throw new SecurityException("Khóa bí mật JWT không hợp lệ", e);
        }
    }

    // Giải mã username từ JWT
    public String getUserNameFromJwtToken(String token) {
        logger.debug("Đang trích xuất tên người dùng từ JWT");
        try {
            String username = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            logger.debug("Tên người dùng đã trích xuất thành công: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Lỗi khi trích xuất tên người dùng từ token: {}", e.getMessage());
            throw e;
        }
    }

    // Xác thực token và trả về thông tin chi tiết về trạng thái token
    public Map<String, Object> validateJwtTokenWithDetails(String authToken) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("requireLogin", false);
        
        try {
            logger.debug("Bắt đầu kiểm tra tính hợp lệ của JWT");

            var parser = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .setAllowedClockSkewSeconds(clockSkewSeconds)
                    .build();

            var claims = parser.parseClaimsJws(authToken);
            String userId = claims.getBody().getSubject();
            result.put("userId", userId);

            logger.debug("Token hợp lệ cho người dùng: {}", userId);

            // Kiểm tra token trong store
            String storedToken = tokenManager.getToken(userId);
            if (storedToken == null) {
                // Token không có trong store
                Date expiration = claims.getBody().getExpiration();
                if (expiration != null && expiration.after(new Date())) {
                    logger.info("Valid token found for user {} but not in store. Re-registering.", userId);
                    tokenManager.saveToken(userId, authToken, expiration.getTime());
                    result.put("valid", true);
                    return result;
                }
                logger.warn("Token not found in store for user: {}", userId);
                result.put("requireLogin", true);
                result.put("message", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
                return result;
            }
            
            Date expiration = claims.getBody().getExpiration();
            Date now = new Date();
            long skewMillis = clockSkewSeconds * 1000;

            logger.debug("Kiểm tra thời gian hết hạn token - Hiện tại: {}, Hết hạn: {}, Sai số: {} ms",
                    now, expiration, skewMillis);

            if (expiration.before(new Date(now.getTime() - skewMillis))) {
                logger.warn("Token đã hết hạn cho người dùng: {}. Hết hạn vào: {}", userId, expiration);
                tokenManager.removeToken(userId);
                result.put("requireLogin", true);
                result.put("message", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
                return result;
            }

            logger.info("Token hợp lệ cho người dùng: {}. Thời gian còn lại: {} ms",
                    userId, (expiration.getTime() - now.getTime()));
            result.put("valid", true);
            return result;

        } catch (ExpiredJwtException e) {
            logger.error("JWT đã hết hạn: {}", e.getMessage());
            try {
                String userId = e.getClaims().getSubject();
                tokenManager.removeToken(userId);
                result.put("userId", userId);
            } catch (Exception ex) {
                logger.error("Lỗi khi xóa token hết hạn: {}", ex.getMessage());
            }
            result.put("requireLogin", true);
            result.put("message", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
            return result;
        } catch (Exception e) {
            logger.error("Lỗi xác thực JWT: {}", e.getMessage());
            result.put("requireLogin", true);
            result.put("message", "Lỗi xác thực, vui lòng đăng nhập lại");
            return result;
        }
    }

    // Xác thực token (phương thức ban đầu giữ nguyên để tương thích)
    public boolean validateJwtToken(String authToken) {
        Map<String, Object> details = validateJwtTokenWithDetails(authToken);
        return (boolean) details.get("valid");
    }

    // Xác thực token
    public boolean validateJwtTokenOld(String authToken) {
        try {
            logger.debug("Bắt đầu kiểm tra tính hợp lệ của JWT");

            var parser = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .setAllowedClockSkewSeconds(clockSkewSeconds)
                    .build();

            var claims = parser.parseClaimsJws(authToken);
            String userId = claims.getBody().getSubject();

            logger.debug("Token hợp lệ cho người dùng: {}", userId);

            // Improved token validation with more lenient approach during development
            String storedToken = tokenManager.getToken(userId);
            if (storedToken == null) {
                // If token not found in store but still valid by signature,
                // consider saving it for development convenience
                Date expiration = claims.getBody().getExpiration();
                if (expiration != null && expiration.after(new Date())) {
                    logger.info("Valid token found for user {} but not in store. Re-registering.", userId);
                    tokenManager.saveToken(userId, authToken, expiration.getTime());
                    return true;
                }
                logger.warn("Token not found in store for user: {}", userId);
                return false;
            }
            
            // Allow any valid token for the user during development
            // In production, you would want to compare storedToken.equals(authToken)
            
            Date expiration = claims.getBody().getExpiration();
            Date now = new Date();
            long skewMillis = clockSkewSeconds * 3600000;

            logger.debug("Kiểm tra thời gian hết hạn token - Hiện tại: {}, Hết hạn: {}, Sai số: {} ms",
                    now, expiration, skewMillis);

            if (expiration.before(new Date(now.getTime() - skewMillis))) {
                logger.warn("Token đã hết hạn cho người dùng: {}. Hết hạn vào: {}", userId, expiration);
                tokenManager.removeToken(userId);
                return false;
            }

            logger.info("Token hợp lệ cho người dùng: {}. Thời gian còn lại: {} ms",
                    userId, (expiration.getTime() - now.getTime()));
            return true;

        } catch (ExpiredJwtException e) {
            logger.error("JWT đã hết hạn: {}", e.getMessage());
            try {
                String userId = e.getClaims().getSubject();
                tokenManager.removeToken(userId);
            } catch (Exception ex) {
                logger.error("Lỗi khi xóa token hết hạn: {}", ex.getMessage());
            }
            return false;
        } catch (Exception e) {
            logger.error("Lỗi xác thực JWT: {}", e.getMessage());
            return false;
        }
    }

    // Lấy JWT từ request
    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Lấy thời gian hết hạn của JWT
    public int getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    // Thêm phương thức để xóa token khi logout
    public void invalidateToken(String userId) {
        logger.info("Invalidating token for user: {}", userId);
        tokenManager.removeToken(userId);
        logger.debug("Token invalidated successfully for user: {}", userId);
    }

    // set dữ liệu cho token
    public String generateTokenFromUsername(String username) {
        logger.info("Generating new token from username: {}", username);
        String token = Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
        logger.debug("Token generated successfully for username: {}", username);
        return token;
    }

    // Thêm phương thức để kiểm tra secret key
    public void debugSecretKey() {
        try {
            Key k = key();
            logger.info("Secret key algorithm: {}", k.getAlgorithm());
            logger.info("Secret key format: {}", k.getFormat());
            logger.info("Secret key length: {} bits", k.getEncoded().length * 8);
        } catch (Exception e) {
            logger.error("Error debugging secret key: {}", e.getMessage());
        }
    }
}
