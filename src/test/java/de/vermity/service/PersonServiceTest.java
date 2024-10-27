package de.vermity.service;

import static de.vermity.util.enums.Gender.FEMALE;
import static de.vermity.util.enums.Gender.MALE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vermity.model.dto.AddressDto;
import de.vermity.model.dto.AuthPersonDto;
import de.vermity.model.dto.LoginDto;
import de.vermity.model.dto.PasswordChangeDto;
import de.vermity.model.dto.PersonDto;
import de.vermity.model.dto.RegisterDto;
import de.vermity.model.entity.Address;
import de.vermity.model.entity.Person;
import de.vermity.persistence.AddressRepository;
import de.vermity.persistence.PersonRepository;
import de.vermity.security.JWTGenerator;
import de.vermity.util.UserUtil;
import de.vermity.util.enums.Gender;
import de.vermity.util.enums.Role;
import io.jsonwebtoken.security.InvalidKeyException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Cedric Stumpf <br> Test class for {@link PersonService}
 */
@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

  @Mock
  private PersonRepository personRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JWTGenerator jwtGenerator;

  @Mock
  private UserUtil userUtil;

  @Mock
  private AddressRepository addressRepository;

  @InjectMocks
  private PersonService personService;

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#createUser(RegisterDto)} tests
   * for successful creation of a user returns user created
   */
  @Test
  void createUser_withValidData_createsUserSuccessfully() {
    // given
    RegisterDto personDto =
        new RegisterDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            "Password1234556!",
            List.of(Role.TENANT));

    // when
    when(personRepository.findByEmail(personDto.email())).thenReturn(Optional.empty());
    when(userUtil.isPasswordValid(personDto.password())).thenReturn(true);
    when(personRepository.saveAndFlush(ArgumentMatchers.any(Person.class)))
        .thenReturn(new Person());

    var response = personService.createUser(personDto);

    // then
    verify(personRepository, times(1)).findByEmail(personDto.email());
    verify(userUtil, times(1)).isPasswordValid(personDto.password());
    verify(personRepository, times(1)).saveAndFlush(any(Person.class));
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
        () -> assertThat(response.getBody()).isEqualTo("Benutzer erstellt"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#createUser(RegisterDto)} tests
   * for duplicate email returns bad request
   */
  @Test
  void createUser_withDuplicateEmail_returnsBadRequest() {
    // given
    RegisterDto personDto =
        new RegisterDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            "Password1234556!",
            List.of(Role.TENANT));

    // when
    when(personRepository.findByEmail(personDto.email())).thenReturn(Optional.of(new Person()));

    var response = personService.createUser(personDto);

    // then
    verify(personRepository, times(1)).findByEmail(personDto.email());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () ->
            assertThat(response.getBody()).isEqualTo("Email existiert bereits!"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#createUser(RegisterDto)} tests
   * for invalid password returns bad request
   */
  @Test
  void createUser_withInvalidPassword_returnsBadRequest() {
    // given
    RegisterDto personDto =
        new RegisterDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            "Invalid",
            List.of(Role.TENANT));

    // when
    when(personRepository.findByEmail(personDto.email())).thenReturn(Optional.empty());
    when(userUtil.isPasswordValid(personDto.password())).thenReturn(false);
    var response = personService.createUser(personDto);

    // then
    verify(personRepository, times(1)).findByEmail(personDto.email());
    verify(userUtil, times(1)).isPasswordValid(personDto.password());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () ->
            assertThat(response.getBody()).isEqualTo("Passwort entspricht nicht den Anforderungen!"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#deleteUser(AuthPersonDto, String)} tests for successful deletion of a user
   * returns user deleted
   */
  @Test
  void deleteUser_withValidCredentials_deletesUserSuccessfully() {
    // given
    String token = "Bearer Token";
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            new AddressDto("Street", 12345, "City", "Country", "State"),
            List.of(Role.TENANT),
            "Password1234556!");

    // when
    when(userUtil.verifyUser(token))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches(authPersonDto.password(), "Password1234556!")).thenReturn(true);
    doNothing().when(personRepository).delete(any(Person.class));
    var response = personService.deleteUser(authPersonDto, token);

    // then
    verify(userUtil, times(1)).verifyUser(token);
    verify(passwordEncoder, times(1)).matches(authPersonDto.password(), "Password1234556!");
    verify(personRepository, times(1)).delete(any(Person.class));
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isEqualTo("Benutzer gelöscht"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#deleteUser(AuthPersonDto, String)} tests for invalid credentials returns
   * unauthorized
   */
  @Test
  void deleteUser_withInvalidCredentials_returnsUnauthorized() {
    // given
    String token = "Bearer Token";
    // create user
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            new AddressDto("Street", 12345, "City", "Country", "State"),
            List.of(Role.TENANT),
            "Password1234556!");

    // when
    when(userUtil.verifyUser("Bearer Token"))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches("Password1234556!", "Password1234556!")).thenReturn(false);
    var response = personService.deleteUser(authPersonDto, token);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#deleteUser(AuthPersonDto, String)} tests for invalid token returns
   * unauthorized
   */
  @Test
  void deleteUser_withInvalidToken_returnsUnauthorized() {
    // given
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            new AddressDto("Street", 12345, "City", "Country", "State"),
            List.of(Role.TENANT),
            "Password1234556!");

    // when
    when(userUtil.verifyUser("Bearer InvalidToken"))
        .thenThrow(new IllegalArgumentException("Invalid token"));
    var response = personService.deleteUser(authPersonDto, "Bearer InvalidToken");

    // then
    verify(userUtil, times(1)).verifyUser("Bearer InvalidToken");
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () ->
            assertThat(response.getBody())
                .isEqualTo("Bitte melde dich zuerst an!"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#deleteUser(AuthPersonDto, String)} tests for invalid user returns
   * unauthorized
   */
  @Test
  void deleteUser_withUserNotFound_returnsUnauthorized() {
    // given
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            new AddressDto("Street", 12345, "City", "Country", "State"),
            List.of(Role.TENANT),
            "Password1234556!");

    // when
    when(userUtil.verifyUser("Bearer InvalidToken")).thenReturn(Optional.empty());
    var response = personService.deleteUser(authPersonDto, "Bearer InvalidToken");

    // then
    verify(userUtil, times(1)).verifyUser("Bearer InvalidToken");
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () ->
            assertThat(response.getBody())
                .isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * successful login returns personDto
   */
  @Test
  void loginUser_withValidCredentials_returnsPersonDto() {
    // given
    // create user
    RegisterDto personDto =
        new RegisterDto(
            "Tester",
            "Test",
            MALE,
            "+49123456789",
            "t@tester.com",
            LocalDate.of(2000, 1, 1),
            "Password1234556!",
            List.of(Role.TENANT));

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "t@tester.com",
            "Password1234556!",
            List.of(new SimpleGrantedAuthority(Role.TENANT.name())));
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    // when
    when(personRepository.findByEmail(personDto.email()))
        .thenReturn(
            Optional.of(
                Person.builder()
                    .firstName("Tester")
                    .lastName("Test")
                    .gender(MALE)
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .email("asd@asd.de")
                    .roleList(List.of(Role.TENANT))
                    .phoneNumber("+49123456789")
                    .password("Password1234556!")
                    .address(
                        Address.builder()
                            .city("city")
                            .country("country")
                            .zip(123412)
                            .state("state")
                            .street("street")
                            .build())
                    .build()));
    when(passwordEncoder.matches("Password1234556!", "Password1234556!")).thenReturn(true);

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtGenerator.generateToken(any())).thenReturn("Bearer Token");
    var response = personService.loginUser(new LoginDto("t@tester.com", "Password1234556!"));

    // then
    verify(personRepository, times(1)).findByEmail(personDto.email());
    verify(passwordEncoder, times(1)).matches("Password1234556!", "Password1234556!");
    verify(jwtGenerator, times(1)).generateToken(any());
    verify(authenticationManager, times(1)).authenticate(any());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isInstanceOf(PersonDto.class),
        () ->
            assertThat(
                ((PersonDto) Objects.requireNonNull(response.getBody()))
                    .token()
                    .startsWith("Bearer"))
                .isTrue());
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * invalid credentials returns unauthorized
   */
  @Test
  void loginUser_withInvalidCredentials_returnsUnauthorized() {
    // given
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "t@tester.com",
            "Password1234556!",
            List.of(new SimpleGrantedAuthority(Role.TENANT.name())));
    context.setAuthentication(authentication);
    context.getAuthentication().setAuthenticated(false);
    SecurityContextHolder.setContext(context);

    // when
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    var response = personService.loginUser(new LoginDto("t@tester.com", "Invalid"));

    // then
    verify(authenticationManager, times(1)).authenticate(any());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * invalid credentials returns InternalServerError
   */
  @Test
  void loginUser_withJwtGeneratorThrowsException_returnsInternalServerError() {
    // given
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "t@tester.com",
            "Password1234556!",
            List.of(new SimpleGrantedAuthority(Role.TENANT.name())));
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    // when
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtGenerator.generateToken(any())).thenThrow(new InvalidKeyException("Invalid key"));

    var response = personService.loginUser(new LoginDto("t@tester.com", "Invalid"));

    // then
    verify(authenticationManager, times(1)).authenticate(any());
    verify(jwtGenerator, times(1)).generateToken(any());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
        () -> assertThat(response.getBody()).isEqualTo("JWT erstellung fehlgeschlagen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * invalid credentials returns unauthorized
   */
  @Test
  void loginUser_withAuthenitcationmangerThrowsException_returnsUnauthorized() {
    // given

    // when
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new AuthenticationException("Bitte melde dich zuerst an!") {
        });
    var response = personService.loginUser(new LoginDto("t@tester.com", "Invalid"));

    // then
    verify(authenticationManager, times(1)).authenticate(any());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen."));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * user not found returns unauthorized
   */
  @Test
  void loginUser_withUserNotFound_returnsUnauthorized() {
    // given
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "t@tester.com",
            "Password1234556!",
            List.of(new SimpleGrantedAuthority(Role.TENANT.name())));
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    // when
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtGenerator.generateToken(any())).thenReturn("Bearer Token");
    when(personRepository.findByEmail(any())).thenReturn(Optional.empty());

    var response = personService.loginUser(new LoginDto("t@tester.com", "Invalid"));

    // then
    verify(authenticationManager, times(1)).authenticate(any());
    verify(jwtGenerator, times(1)).generateToken(any());
    verify(personRepository, times(1)).findByEmail(any());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * password miss match returns unauthorized
   */
  @Test
  void loginUser_withPasswordMissMatch_returnsUnauthorized() {
    // given
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "t@tester.com",
            "Password1234556!",
            List.of(new SimpleGrantedAuthority(Role.TENANT.name())));
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    // when
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtGenerator.generateToken(any())).thenReturn("Bearer Token");
    when(personRepository.findByEmail(any()))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    var response = personService.loginUser(new LoginDto("t@tester.com", "Invalid"));

    // then
    verify(authenticationManager, times(1)).authenticate(any());
    verify(jwtGenerator, times(1)).generateToken(any());
    verify(personRepository, times(1)).findByEmail(any());
    verify(passwordEncoder, times(1)).matches(any(), any());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#loginUser(LoginDto)} tests for
   * invalid email returns unauthorized
   */
  @Test
  void changePassword_withValidData_changesPasswordSuccessfully() {
    // given

    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches("Password1234556!", "Password1234556!")).thenReturn(true);
    when(userUtil.isPasswordValid("NewSpecialPassword123414112412!??")).thenReturn(true);
    when(passwordEncoder.encode("NewSpecialPassword123414112412!??"))
        .thenReturn("NewSpecialPassword123414112412!??");
    when(personRepository.saveAndFlush(any(Person.class))).thenReturn(new Person());
    var response =
        personService.changePassword(
            new PasswordChangeDto("Password1234556!", "NewSpecialPassword123414112412!??"),
            "Bearer Token");
    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    verify(passwordEncoder, times(1)).matches("Password1234556!", "Password1234556!");
    verify(userUtil, times(1)).isPasswordValid("NewSpecialPassword123414112412!??");
    verify(passwordEncoder, times(1)).encode("NewSpecialPassword123414112412!??");
    verify(personRepository, times(1)).saveAndFlush(any(Person.class));
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isEqualTo("Passwort geändert"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#changePassword(PasswordChangeDto, String)} tests for invalid old password
   * returns unauthorized
   */
  @Test
  void changePassword_withInvalidOldPassword_returnsUnauthorized() {
    // given

    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches("Invalid", "Password1234556!")).thenReturn(false);
    var response =
        personService.changePassword(
            new PasswordChangeDto("Invalid", "NewSpecialPassword123414112412!??"), "Bearer Token");
    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    verify(passwordEncoder, times(1)).matches("Invalid", "Password1234556!");
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#changePassword(PasswordChangeDto, String)} tests for invalid token returns
   * unauthorized
   */
  @Test
  void changePassword_withInvalidToken_returnsUnauthorized() {
    // when
    when(userUtil.verifyUser("Bearer Invalid"))
        .thenThrow(new IllegalArgumentException("Invalid token"));
    var response =
        personService.changePassword(
            new PasswordChangeDto("Password1234556", "NewSpecialPassword123414112412!??"),
            "Bearer Invalid");
    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () ->
            assertThat(response.getBody())
                .isEqualTo("Bitte melde dich zuerst an!"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#changePassword(PasswordChangeDto, String)} tests for user not found
   * returns unauthorized
   */
  @Test
  void changePassword_withUserNotFound_returnsUnauthorized() {
    // when
    when(userUtil.verifyUser("Bearer Invalid")).thenReturn(Optional.empty());
    var response =
        personService.changePassword(
            new PasswordChangeDto("Password1234556", "NewSpecialPassword123414112412!??"),
            "Bearer Invalid");
    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#changePassword(PasswordChangeDto, String)} tests for invalid new password
   * returns bad request
   */
  @Test
  void changePassword_withInvalidNewPassword_returnsBadRequest() {
    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches("Password1234556!", "Password1234556!")).thenReturn(true);
    when(userUtil.isPasswordValid("Invalid")).thenReturn(false);
    var response =
        personService.changePassword(
            new PasswordChangeDto("Password1234556!", "Invalid"), "Bearer Token");
    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    verify(passwordEncoder, times(1)).matches("Password1234556!", "Password1234556!");
    verify(userUtil, times(1)).isPasswordValid("Invalid");
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () ->
            assertThat(response.getBody())
                .isEqualTo("Neues Passwort entspricht nicht den Anforderungen!"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#modifyUser(AuthPersonDto, String)} tests for successful modification of a
   * user returns personDto
   */
  @Test
  void modifyUser_withValidData_modifiesUserSuccessfully() {
    // given
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            "t@tester.com",
            "Password1234556!",
            List.of(new SimpleGrantedAuthority(Role.TENANT.name())));
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Adam",
            "Married.PNG",
            Gender.FEMALE,
            "+32222222222",
            "new@mail.com",
            LocalDate.of(1800, 1, 1),
            new AddressDto("street", 12345, "city", "country", "state"),
            List.of(Role.TENANT, Role.LANDLORD),
            "Password1234556!");

    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches("Password1234556!", "Password1234556!")).thenReturn(true);
    when(personRepository.saveAndFlush(any(Person.class)))
        .thenReturn(
            Person.builder()
                .firstName("Tester")
                .lastName("Test")
                .gender(MALE)
                .email("new@mail.com")
                .phoneNumber("+32222222222")
                .roleList(List.of(Role.TENANT, Role.LANDLORD))
                .password("Password1234556!")
                .birthDate(LocalDate.of(2000, 1, 1))
                .address(
                    Address.builder()
                        .city("city")
                        .country("country")
                        .zip(123412)
                        .state("state")
                        .street("street")
                        .build())
                .build());
    when(addressRepository.findByStateAndCityAndStreetAndZipAndCountry(
        "state", "city", "street", 12345, "country"))
        .thenReturn(
            Optional.of(
                Address.builder()
                    .city("city")
                    .country("country")
                    .zip(123412)
                    .state("state")
                    .street("street")
                    .build()));
    var response = personService.modifyUser(authPersonDto, "Bearer Token");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    verify(passwordEncoder, times(1)).matches("Password1234556!", "Password1234556!");
    verify(personRepository, times(1)).saveAndFlush(any(Person.class));
    verify(addressRepository, times(1))
        .findByStateAndCityAndStreetAndZipAndCountry("state", "city", "street", 12345, "country");

    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isInstanceOf(PersonDto.class),
        () ->
            assertThat(((PersonDto) Objects.requireNonNull(response.getBody())).email())
                .isEqualTo("new@mail.com"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#modifyUser(AuthPersonDto, String)} tests for invalid credentials returns
   * unauthorized
   */
  @Test
  void modifyUser_withInvalidCredentials_returnsUnauthorized() {
    // given
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Adam",
            "Married.PNG",
            Gender.FEMALE,
            "+32222222222",
            "new@mail.com",
            LocalDate.of(1800, 1, 1),
            new AddressDto("street", 12345, "city", "country", "state"),
            List.of(Role.TENANT, Role.LANDLORD),
            "Invalid Password");

    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.of(Person.builder().password("Password1234556!").build()));
    when(passwordEncoder.matches("Invalid Password", "Password1234556!")).thenReturn(false);

    var response = personService.modifyUser(authPersonDto, "Bearer Token");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    verify(passwordEncoder, times(1)).matches("Invalid Password", "Password1234556!");
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#modifyUser(AuthPersonDto, String)} tests for invalid credentials returns
   * unauthorized
   */
  @Test
  void modifyUser_withUserNotFound_returnsUnauthorized() {
    // given
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Adam",
            "Married.PNG",
            Gender.FEMALE,
            "+32222222222",
            "new@mail.com",
            LocalDate.of(1800, 1, 1),
            new AddressDto("street", 12345, "city", "country", "state"),
            List.of(Role.TENANT, Role.LANDLORD),
            "Invalid Password");

    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.empty());

    var response = personService.modifyUser(authPersonDto, "Bearer Token");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"));
  }

  /**
   * @author Cedric Stumpf <br> Test method for
   * {@link PersonService#modifyUser(AuthPersonDto, String)} tests for invalid token returns
   * unauthorized
   */
  @Test
  void modifyUser_withInvalidToken_returnsUnauthorized() {
    // given

    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Adam",
            "Married.PNG",
            Gender.FEMALE,
            "+32222222222",
            "new@mail.com",
            LocalDate.of(1800, 1, 1),
            new AddressDto("street", 12345, "city", "country", "state"),
            List.of(Role.TENANT, Role.LANDLORD),
            "Password1234556!");

    // when
    when(userUtil.verifyUser(anyString())).thenThrow(new IllegalArgumentException("Invalid token"));

    var response = personService.modifyUser(authPersonDto, "Bearer Invalid");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () ->
            assertThat(response.getBody())
                .isEqualTo("Bitte melde dich zuerst an!"),
        () -> assertThat(response.getBody()).isInstanceOf(String.class));
  }

  /**
   * Test method for {@link PersonService#modifyUser(AuthPersonDto, String)} tests for invalid new
   * address returns bad request
   *
   * @author Cedric Stumpf
   */
  @Test
  void modifyUser_withNonExistentAddress_createsNewAddress() {
    // given
    AuthPersonDto authPersonDto =
        new AuthPersonDto(
            "Adam",
            "Married.PNG",
            Gender.FEMALE,
            "+32222222222",
            "new@mail.com",
            LocalDate.of(1800, 1, 1),
            new AddressDto("street", 12345, "city", "country", "state"),
            List.of(Role.TENANT, Role.LANDLORD),
            "Password1234556!");
    Person person = Person.builder()
        .address(
        Address.builder().street("street").zip(12345).country("country").state("state").city("city")
            .build())
        .firstName("Adam")
        .lastName("Married.PNG")
        .gender(FEMALE)
        .phoneNumber("+32222222222")
        .email("new@email.com")
        .birthDate(LocalDate.of(1800, 1, 1))
        .roleList(List.of(Role.TENANT, Role.LANDLORD))
        .password("Password1234556!").build();
    // when
    when(userUtil.verifyUser(anyString()))
        .thenReturn(Optional.of(person));
    when(passwordEncoder.matches("Password1234556!", "Password1234556!")).thenReturn(true);
    when(addressRepository.findByStateAndCityAndStreetAndZipAndCountry(
        "state", "city", "street", 12345, "country"))
        .thenReturn(Optional.empty());
    when(addressRepository.saveAndFlush(any())).thenReturn(
        Address.builder().street("street").zip(12345).country("country").state("state").city("city")
            .build());
    when(personRepository.saveAndFlush(any())).thenReturn(person);
    var response = personService.modifyUser(authPersonDto, "Bearer Token");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    verify(passwordEncoder, times(1)).matches("Password1234556!", "Password1234556!");
    verify(addressRepository, times(1))
        .findByStateAndCityAndStreetAndZipAndCountry("state", "city", "street", 12345, "country");
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertNotNull(response.getBody()),
        () -> assertThat(response.getBody()).isInstanceOf(PersonDto.class));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#getUser(String)} tests for
   * valid token returns personDto
   */
  @Test
  void getUser_withValidToken_returnsPersonDto() {
    // given
    String token = "Bearer Token";
    // when
    when(userUtil.verifyUser(token))
        .thenReturn(
            Optional.of(
                Person.builder()
                    .firstName("Tester")
                    .lastName("Test")
                    .gender(MALE)
                    .email("new@mail.com")
                    .phoneNumber("+32222222222")
                    .roleList(List.of(Role.TENANT, Role.LANDLORD))
                    .password("Password1234556!")
                    .birthDate(LocalDate.of(2000, 1, 1))
                    .address(
                        Address.builder()
                            .city("city")
                            .country("country")
                            .zip(123412)
                            .state("state")
                            .street("street")
                            .build())
                    .build()));
    var response = personService.getUser(token);

    // then
    verify(userUtil, times(1)).verifyUser(token);
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).isInstanceOf(PersonDto.class),
        () -> assertThat(response.getBody()).isNotNull());
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#getUser(String)} tests for
   * invalid token returns unauthorized
   */
  @Test
  void getUser_withInvalidToken_returnsUnauthorized() {
    // given
    // when
    when(userUtil.verifyUser(anyString())).thenThrow(new IllegalArgumentException("Invalid token"));

    var response = personService.getUser("token");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () ->
            assertThat(response.getBody())
                .isEqualTo("Bitte melde dich zuerst an!"),
        () -> assertThat(response.getBody()).isInstanceOf(String.class));
  }

  /**
   * @author Cedric Stumpf <br> Test method for {@link PersonService#getUser(String)} tests for
   * invalid user returns unauthorized
   */
  @Test
  void getUser_withInvalidUser_returnsUnauthorized() {
    // given
    // when
    when(userUtil.verifyUser(anyString())).thenReturn(Optional.empty());

    var response = personService.getUser("token");

    // then
    verify(userUtil, times(1)).verifyUser(anyString());
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
        () -> assertThat(response.getBody()).isEqualTo("Invalide Anmeldeinformationen"),
        () -> assertThat(response.getBody()).isInstanceOf(String.class));
  }
}
