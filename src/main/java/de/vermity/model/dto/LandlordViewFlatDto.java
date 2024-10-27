package de.vermity.model.dto;

import de.vermity.model.entity.AdditionalCost;
import java.io.Serializable;
import java.util.List;
import lombok.NonNull;

/**
 * @param flatId
 * @param tenant
 * @param meterList
 * @param location
 * @param rooms
 * @param squareMeter
 * @param residents
 * @param additionList
 * @param coldRent
 * @param warmRent
 * @author Cedric Stumpf
 */
public record LandlordViewFlatDto(
    int flatId,
    LandlordViewPersonDto tenant,
    @NonNull List<MeterDto> meterList,
    @NonNull String location,
    int rooms,
    int squareMeter,
    int residents,
    @NonNull List<AdditionalCost> additionList,
    double coldRent,
    double warmRent,
    @NonNull List<InvoiceDto> invoiceList
) implements Serializable {

}
