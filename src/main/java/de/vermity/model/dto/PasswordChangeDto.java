package de.vermity.model.dto;

import java.io.Serializable;
import lombok.NonNull;

/**
 * DTO for changing the password of a {@link de.vermity.model.entity.Person}
 *
 * @param oldPassword
 * @param newPassword
 * @author Cedric Stumpf
 */
public record PasswordChangeDto(
    @NonNull String oldPassword,
    @NonNull String newPassword
) implements Serializable {

}
