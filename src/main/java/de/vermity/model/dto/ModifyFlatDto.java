package de.vermity.model.dto;


import java.io.Serializable;

/**
 * @param buildingId         Id of the building
 * @param flatId             Id of the flat
 * @param newTenantEmail     Email of the new tenant. Can be empty, but then first and last name
 *                           must be set
 * @param newTenantFirstName First name of the new tenant. Can be empty, but then email must be set
 * @param newTenantLastName  Last name of the new tenant. Can be empty, but then email must be set
 * @author Cedric Stumpf
 */
public record ModifyFlatDto(
    int buildingId,
    int flatId,
    String newTenantEmail,
    String newTenantFirstName,
    String newTenantLastName,
    int residents
) implements Serializable {

}
