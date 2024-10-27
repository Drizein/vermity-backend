package de.vermity.service;

import de.vermity.model.dto.AddressDto;
import de.vermity.model.dto.BuildingDto;
import de.vermity.model.dto.FlatDto;
import de.vermity.model.dto.InvoiceDto;
import de.vermity.model.dto.LandlordViewDto;
import de.vermity.model.dto.LandlordViewFlatDto;
import de.vermity.model.dto.LandlordViewPersonDto;
import de.vermity.model.dto.MeterDto;
import de.vermity.model.dto.ModifyBuildingDto;
import de.vermity.model.entity.AdditionalCost;
import de.vermity.model.entity.Address;
import de.vermity.model.entity.Building;
import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Meter;
import de.vermity.model.entity.Person;
import de.vermity.model.entity.Update;
import de.vermity.persistence.AdditionalCostRepository;
import de.vermity.persistence.AddressRepository;
import de.vermity.persistence.BuildingRepository;
import de.vermity.persistence.FlatRepository;
import de.vermity.persistence.MeterRepository;
import de.vermity.persistence.PersonRepository;
import de.vermity.persistence.UpdateRepository;
import de.vermity.security.JWTGenerator;
import de.vermity.util.UserUtil;
import de.vermity.util.enums.Role;
import io.jsonwebtoken.JwtException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handles:
 * <li>Building creation
 * <li>Building deletion
 * <li>Building modification
 * <li>Getting all buildings
 * <li>Getting all buildings for landlord view
 *
 * @author Jan Tiedt
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BuildingService {

  private final BuildingRepository buildingRepository;
  private final JWTGenerator jwtGenerator;
  private final FlatRepository flatRepository;
  private final MeterRepository meterRepository;
  private final AddressRepository addressRepository;
  private final UserUtil userUtil;
  private final PersonRepository personRepository;
  private final AdditionalCostRepository additionalCostRepository;
  private final UpdateRepository updateRepository;

  /**
   * Get all buildings build into BuildingDto
   *
   * @param buildingList List of buildings
   * @return List of BuildingDto
   * @author Cedric Stumpf
   */
  private static List<BuildingDto> getBuildingDtoList(List<Building> buildingList) {
    return buildingList.stream().map(building -> new BuildingDto(building.getId(),
        building.getFlatList().stream().map(flat -> new FlatDto(flat.getId(),
            flat.getMeterList().stream().map(
                meter -> new MeterDto(meter.getId(), meter.getReading(), meter.getMeterNr(),
                    meter.getType(), meter.getCostPerUnit(), meter.getBaseCost())).toList(),
            flat.getLocation(), flat.getRooms(), flat.getSquareMeter(), flat.getResidents(),
            flat.getAdditionList(), flat.getColdRent(), flat.getWarmRent(),
            flat.getInvoiceList().stream()
                .map(invoice -> new InvoiceDto(invoice.getId(), invoice.isPaid(), invoice.getPdf()))
                .toList())).toList(), building.getOperatingCosts(),
        new AddressDto(building.getAddress().getStreet(), building.getAddress().getZip(),
            building.getAddress().getCity(), building.getAddress().getCountry(),
            building.getAddress().getState()))).toList();
  }

  /**
   * Build BuildingDto from Building
   *
   * @param savedBuilding Building to build
   * @return BuildingDto from Building
   * @author Cedric Stumpf
   */
  private static BuildingDto buildBuildingDto(Optional<Building> savedBuilding) {
    return new BuildingDto(savedBuilding.get().getId(), savedBuilding.get().getFlatList().stream()
        .map(flat -> new FlatDto(flat.getId(), flat.getMeterList().stream().map(
            meter -> new MeterDto(meter.getId(), meter.getReading(), meter.getMeterNr(),
                meter.getType(), meter.getCostPerUnit(), meter.getBaseCost())).toList(),
            flat.getLocation(), flat.getRooms(), flat.getSquareMeter(), flat.getResidents(),
            flat.getAdditionList(), flat.getColdRent(), flat.getWarmRent(),
            flat.getInvoiceList().stream()
                .map(invoice -> new InvoiceDto(invoice.getId(), invoice.isPaid(), invoice.getPdf()))
                .toList())).toList(), savedBuilding.get().getOperatingCosts(),
        new AddressDto(savedBuilding.get().getAddress().getStreet(),
            savedBuilding.get().getAddress().getZip(), savedBuilding.get().getAddress().getCity(),
            savedBuilding.get().getAddress().getCountry(),
            savedBuilding.get().getAddress().getState()));
  }

  /**
   * @param buildingDto Building to create
   * @return Status code
   * @author Jan Tiedt, Cedric Stumpf
   */
  public ResponseEntity<String> createBuilding(BuildingDto buildingDto, String bearerToken) {
    Optional<Person> user;
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()) {
        log.warn("User with token '{}' not found", jwtGenerator.extractToken(bearerToken));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Bitte melde dich zuerst an!");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }

    // Check for duplicate Building, if known return BadRequest
    log.info("Creating new building");
    if (buildingRepository.findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
        buildingDto.address().street(), buildingDto.address().city(), buildingDto.address().state(),
        buildingDto.address().zip(), buildingDto.address().country()).isPresent()) {
      return ResponseEntity.badRequest().body("Gebäude an dieser Adresse existiert bereits.");
    }

    // persist new flats and meters
    // checking for duplicate meter
    if (!meterRepository.findByMeterNrIn(
        buildingDto.flatList().stream().flatMap(flat -> flat.meterList().stream())
            .map(MeterDto::meterNr).toList()).isEmpty()) {
      log.info("Meter already registered!");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Diese Zählernummer existiert bereits.");
    }

    log.info("Saving flats");
    var flats = buildingDto.flatList().stream().map(flat -> {
      var meterList = flat.meterList().stream().map(
          meter -> Meter.builder().meterNr(meter.meterNr()).reading(meter.reading())
              .type(meter.type()).costPerUnit(meter.costPerUnit()).baseCost(meter.baseCost())
              .build()).toList();
      log.info("Saving meters");
      meterList = meterRepository.saveAllAndFlush(meterList);
      meterList.forEach(m -> {
        Update update = Update.builder().meter(m).reading(m.getReading()).build();
        m.getUpdateList().add(update);
        updateRepository.saveAndFlush(update);
      });
      meterList = meterRepository.saveAllAndFlush(meterList);
      var additionList = flat.additionList().stream().map(
              addition -> AdditionalCost.builder().name(addition.getName())
                  .description(addition.getDescription()).amount(addition.getAmount())
                  .distribution(addition.getDistribution()).frequency(addition.getFrequency()).build())
          .toList();
      additionList = additionalCostRepository.saveAllAndFlush(additionList);

      return Flat.builder().rooms(flat.rooms()).squareMeter(flat.squareMeter())
          .location(flat.location()).warmRent(flat.warmRent()).coldRent(flat.coldRent())
          .additionList(additionList).meterList(meterList).residents(flat.residents()).build();
    }).toList();
    var persistedFlats = flatRepository.saveAllAndFlush(flats);

    // persist building
    var building = Building.builder().landlord(user.get()).operatingCosts(
            additionalCostRepository.saveAllAndFlush(buildingDto.operatingCosts().stream().map(
                operatingCost -> AdditionalCost.builder().name(operatingCost.getName())
                    .description(operatingCost.getDescription()).amount(operatingCost.getAmount())
                    .distribution(operatingCost.getDistribution())
                    .frequency(operatingCost.getFrequency()).build()).toList()))
        .flatList(persistedFlats).build();

    var address = addressRepository.findByStateAndCityAndStreetAndZipAndCountry(
        buildingDto.address().state(), buildingDto.address().city(), buildingDto.address().street(),
        buildingDto.address().zip(), buildingDto.address().country());

    if (address.isPresent()) {
      building.setAddress(address.get());
    } else {
      building.setAddress(addressRepository.saveAndFlush(
          Address.builder().street(buildingDto.address().street()).zip(buildingDto.address().zip())
              .city(buildingDto.address().city()).country(buildingDto.address().country())
              .state(buildingDto.address().state()).build()));
    }

    log.info("Saving building");
    buildingRepository.saveAndFlush(building);

    if (!user.get().getRoleList().contains(Role.LANDLORD)) {
      List<Role> mutableRoleList = new ArrayList<>(user.get().getRoleList());
      mutableRoleList.add(Role.LANDLORD);
      user.get().setRoleList(mutableRoleList);
      personRepository.saveAndFlush(user.get());
      return ResponseEntity.ok("Gebäude erstellt und dir die Rolle 'Vermieter zugewiesen.");
    }
    return ResponseEntity.ok("Gebäude erstellt.");
  }

  /**
   * Get all buildings
   *
   * @param bearerToken Token of the user
   * @return List of all buildings or message if not successful
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getAllBuildings(String bearerToken) {
    try {
      if (userUtil.verifyUser(bearerToken).isEmpty()) {
        log.warn("User with token '{}' not found", jwtGenerator.extractToken(bearerToken));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Bitte melde dich zuerst an!");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }
    List<Building> buildingList = buildingRepository.findAll();

    var buildingDtoList = getBuildingDtoList(buildingList);

    return ResponseEntity.ok().body(buildingDtoList);
  }

  /**
   * @param buildingDto Building to delete
   * @return Status code
   * @author Jan Tiedt
   */
  public ResponseEntity<String> deleteBuilding(BuildingDto buildingDto, String bearerToken) {
    Optional<Person> user;
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()) {
        log.warn("User with token '{}' not found", jwtGenerator.extractToken(bearerToken));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Bitte melde dich zuerst an!");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }
    var deleteBuilding = buildingRepository.findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
        buildingDto.address().street(), buildingDto.address().city(), buildingDto.address().state(),
        buildingDto.address().zip(), buildingDto.address().country());
    if (deleteBuilding.isEmpty()) {
      log.info("Building not found");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }
    if (!deleteBuilding.get().getLandlord().equals(user.get())) {
      log.info("User '{}' is not the landlord of the building", user.get().getId());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Dies ist nicht dein Gebäude!");
    }
    deleteBuilding.ifPresent(building -> flatRepository.deleteAll(building.getFlatList()));
    buildingRepository.delete(deleteBuilding.get());
    log.info("Building deleted");
    return ResponseEntity.status(HttpStatus.OK).body("Gebäude gelöscht.");
  }

  /**
   * Modify a building
   *
   * @param modifyBuildingDto Building to modify
   * @param bearerToken       Token of the Landlord
   * @return Status code and modified building or message if not successful
   * @author Jan Tiedt
   */

  public ResponseEntity<?> modifyBuilding(ModifyBuildingDto modifyBuildingDto, String bearerToken) {
    Optional<Person> user;
    // verify Token
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()) {
        log.warn("User with token '{}' not found", jwtGenerator.extractToken(bearerToken));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Bitte melde dich zuerst an!");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }

    var savedBuilding = buildingRepository.findById(modifyBuildingDto.buildingId());

    if (savedBuilding.isEmpty()) {
      log.info("Building not found");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }
    if (!savedBuilding.get().getLandlord().equals(user.get())) {
      log.info("User '{}' is not the landlord of the building", user.get().getId());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Dies ist nicht dein Gebäude!");
    }

    var address = addressRepository.findByStateAndCityAndStreetAndZipAndCountry(
        modifyBuildingDto.address().state(), modifyBuildingDto.address().city(),
        modifyBuildingDto.address().street(), modifyBuildingDto.address().zip(),
        modifyBuildingDto.address().country());

    if (address.isPresent() && !address.get().equals(savedBuilding.get().getAddress())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("An dieser Adresse existiert bereits ein Gebäude!");
    }

    modifyFlats(modifyBuildingDto, savedBuilding);
    savedBuilding = buildingRepository.findById(savedBuilding.get().getId());
    savedBuilding = addNewFlatToBuilding(modifyBuildingDto, savedBuilding);
    try {
      savedBuilding = addNewMeterToFlat(modifyBuildingDto, savedBuilding);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Zählernummer existiert bereits!");
    }

    if (address.isEmpty()) {
      savedBuilding.get().setAddress(addressRepository.saveAndFlush(
          Address.builder().street(modifyBuildingDto.address().street())
              .zip(modifyBuildingDto.address().zip()).city(modifyBuildingDto.address().city())
              .country(modifyBuildingDto.address().country())
              .state(modifyBuildingDto.address().state()).build()));
    }

    var newOperationCosts = additionalCostRepository.saveAllAndFlush(
        modifyBuildingDto.operatingCosts());
    savedBuilding.get().setOperatingCosts(newOperationCosts);

    BuildingDto modifiedBuilding = buildBuildingDto(savedBuilding);

    return ResponseEntity.status(HttpStatus.OK).body(modifiedBuilding);
  }

  /**
   * Add new meter to flat
   *
   * @param modifyBuildingDto ModifyBuildingDto
   * @param savedBuilding     Building
   * @return Optional of Building with new meter or savedBuilding if no new meter is added
   * @author Cedric Stumpf
   */
  private Optional<Building> addNewMeterToFlat(ModifyBuildingDto modifyBuildingDto,
      Optional<Building> savedBuilding) throws RuntimeException {

    // Extract meter numbers from ModifyBuildingDto
    List<String> newMeterNumbers = modifyBuildingDto.flatList().stream()
        .flatMap(flat -> flat.meterList().stream())
        .map(MeterDto::meterNr)
        .filter(s -> savedBuilding.get().getFlatList().stream()
            .flatMap(f -> f.getMeterList().stream())
            .noneMatch(savedMeter -> s.equals(savedMeter.getMeterNr())))
        .toList();

    if (!meterRepository.findByMeterNrIn(newMeterNumbers).isEmpty()) {
      log.warn("Meter number already exists!");
      throw new RuntimeException("Meter number already exists!");
    }

    modifyBuildingDto.flatList().forEach(flat -> flat.meterList().stream().filter(
            modifyMeter -> savedBuilding.get().getFlatList().stream()
                .flatMap(f -> f.getMeterList().stream())
                .noneMatch(savedMeter -> modifyMeter.meterNr().equals(savedMeter.getMeterNr())))
        .forEach(modifyMeter -> {
          Meter newMeter = Meter.builder().meterNr(modifyMeter.meterNr())
              .reading(modifyMeter.reading()).type(modifyMeter.type())
              .costPerUnit(modifyMeter.costPerUnit()).baseCost(modifyMeter.baseCost())
              .costPerUnit(modifyMeter.costPerUnit()).build();
          Meter meter = meterRepository.saveAndFlush(newMeter);
          Update update = Update.builder().meter(meter).reading(meter.getReading()).build();
          meter.getUpdateList().add(update);
          updateRepository.saveAndFlush(update);
          meter = meterRepository.saveAndFlush(meter);
          savedBuilding.get().getFlatList().stream().filter(f -> f.getId().equals(flat.flatId()))
              .findFirst().get().getMeterList().add(meter);
        }));
    return savedBuilding;
  }

  /**
   * Add new flat to building
   *
   * @param modifyBuildingDto ModifyBuildingDto
   * @param savedBuilding     Building
   * @return Optional of Building with new flat or savedBuilding if no new flat is added
   * @author Cedric Stumpf
   */
  private Optional<Building> addNewFlatToBuilding(ModifyBuildingDto modifyBuildingDto,
      Optional<Building> savedBuilding) {
    if (modifyBuildingDto.flatList().size() == savedBuilding.get().getFlatList().size()) {
      log.info("No new flats to add");
      return savedBuilding;
    }
    log.info("Add of a new flat is requested");

    // Identify new flats
    Building finalSavedBuilding = savedBuilding.get();
    var mutableFlatList = new ArrayList<>(finalSavedBuilding.getFlatList());
    List<FlatDto> newFlats = modifyBuildingDto.flatList().stream().filter(
        modifyFlat -> mutableFlatList.stream()
            .noneMatch(savedFlat -> savedFlat.getId().equals(modifyFlat.flatId()))).toList();

    // Add new flats to savedBuilding
    for (FlatDto newFlatDto : newFlats) {
      Flat newFlat = Flat.builder().rooms(newFlatDto.rooms()).squareMeter(newFlatDto.squareMeter())
          .location(newFlatDto.location()).warmRent(newFlatDto.warmRent())
          .coldRent(newFlatDto.coldRent())
          .additionList(newFlatDto.additionList().stream().map(addition -> {
            var addCost = AdditionalCost.builder().name(addition.getName())
                .description(addition.getDescription()).amount(addition.getAmount())
                .distribution(addition.getDistribution()).frequency(addition.getFrequency())
                .build();
            return additionalCostRepository.saveAndFlush(addCost);
          }).toList()).meterList(newFlatDto.meterList().stream().map(meter -> {
            var newMeter = Meter.builder().meterNr(meter.meterNr()).reading(meter.reading())
                .type(meter.type()).costPerUnit(meter.costPerUnit()).baseCost(meter.baseCost())
                .build();
            return meterRepository.saveAndFlush(newMeter);
          }).toList()).build();
      Flat flat = flatRepository.saveAndFlush(newFlat);
      mutableFlatList.add(flat);
    }
    finalSavedBuilding.setFlatList(mutableFlatList);
    // Save the updated building
    return Optional.of(finalSavedBuilding);
  }

  /**
   * Modify flats
   *
   * @param modifyBuildingDto ModifyBuildingDto
   * @param savedBuilding     Building
   * @author Cedric Stumpf
   */
  private void modifyFlats(ModifyBuildingDto modifyBuildingDto, Optional<Building> savedBuilding) {
    savedBuilding.get().getFlatList()
        .forEach(flat -> modifyBuildingDto.flatList().forEach(modifyFlat -> {
          // Check if any meter matches
          boolean meterMatch = modifyFlat.meterList().stream().anyMatch(
              modifyMeter -> flat.getMeterList().stream()
                  .anyMatch(meter -> modifyMeter.meterNr().equals(meter.getMeterNr())));

          // find all new additions in modifyFlat for the flat
          List<AdditionalCost> newAdditions = modifyFlat.additionList().stream().filter(
                  addition -> flat.getAdditionList().stream()
                      .noneMatch(additionalCost -> additionalCost.getName().equals(addition.getName())))
              .map(addition -> AdditionalCost.builder().name(addition.getName())
                  .description(addition.getDescription()).amount(addition.getAmount())
                  .distribution(addition.getDistribution()).frequency(addition.getFrequency())
                  .build()).toList();

          var mutableAdditionList = new ArrayList<>(flat.getAdditionList());
          mutableAdditionList.addAll(newAdditions);

          // If a match is found, update the flat properties
          if (meterMatch) {
            modifyFlat.meterList().forEach(modifyMeter -> flat.getMeterList().forEach(meter -> {
              if (modifyMeter.meterNr().equals(meter.getMeterNr())) {
                Update update = Update.builder().meter(meter).reading(modifyMeter.reading())
                    .person(savedBuilding.get().getLandlord()).build();
                meter.getUpdateList().add(updateRepository.saveAndFlush(update));
                meter.setReading(modifyMeter.reading());
                meterRepository.saveAndFlush(meter);
              }
            }));

            flat.setSquareMeter(modifyFlat.squareMeter());
            flat.setLocation(modifyFlat.location());
            flat.setRooms(modifyFlat.rooms());
            flat.setResidents(modifyFlat.residents());
            flat.setColdRent(modifyFlat.coldRent());
            flat.setWarmRent(modifyFlat.warmRent());
            flat.setAdditionList(mutableAdditionList);
            flat.setUpdatedAt();
            flatRepository.saveAndFlush(flat);
          }
        }));
  }

  /**
   * Get all buildings for landlord view
   *
   * @param bearerToken Token of the Landlord
   * @return List of all buildings of the landlord view or message if not successful
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getAllBuildingsLandlordView(String bearerToken) {
    Optional<Person> user;
    // verify Token
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()) {
        log.warn("User with token '{}' not found", jwtGenerator.extractToken(bearerToken));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Bitte melde dich zuerst an!");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User '{}' not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }

    List<Building> buildingList = buildingRepository.findByLandlord(user.get());

    var buildingDtoList = buildingList.stream().map(
        building -> new LandlordViewDto(building.getId(),
            building.getFlatList().stream().map(flat -> {
              LandlordViewPersonDto tenantDto = null;
              if (flat.getTenant() != null) {
                tenantDto = new LandlordViewPersonDto(flat.getTenant().getFirstName(),
                    flat.getTenant().getLastName(), flat.getTenant().getGender(),
                    flat.getTenant().getPhoneNumber(), flat.getTenant().getEmail(),
                    new AddressDto(flat.getTenant().getAddress().getStreet(),
                        flat.getTenant().getAddress().getZip(),
                        flat.getTenant().getAddress().getCity(),
                        flat.getTenant().getAddress().getCountry(),
                        flat.getTenant().getAddress().getState()));
              }
              return new LandlordViewFlatDto(flat.getId(), tenantDto, flat.getMeterList().stream()
                  .map(meter -> new MeterDto(meter.getId(), meter.getReading(), meter.getMeterNr(),
                      meter.getType(), meter.getCostPerUnit(), meter.getBaseCost())).toList(),
                  flat.getLocation(), flat.getRooms(), flat.getSquareMeter(), flat.getResidents(),
                  flat.getAdditionList(), flat.getColdRent(), flat.getWarmRent(),
                  flat.getInvoiceList().stream().map(
                      invoice -> new InvoiceDto(invoice.getId(), invoice.isPaid(),
                          invoice.getPdf())).toList());
            }).toList(), building.getOperatingCosts(),
            new AddressDto(building.getAddress().getStreet(), building.getAddress().getZip(),
                building.getAddress().getCity(), building.getAddress().getCountry(),
                building.getAddress().getState()))).toList();

    return ResponseEntity.ok().body(buildingDtoList);
  }
}
