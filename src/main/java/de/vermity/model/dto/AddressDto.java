package de.vermity.model.dto;


import java.io.Serializable;
import lombok.NonNull;

/**
 * DTO for {@link de.vermity.model.entity.Address}
 *
 * @author Cedric Stumpf
 */
public record AddressDto(
    @NonNull String street,
    int zip,
    @NonNull String city,
    @NonNull String country,
    @NonNull String state
) implements Serializable {

}
