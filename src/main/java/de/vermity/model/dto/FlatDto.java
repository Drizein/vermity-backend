package de.vermity.model.dto;

import de.vermity.model.entity.AdditionalCost;
import java.io.Serializable;
import java.util.List;
import lombok.NonNull;

/**
 * DTO for {@link de.vermity.model.entity.Flat}
 *
 * @param flatId
 * @param meterList
 * @param location
 * @param rooms
 * @param squareMeter
 * @param residents
 * @param additionList
 * @param coldRent
 * @param warmRent
 * @param invoiceList
 * @author Cedric Stumpf
 */
public record FlatDto(
    int flatId,
    @NonNull List<MeterDto> meterList,
    @NonNull String location,
    int rooms,
    int squareMeter,
    int residents,
    @NonNull List<AdditionalCost> additionList,
    double coldRent,
    double warmRent,
    List<InvoiceDto> invoiceList
) implements Serializable {

}
