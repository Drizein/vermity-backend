package de.vermity.model.dto;

import de.vermity.util.enums.Gender;
import de.vermity.util.enums.Role;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;

/**
 * DTO for {@link de.vermity.model.entity.Person}
 *
 * @param firstName
 * @param lastName
 * @param gender
 * @param phoneNumber
 * @param email
 * @param birthDate
 * @param password
 * @param roleList
 * @author Cedric Stumpf
 */
public record RegisterDto(
    @NonNull String firstName,
    @NonNull String lastName,
    @NonNull Gender gender,
    @NonNull String phoneNumber,
    @NonNull String email,
    @NonNull LocalDate birthDate,
    @NonNull String password,
    @NonNull List<Role> roleList
) implements Serializable {

}