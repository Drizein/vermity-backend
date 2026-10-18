package de.vermity.model.dto;

import de.vermity.model.entity.AdditionalCost;
import lombok.NonNull;

import java.io.Serializable;
import java.util.List;

/**
 * @param buildingId
 * @param flatList
 * @param operatingCosts
 * @param address
 * @author Drizein
 */
public record LandlordViewDto(
        int buildingId,
        @NonNull List<LandlordViewFlatDto> flatList,
        @NonNull List<AdditionalCost> operatingCosts,
        @NonNull AddressDto address

) implements Serializable {

}
