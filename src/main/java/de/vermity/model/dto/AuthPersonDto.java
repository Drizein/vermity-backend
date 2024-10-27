package de.vermity.model.dto;

import de.vermity.util.enums.Gender;
import de.vermity.util.enums.Role;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;

/**
 * @param firstName
 * @param lastName
 * @param gender
 * @param phoneNumber
 * @param email
 * @param birthDate
 * @param address
 * @param roleList
 * @param password
 * @author Jan Tiedt
 */
public record AuthPersonDto(
    @NonNull String firstName,
    @NonNull String lastName,
    @NonNull Gender gender,
    @NonNull String phoneNumber,
    @NonNull String email,
    @NonNull LocalDate birthDate,
    @NonNull AddressDto address,
    @NonNull List<Role> roleList,
    @NonNull String password
) implements Serializable {

}
