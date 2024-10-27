package de.vermity.model.dto;

import de.vermity.model.entity.AdditionalCost;
import java.io.Serializable;
import java.util.List;
import lombok.NonNull;

/**
 * DTO for {@link de.vermity.model.entity.Building}
 *
 * @param buildingId
 * @param flatList
 * @param operatingCosts
 * @param address
 * @author Jan Tiedt
 */
public record BuildingDto(
    int buildingId,
    @NonNull List<FlatDto> flatList,
    @NonNull List<AdditionalCost> operatingCosts,
    @NonNull AddressDto address
) implements Serializable {

}
