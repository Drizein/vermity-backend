package de.vermity.model.dto;

import java.io.Serializable;
import lombok.NonNull;

/**
 * DTO for {@link de.vermity.model.entity.Person}
 *
 * @param email
 * @param password
 * @author Jan Tiedt return
 */
public record LoginDto(
    @NonNull String email,
    @NonNull String password

) implements Serializable {

}
