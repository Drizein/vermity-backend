package de.vermity.model.dto;

import de.vermity.util.enums.Gender;
import de.vermity.util.enums.Role;
import lombok.NonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * @param firstName
 * @param lastName
 * @param gender
 * @param phoneNumber
 * @param email
 * @param birthDate
 * @param address
 * @param roleList
 * @param token
 * @author Drizein
 */
public record PersonDto(
        @NonNull String firstName,
        @NonNull String lastName,
        @NonNull Gender gender,
        @NonNull String phoneNumber,
        @NonNull String email,
        @NonNull LocalDate birthDate,
        AddressDto address,
        @NonNull List<Role> roleList,
        @NonNull String token

) implements Serializable {

}
