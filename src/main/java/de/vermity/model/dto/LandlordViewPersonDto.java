package de.vermity.model.dto;

import de.vermity.util.enums.Gender;
import java.io.Serializable;

/**
 * @param firstName
 * @param lastName
 * @param gender
 * @param phoneNumber
 * @param email
 * @param address
 * @author Cedric Stumpf
 */
public record LandlordViewPersonDto(
    String firstName,
    String lastName,
    Gender gender,
    String phoneNumber,
    String email,
    AddressDto address

) implements Serializable {

}
