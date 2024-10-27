package de.vermity.model.dto;

import de.vermity.model.entity.AdditionalCost;
import java.io.Serializable;
import java.util.List;
import lombok.NonNull;

/**
 * @param buildingId
 * @param flatList
 * @param operatingCosts
 * @param address
 * @author Cedric Stumpf
 */
public record LandlordViewDto(
    int buildingId,
    @NonNull List<LandlordViewFlatDto> flatList,
    @NonNull List<AdditionalCost> operatingCosts,
    @NonNull AddressDto address

) implements Serializable {

}
