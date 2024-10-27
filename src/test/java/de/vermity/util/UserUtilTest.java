package de.vermity.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

import de.vermity.model.entity.Person;
import de.vermity.persistence.PersonRepository;
import de.vermity.security.JWTGenerator;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for the UserUtil class.
 *
 * @author Cedric Stumpf
 */
@ExtendWith(MockitoExtension.class)
class UserUtilTest {

  @Mock
  private JWTGenerator jwtGenerator;

  @Mock
  private PersonRepository personRepository;

  @InjectMocks
  private UserUtil userUtil;

  private static Stream<Arguments> provideVerifyUser() {
    return Stream.of(
        Arguments.of(
            "Bearer validToken",
            "useremail@email.com",
            Optional.of(Person.builder().id(1).firstName("Test").lastName("User")
                .email("useremail@email.com").build())
        ),
        Arguments.of(
            "Bearer invalidToken",
            "",
            Optional.empty()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideVerifyUser")
  void verifyUser(String token, String email, Optional<Person> expectedPerson) {
    lenient().when(jwtGenerator.extractToken(token)).thenReturn(token);
    lenient().when(jwtGenerator.getUsernameFromJWT(token)).thenReturn(email);
    lenient().when(personRepository.findByEmail(email)).thenReturn(expectedPerson);

    var response = userUtil.verifyUser(token);

    assertEquals(expectedPerson, response);
  }

  @ParameterizedTest
  @CsvSource({
      "IchBinEinPasswort123!, true",
      "InvalidPassword, false"
  })
  void isPasswordValid(String password, Boolean expected) {
    var response = userUtil.isPasswordValid(password);

    assertEquals(expected, response);
  }
}