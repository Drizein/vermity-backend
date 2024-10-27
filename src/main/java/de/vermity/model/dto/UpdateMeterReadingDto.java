package de.vermity.model.dto;

import java.io.Serializable;

/**
 * DTO for updating a meter reading
 *
 * @param flatId     the id of the flat
 * @param meterId    the id of the meter
 * @param newReading the new reading
 * @author Cedric Stumpf
 */
public record UpdateMeterReadingDto(int flatId, int meterId, int newReading)
    implements Serializable {

}
