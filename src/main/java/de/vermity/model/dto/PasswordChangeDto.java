package de.vermity.model.dto;

import lombok.NonNull;

import java.io.Serializable;

/**
 * DTO for changing the password of a {@link de.vermity.model.entity.Person}
 *
 * @param oldPassword
 * @param newPassword
 * @author Drizein
 */
public record PasswordChangeDto(
        @NonNull String oldPassword,
        @NonNull String newPassword
) implements Serializable {

}
