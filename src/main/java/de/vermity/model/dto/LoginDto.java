package de.vermity.model.dto;

import lombok.NonNull;

import java.io.Serializable;

/**
 * DTO for {@link de.vermity.model.entity.Person}
 *
 * @param email
 * @param password
 * @author Drizein
 */
public record LoginDto(
        @NonNull String email,
        @NonNull String password

) implements Serializable {

}
