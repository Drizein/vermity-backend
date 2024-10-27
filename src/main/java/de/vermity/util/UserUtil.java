package de.vermity.util;

import de.vermity.model.entity.Person;
import de.vermity.persistence.PersonRepository;
import de.vermity.security.JWTGenerator;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Util class for verifying user and validating password
 *
 * @author Cedric Stumpf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserUtil {

  private final static Pattern PASSWORD_REGEX = Pattern.compile(
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&+-])[A-Za-z\\d@$!%*?&+-]{12,}$");

  @Autowired
  private final JWTGenerator jwtGenerator;

  @Autowired
  private final PersonRepository personRepository;

  public Optional<Person> verifyUser(String bearerToken)
      throws IllegalArgumentException, JwtException {
    // verify user by bearer token
    log.info("Verifying user");
    String usernameFromJWT = jwtGenerator.getUsernameFromJWT(
        jwtGenerator.extractToken(bearerToken));
    return personRepository.findByEmail(usernameFromJWT);
  }

  public boolean isPasswordValid(String password) {
    log.info("Verifying password");
    return PASSWORD_REGEX.matcher(password)
        .matches(); // PASSWORD_REGEX.pattern().matches(password) der Regex als String (s.h. Constante) wird gegen den String des PW versucht zu matchen -> schlägt fehl. Des Wegen matcher(password).matches()
  }

}
