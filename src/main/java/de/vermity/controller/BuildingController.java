package de.vermity.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import de.vermity.model.dto.BuildingDto;
import de.vermity.model.dto.ModifyBuildingDto;
import de.vermity.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handles:
 * <li>Building creation</li>
 * <li>Building deletion</li>
 * <li>Building modification</li>
 * <li>Building retrieval</li>
 * <li>Building retrieval for landlord</li>
 *
 * @author Jan Tiedt
 * @author Cedric Stumpf
 */
@RestController
@Slf4j
@Tag(name = "Building", description = "handles all building related stuff")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BuildingController {

  private final BuildingService buildingService;

  /**
   * @param buildingDto   BuildingDto with password for validation
   * @return Error or success messages
   * @author Jan Tiedt
   */
  @PostMapping(value = "createBuilding", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Create a new building")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Building created"),
      @ApiResponse(responseCode = "400", description = "Building, Address, Meter already exists"),
      @ApiResponse(responseCode = "401", description = "Not authorized"),
  })
  ResponseEntity<String> createBuilding(@RequestBody BuildingDto buildingDto,
      @RequestHeader String Authorization) {
    log.info("Creating building: '{}'", buildingDto.toString());
    return buildingService.createBuilding(buildingDto, Authorization);
  }

  /**
   * @param Authorization JWT
   * @return all buildings with flats and tenants for landlord
   * @author Cedric Stumpf
   * @see BuildingService#getAllBuildings(String)
   */
  @GetMapping(value = "getAllBuildings", produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "retrieve all buildings")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Building created"),
      @ApiResponse(responseCode = "401", description = "Not authorized"),
  })
  ResponseEntity<?> getAllBuildings(@RequestHeader String Authorization) {
    log.info("retrieving all buildings");
    return buildingService.getAllBuildings(Authorization);
  }

  /**
   * @param Authorization JWT
   * @return all buildings with flats and tenants for landlord
   * @author Cedric Stumpf
   * @see BuildingService#getAllBuildingsLandlordView(String)
   */
  @GetMapping(value = "getAllBuildingsLandlordView", produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "retrieve all buildings")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Building created"),
      @ApiResponse(responseCode = "401", description = "Not authorized"),
  })
  ResponseEntity<?> getAllBuildingsLandlordView(@RequestHeader String Authorization) {
    log.info("retrieving all buildings for landlord");
    return buildingService.getAllBuildingsLandlordView(Authorization);
  }


  /**
   * @param Authorization JWT
   * @param buildingDto   BuildingDto with password for validation
   * @return Error or success messages
   * @author Jan Tiedt
   */
  @DeleteMapping(value = "deleteBuilding", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "delete a building")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Building deleted"),
      @ApiResponse(responseCode = "400", description = "Building not found"),
      @ApiResponse(responseCode = "401", description = "Not authorized"),
  })
  ResponseEntity<String> deleteBuilding(@RequestBody BuildingDto buildingDto,
      @RequestHeader String Authorization) {
    return buildingService.deleteBuilding(buildingDto, Authorization);
  }

  /**
   * @param Authorization     JWT
   * @param modifyBuildingDto ModifyBuildingDto with password for validation
   * @return Error or success messages
   * @author Jan Tiedt
   */
  @PatchMapping(value = "modifyBuilding", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "modify a building")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Building modified"),
      @ApiResponse(responseCode = "400", description = "Building not found"),
      @ApiResponse(responseCode = "401", description = "Not authorized"),
  })
  ResponseEntity<?> modifyBuilding(@RequestBody ModifyBuildingDto modifyBuildingDto,
      @RequestHeader String Authorization) {
    return buildingService.modifyBuilding(modifyBuildingDto, Authorization);
  }

}
