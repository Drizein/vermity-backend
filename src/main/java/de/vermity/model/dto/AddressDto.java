package de.vermity.model.dto;


import lombok.NonNull;

import java.io.Serializable;

/**
 * DTO for {@link de.vermity.model.entity.Address}
 *
 * @author Drizein
 */
public record AddressDto(
        @NonNull String street,
        int zip,
        @NonNull String city,
        @NonNull String country,
        @NonNull String state
) implements Serializable {

}
