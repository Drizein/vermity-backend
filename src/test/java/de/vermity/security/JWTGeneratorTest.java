package de.vermity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.security.InvalidKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for the JWTGenerator class.
 *
 * @author Cedric Stumpf
 */
@TestPropertySource("classpath:application-test.properties")
@SpringJUnitConfig(JWTGenerator.class)
class JWTGeneratorTest {

  private JWTGenerator jwtGenerator;

  @Value("${vermity.token.expiration}")
  private long JWT_EXPIRATION;

  @BeforeEach
  void setUp() {
    jwtGenerator = new JWTGenerator();
  }

  @ParameterizedTest
  @CsvSource({"test@example.com, 3600000", "another@example.com, 7200000"})
  void generateToken_createsValidToken(String email, long expiration) throws InvalidKeyException {
    // given
    Authentication authentication = Mockito.mock(Authentication.class);
    when(authentication.getName()).thenReturn(email);
    // Update JWT_EXPIRATION for the test case
    ReflectionTestUtils.setField(jwtGenerator, "JWT_EXPIRATION", expiration);

    // when
    String token = jwtGenerator.generateToken(authentication);

    // then
    assertThat(token).isNotEmpty();
    assertThat(jwtGenerator.validateToken(token)).isTrue();
    assertThat(ReflectionTestUtils.getField(jwtGenerator, "JWT_EXPIRATION")).isEqualTo(expiration);
  }

  @ParameterizedTest
  @ValueSource(longs = {3600000})
  void validateTokenExpiration(Long expiration) {
    // given

    // when & then
    assertAll(() -> assertEquals(expiration, JWT_EXPIRATION));

  }

  @ParameterizedTest
  @CsvSource({"Bearer token, token", "Bearer anotherToken, anotherToken", "InvalidToken, ''",
      "AnotherInvalidToken, ''"})
  void extractToken_validBearerToken_returnsToken(String bearerToken, String expectedToken) {
    // when
    String token = jwtGenerator.extractToken(bearerToken);

    // then
    assertEquals(expectedToken, token);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void extractToken_validBearerToken_returnsEmptyString(String bearerToken) {
    // when
    String token = jwtGenerator.extractToken(bearerToken);

    // then
    assertEquals("", token);
  }
}