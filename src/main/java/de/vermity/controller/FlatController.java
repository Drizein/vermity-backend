package de.vermity.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import de.vermity.model.dto.ModifyFlatDto;
import de.vermity.model.dto.UpdateMeterReadingDto;
import de.vermity.service.FlatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handles:
 * <li>Update the tenant of a flat</li>
 * <li>Get the flat of a tenant</li>
 * <li>Update the reading of a meter of a flat</li>
 *
 * @author Cedric Stumpf
 */
@RestController
@Slf4j
@Tag(name = "Flat", description = "handles all flat related stuff")
@RequiredArgsConstructor
@RequestMapping("/auth")
public class FlatController {

  private final FlatService flatService;

  /**
   * Update the tenant of a flat
   *
   * @param modifyFlatDto DTO for updating the tenant of a flat
   * @param Authorization the token of the user
   * @return ResponseEntity with the status of the update
   * @author Cedric Stumpf
   * @see ModifyFlatDto
   */
  @PatchMapping(value = "updateTenant", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Update the tenant of a flat")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Tenant updated"),
      @ApiResponse(responseCode = "400", description = "Building, Flat, Tenant not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  ResponseEntity<String> updateTenant(@RequestBody ModifyFlatDto modifyFlatDto,
      @RequestHeader String Authorization) {
    log.info("Updating tenant: '{}'", modifyFlatDto.toString());
    return flatService.updateTenant(modifyFlatDto, Authorization);
  }

  /**
   * Get the flat of a tenant
   *
   * @param Authorization the token of the user
   * @return ResponseEntity with the flat of the tenant or an error message
   * @author Cedric Stumpf
   */
  @GetMapping(value = "getFlat", produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "Get flat by tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Flat found"),
      @ApiResponse(responseCode = "400", description = "Flat not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  ResponseEntity<?> getFlat(@RequestHeader String Authorization) {
    log.info("Getting flat");
    return flatService.getFlat(Authorization);
  }

  /**
   * Update the reading of a meter of a flat
   *
   * @param updateMeterReadingDto DTO for updating the reading of a meter
   * @param Authorization         the token of the user
   * @return ResponseEntity with the status of the update or an error message
   * @author Cedric Stumpf
   */
  @PatchMapping(value = "updateMeterReading", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Update the reading of a meter of a flat")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Meter reading updated"),
      @ApiResponse(responseCode = "400", description = "Flat, Meter not found or meter reading is less than the last one"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  ResponseEntity<?> updateMeterReading(@RequestBody UpdateMeterReadingDto updateMeterReadingDto,
      @RequestHeader String Authorization) {
    log.info("Updating meter reading: '{}'", updateMeterReadingDto.toString());
    return flatService.updateMeterReading(updateMeterReadingDto, Authorization);
  }

  /**
   * Get landlord of a flat
   *
   * @param Authorization the token of the user
   * @param flatId        the id of the flat
   * @return ResponseEntity with the landlord or an error message
   * @author Cedric Stumpf
   */
  @GetMapping(value = "getLandlordByFlat", produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "Get landlord by flatId")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Landlord found"),
      @ApiResponse(responseCode = "400", description = "Flat/Building not found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  ResponseEntity<?> getLandlordByFlat(@RequestHeader int flatId,
      @RequestHeader String Authorization) {
    log.info("Getting landlord of flat");
    return flatService.getLandlordByFlat(flatId, Authorization);
  }

}
