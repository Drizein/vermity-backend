package de.vermity.service;

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
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.InvalidKeyException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class PersonService {

  private final PersonRepository personRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JWTGenerator jwtGenerator;
  private final UserUtil userUtil;
  private final AddressRepository addressRepository;

  /**
   * @param registerDto User data
   * @return Status code
   * @author Cedric Stumpf
   */
  public ResponseEntity<String> createUser(RegisterDto registerDto) {
    // Check for duplicate email, if known return BadRequest
    log.info("Creating an user");
    if (personRepository.findByEmail(registerDto.email()).isPresent()) {
      return ResponseEntity.badRequest().body("Email existiert bereits!");
    }
    if (!userUtil.isPasswordValid(registerDto.password())) {
      return ResponseEntity.badRequest().body("Passwort entspricht nicht den Anforderungen!");
    }
    if (!registerDto.birthDate().isBefore(LocalDate.now().minusYears(16))) {
      log.warn("User must be at least 16 years old to register");
      return ResponseEntity.badRequest().body("Du musst mindestens 16 Jahre alt sein, um dich zu registrieren.");
    }

    Address address;
    if (addressRepository.findByStateAndCityAndStreetAndZipAndCountry("", "", "", 0, "").isPresent()) {
      address = addressRepository.findByStateAndCityAndStreetAndZipAndCountry("", "", "", 0, "").get();
    } else {
      address = addressRepository.saveAndFlush(Address.builder()
          .zip(0)
          .city("")
          .state("")
          .country("")
          .street("")
          .build());
    }

    var person =
        Person.builder()
            .email(registerDto.email())
            .firstName(registerDto.firstName())
            .lastName(registerDto.lastName())
            .phoneNumber(registerDto.phoneNumber())
            .gender(registerDto.gender())
            .birthDate(registerDto.birthDate())
            .password(passwordEncoder.encode(registerDto.password()))
            .address(address)
            .roleList(registerDto.roleList())
            .build();
    personRepository.saveAndFlush(person);
    log.info("User created");
    return ResponseEntity.created(URI.create("/")).body("Benutzer erstellt");
  }

  /**
   * @param authPersonDto PersonDto with password for validation
   * @return Status code
   * @author Jan Tiedt
   */
  public ResponseEntity<String> deleteUser(AuthPersonDto authPersonDto, String bearerToken) {
    // Check for email, if unknown return BadRequest
    log.info("Delete an user");
    Optional<Person> user;
    // verify Token
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()
          || !passwordEncoder.matches(authPersonDto.password(), user.get().getPassword())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalide Anmeldeinformationen");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }
    personRepository.delete(user.get());
    log.info("User deleted");
    return ResponseEntity.status(HttpStatus.OK).body("Benutzer gelöscht");
  }

  /**
   * @param loginDto Email and password
   * @return warn message or on success personDto
   * @author Jan Tiedt
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> loginUser(LoginDto loginDto) {
    log.info("User requested login");
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password()));
    } catch (AuthenticationException e) {
      log.warn("Authentication failed", e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Invalide Anmeldeinformationen.");
    }
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String token;
    if (!authentication.isAuthenticated()) {
      log.warn("Authentication failed. Invalid credentials");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalide Anmeldeinformationen");
    }

    try {
      token = jwtGenerator.generateToken(authentication);
    } catch (InvalidKeyException e) {
      log.warn("JWT generation failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("JWT erstellung fehlgeschlagen");
    }

    var person = personRepository.findByEmail(loginDto.email());
    if (person.isEmpty()
        || !passwordEncoder.matches(loginDto.password(), person.get().getPassword())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalide Anmeldeinformationen");
    }
    var personDto =
        new PersonDto(
            person.get().getFirstName(),
            person.get().getLastName(),
            person.get().getGender(),
            person.get().getPhoneNumber(),
            person.get().getEmail(),
            person.get().getBirthDate(),
            new AddressDto(
                person.get().getAddress().getStreet(),
                person.get().getAddress().getZip(),
                person.get().getAddress().getCity(),
                person.get().getAddress().getCountry(),
                person.get().getAddress().getState()),
            person.get().getRoleList(),
            "Bearer " + token);
    return ResponseEntity.ok(personDto);
  }

  /**
   * Change password
   *
   * @param passwordChangeDto old and new password
   * @param bearerToken       JWT
   * @return warn messages or on success password changed message
   * @author Cedric Stumpf
   */
  public ResponseEntity<String> changePassword(
      PasswordChangeDto passwordChangeDto, String bearerToken) {
    log.info("Change of password requested");
    Optional<Person> user;
    // verify Token
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()
          || !passwordEncoder.matches(passwordChangeDto.oldPassword(), user.get().getPassword())) {
        log.warn("Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalide Anmeldeinformationen");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }
    if (!userUtil.isPasswordValid(passwordChangeDto.newPassword())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Neues Passwort entspricht nicht den Anforderungen!");
    }
    user.get().setPassword(passwordEncoder.encode(passwordChangeDto.newPassword()));
    user.get().setUpdatedAt();
    personRepository.saveAndFlush(user.get());
    log.info("Password changed");
    return ResponseEntity.ok("Passwort geändert");
  }

  /**
   * @param authPersonDto PersonDto with password for validation
   * @param bearerToken   Token for verification
   * @return warn message or on success personDto
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> modifyUser(AuthPersonDto authPersonDto, String bearerToken) {
    log.info("Modify an user");
    Optional<Person> user;
    // verify Token
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()
          || !passwordEncoder.matches(authPersonDto.password(), user.get().getPassword())) {
        log.warn("Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalide Anmeldeinformationen");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }
    Optional<Address> address =
        addressRepository.findByStateAndCityAndStreetAndZipAndCountry(
            authPersonDto.address().state(),
            authPersonDto.address().city(),
            authPersonDto.address().street(),
            authPersonDto.address().zip(),
            authPersonDto.address().country());
    if (address.isEmpty()) {
      log.warn("Address not found, creating new one");
      address = Optional.of(addressRepository.saveAndFlush(Address.builder()
          .zip(authPersonDto.address().zip())
          .city(authPersonDto.address().city())
          .state(authPersonDto.address().state())
          .country(authPersonDto.address().country())
          .street(authPersonDto.address().street())
          .build()));
    }

    user.get().setFirstName(authPersonDto.firstName());
    user.get().setAddress(address.get());
    user.get().setEmail(authPersonDto.email());
    user.get().setLastName(authPersonDto.lastName());
    user.get().setGender(authPersonDto.gender());
    user.get().setPhoneNumber(authPersonDto.phoneNumber());
    user.get().setRoleList(authPersonDto.roleList());
    var savedUser = personRepository.saveAndFlush(user.get());

    PersonDto modifiedPerson = buildPersonDTO(savedUser, bearerToken);
    log.info("User modified");
    return ResponseEntity.status(HttpStatus.OK).body(modifiedPerson);
  }

  /**
   * Get user by JWT
   *
   * @param bearerToken JWT
   * @return PersonDto
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getUser(String bearerToken) {
    Optional<Person> user;
    // verify Token
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()) {
        log.warn("Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalide Anmeldeinformationen");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }
    return ResponseEntity.status(HttpStatus.OK).body(buildPersonDTO(user.get(), bearerToken));
  }

  /**
   * @param person      Person
   * @param bearerToken JWT
   * @return PersonDto
   * @author Cedric Stumpf
   */
  private PersonDto buildPersonDTO(Person person, String bearerToken) {
    return new PersonDto(
        person.getFirstName(),
        person.getLastName(),
        person.getGender(),
        person.getPhoneNumber(),
        person.getEmail(),
        person.getBirthDate(),
        new AddressDto(
            person.getAddress().getStreet(),
            person.getAddress().getZip(),
            person.getAddress().getCity(),
            person.getAddress().getCountry(),
            person.getAddress().getState()),
        person.getRoleList(),
        bearerToken);
  }
}
