package de.vermity.controller;


import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import de.vermity.model.dto.AuthPersonDto;
import de.vermity.model.dto.LoginDto;
import de.vermity.model.dto.PasswordChangeDto;
import de.vermity.model.dto.RegisterDto;
import de.vermity.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(name = "Users", description = "handles all user related stuff")
@RequiredArgsConstructor
@RequestMapping
public class PersonController {

  private final PersonService personService;

  /**
   * @param registerDto User data
   * @return Error or success messages
   * @author Cedric Stumpf
   */
  @PostMapping(value = "register", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Create a new user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User created"),
      @ApiResponse(responseCode = "400", description = "Email already exists or Password does not match the requirements!")

  })
  ResponseEntity<String> register(@RequestBody RegisterDto registerDto) {
    return personService.createUser(registerDto);
  }

  /**
   * @param authPersonDto PersonDto with password for validation
   * @return Error or success messages
   * @author Jan Tiedt
   */
  @DeleteMapping(value = "/auth/delete", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "delete a user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User deleted"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials")

  })
  ResponseEntity<String> delete(@RequestBody AuthPersonDto authPersonDto,
      @RequestHeader String Authorization) {
    return personService.deleteUser(authPersonDto, Authorization);
  }


  /**
   * @param loginDto User data
   * @return Error message or personDto on success
   * @author Cedric Stumpf
   */
  @PostMapping(value = "login", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "Login an user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login successful"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "500", description = "JWT could not be created"),
  })
  ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
    return personService.loginUser(loginDto);
  }

  /**
   * @param Authorization JWT
   * @return Error message or user entity on success
   * @author Cedric Stumpf
   */
  @GetMapping(value = "/auth/getUser", produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "Get an user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "successful"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials")
  })
  ResponseEntity<?> getUser(@RequestHeader String Authorization) {
    return personService.getUser(Authorization);
  }

  /**
   * @param passwordChangeDto old and new password
   * @return Error or success message
   * @author Cedric Stumpf
   */
  @PatchMapping(value = "/auth/changePassword", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Change Password of an user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Password changed"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "400", description = "Password does not match the requirements!")
  })
  ResponseEntity<String> changePassword(@RequestBody PasswordChangeDto passwordChangeDto,
      @RequestHeader String Authorization) {
    return personService.changePassword(passwordChangeDto, Authorization);
  }

  /**
   * @param authPersonDto PersonDto with password for validation
   * @return Error messages or on success user entity
   * @author Cedric Stumpf
   */
  @PatchMapping(value = "/auth/modifyUser", consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "Modify user")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User modified"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "400", description = "Address not found")
  })
  ResponseEntity<?> modifyUser(@RequestBody AuthPersonDto authPersonDto,
      @RequestHeader String Authorization) {
    return personService.modifyUser(authPersonDto, Authorization);
  }

}
