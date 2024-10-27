package de.vermity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * This class is used to generate and validate JWT tokens for the authentication of users. The JWT
 * token is generated using the email of the user. The token is then used to authenticate the user.
 *
 * @author Cedric Stumpf
 */
@Component
public class JWTGenerator {

  private static final SecretKey KEY = Jwts.SIG.HS512.key().build();
  @Value("${vermity.token.expiration:3600000}")
  private long JWT_EXPIRATION;

  public String generateToken(Authentication authentication) throws InvalidKeyException {
    String email = authentication.getName();
    Date currentDate = new Date();
    Date expireDate = new Date(currentDate.getTime() + JWT_EXPIRATION);

    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date())
        .expiration(expireDate)
        .signWith(KEY)
        .compact();
  }

  public String getUsernameFromJWT(String token) throws JwtException, IllegalArgumentException {
    Claims claims = Jwts.parser()
        .verifyWith(KEY)
        .build()
        .parseSignedClaims(token)
        .getPayload();
    return claims.getSubject();
  }

  public boolean validateToken(String token) throws JwtException, IllegalArgumentException {
    Jwts.parser()
        .verifyWith(KEY)
        .build()
        .parseSignedClaims(token);
    return true;
  }

  public String extractToken(String bearerToken) {
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return "";
  }
}
