package de.vermity.model.dto;

import de.vermity.util.enums.MeterType;
import java.io.Serializable;
import lombok.NonNull;

/**
 * @param reading
 * @param meterNr
 * @param type    DTO for {@link de.vermity.model.entity.Meter}
 * @author Cedric Stumpf
 */
public record MeterDto(
    int meterId,
    int reading,
    @NonNull String meterNr,
    @NonNull MeterType type,
    double costPerUnit,
    double baseCost
)
    implements Serializable {

}
