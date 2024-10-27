package de.vermity.service;

import de.vermity.model.dto.AddressDto;
import de.vermity.model.dto.FlatDto;
import de.vermity.model.dto.InvoiceDto;
import de.vermity.model.dto.LandlordViewPersonDto;
import de.vermity.model.dto.MeterDto;
import de.vermity.model.dto.ModifyFlatDto;
import de.vermity.model.dto.UpdateMeterReadingDto;
import de.vermity.model.entity.Person;
import de.vermity.model.entity.Update;
import de.vermity.persistence.BuildingRepository;
import de.vermity.persistence.FlatRepository;
import de.vermity.persistence.MeterRepository;
import de.vermity.persistence.PersonRepository;
import de.vermity.persistence.UpdateRepository;
import de.vermity.security.JWTGenerator;
import de.vermity.util.UserUtil;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service handles:
 * <li>Flat creation
 * <li>Flat update
 * <li>Flat getting
 *
 * @author Cedric Stumpf
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlatService {

  private final UpdateRepository updateRepository;
  private final BuildingRepository buildingRepository;
  private final PersonRepository personRepository;
  private final JWTGenerator jwtGenerator;
  private final FlatRepository flatRepository;
  private final MeterRepository meterRepository;
  private final UserUtil userUtil;

  /**
   * Landlord can update the tenant of a flat.
   *
   * @param modifyFlatDto The dto with the new tenant information
   * @param bearerToken   The token of the landlord
   * @return ResponseEntity with message if successful or not
   * @author Cedric Stumpf
   */
  public ResponseEntity<String> updateTenant(ModifyFlatDto modifyFlatDto, String bearerToken) {
    // Get user from token
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

    // Get building from repository
    var building = buildingRepository.findById(modifyFlatDto.buildingId());
    if (building.isEmpty()) {
      log.warn("Building with id '{}' not found", modifyFlatDto.buildingId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }

    // Check if user is landlord of building
    if (!building.get().getLandlord().getId().equals(user.get().getId())) {
      log.warn("User '{}' is not the landlord of building '{}'", user.get().getEmail(),
          building.get().getId());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Du bist nicht der Vermieter dieses Gebäudes");
    }

    // Get new tenant from repository, when provided with new tenant email
    Optional<Person> newTenant;
    if (!modifyFlatDto.newTenantEmail().isBlank()) {
      newTenant = personRepository.findByEmail(modifyFlatDto.newTenantEmail());
      if (newTenant.isEmpty()) {
        log.warn("New tenant with email '{}' not found", modifyFlatDto.newTenantEmail());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            "Neuen Mieter nicht gefunden. Bitte gib eine Vor- und Nachnamen an.");
      } // create new "simple" tenant with first and last name
    } else if (!modifyFlatDto.newTenantFirstName().isBlank() && !modifyFlatDto.newTenantLastName()
        .isBlank()) {
      newTenant = Optional.of(personRepository.saveAndFlush(
          Person.builder().lastName(modifyFlatDto.newTenantLastName())
              .firstName(modifyFlatDto.newTenantFirstName()).address(building.get().getAddress())
              .build()));
    } else {
      log.warn("No new tenant provided");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Keinen neuen Mieter angegeben");
    }

    // Get flatList from modifyFlatDto
    var flatList = building.get().getFlatList().stream()
        .filter(f -> f.getId().equals(modifyFlatDto.flatId())).toList();
    if (flatList.isEmpty()) {
      log.warn("Flat with id '{}' not found", modifyFlatDto.flatId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wohnung nicht gefunden");
    }
    // Set new tenant and save to repository
    flatList.getFirst().setTenant(newTenant.get());
    flatList.getFirst().setResidents(modifyFlatDto.residents());
    flatRepository.saveAndFlush(flatList.getFirst());
    log.info("Tenant of flat with id '{}' updated", modifyFlatDto.flatId());
    return ResponseEntity.ok("Mieter aktualisiert");
  }

  /**
   * Get flat by tenant
   *
   * @param bearerToken The token of the user
   * @return ResponseEntity with flatDto if successful or message if not
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getFlat(String bearerToken) {
    // Get user from token
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
    var flat = flatRepository.findByTenant(user.get());
    if (flat.isEmpty()) {
      log.warn("Flat with tenant '{}' not found", user.get().getEmail());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wohnung nicht gefunden");
    }
    return ResponseEntity.ok(flat.stream().map(f -> new FlatDto(f.getId(), f.getMeterList().stream().map(
        meter -> new MeterDto(meter.getId(), meter.getReading(), meter.getMeterNr(),
            meter.getType(), meter.getCostPerUnit(), meter.getBaseCost())).toList(),
        f.getLocation(),
        f.getRooms(), f.getSquareMeter(), f.getResidents(),
        f.getAdditionList(), f.getColdRent(), f.getWarmRent(),
        f.getInvoiceList().stream()
            .map(invoice -> new InvoiceDto(invoice.getId(), invoice.isPaid(), invoice.getPdf()))
            .toList())).toList());
  }

  /**
   * Update the reading of a meter of a flat.
   *
   * @return ResponseEntity with message if successful or not
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> updateMeterReading(UpdateMeterReadingDto updateMeterReadingDto,
      String bearerToken) {
    // Get user from token
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
    var flat = flatRepository.findById(updateMeterReadingDto.flatId());
    if (flat.isEmpty()) {
      log.warn("Flat with id '{}' not found", updateMeterReadingDto.flatId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wohnung nicht gefunden");
    }

    if (!flat.get().getTenant().getId().equals(user.get().getId())) {
      log.warn("User '{}' is not the tenant of flat with id '{}'", user.get().getEmail(),
          updateMeterReadingDto.flatId());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Du bist nicht der Mieter dieser Wohnung");
    }

    var meter = meterRepository.findById(updateMeterReadingDto.meterId());
    if (meter.isEmpty()) {
      log.warn("Meter with id '{}' not found", updateMeterReadingDto.meterId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Zähler nicht gefunden");
    }
    if (!flat.get().getMeterList().contains(meter.get())) {
      log.warn("Meter with id '{}' not found in flat with id '{}'",
          updateMeterReadingDto.meterId(), updateMeterReadingDto.flatId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Zähler nicht in Wohnung gefunden");
    }
    if (meter.get().getReading() > updateMeterReadingDto.newReading()) {
      log.warn("New reading '{}' is lower than current reading '{}'",
          updateMeterReadingDto.newReading(), meter.get().getReading());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Neuer Zählerstand ist niedriger als der aktuelle Zählerstand");
    }

    Update update = Update.builder().meter(meter.get()).reading(updateMeterReadingDto.newReading())
        .person(user.get()).build();
    meter.get().getUpdateList().add(updateRepository.saveAndFlush(update));
    meter.get().setReading(updateMeterReadingDto.newReading());
    meterRepository.saveAndFlush(meter.get());
    log.info("Meter reading of meter with id '{}' updated", updateMeterReadingDto.meterId());
    return ResponseEntity.ok("Zählerstand aktualisiert");
  }

  /**
   * Get landlord of a flat.
   *
   * @param flatId      The id of the flat
   * @param bearerToken The token of the tenant
   * @return ResponseEntity with landlordDto if successful or message if not
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getLandlordByFlat(int flatId, String bearerToken) {
    // Get user from token
    Optional<Person> user;
    try {
      user = userUtil.verifyUser(bearerToken);
      if (user.isEmpty()) {
        log.warn("User with token {} not found", jwtGenerator.extractToken(bearerToken));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Bitte melde dich zuerst an!");
      }
    } catch (IllegalArgumentException | JwtException e) {
      log.warn("User {} not found", jwtGenerator.extractToken(bearerToken));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Bitte melde dich zuerst an!");
    }

    var flat = flatRepository.findById(flatId);

    if (flat.isEmpty()) {
      log.warn("Flat with id {} not found", flatId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wohnung nicht gefunden");
    }
    if (!flat.get().getTenant().getId().equals(user.get().getId())) {
      log.warn("User {} is not the tenant of flat with id {}", user.get().getEmail(), flatId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body("Du bist nicht der Mieter dieser Wohnung");
    }

    var building = buildingRepository.findByFlatListContaining(flat.get());
    if (building.isEmpty()) {
      log.warn("Building with flat {} not found", flatId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }

    return ResponseEntity.status(HttpStatus.OK).body(
        new LandlordViewPersonDto(
            building.get().getLandlord().getFirstName(),
            building.get().getLandlord().getLastName(),
            building.get().getLandlord().getGender(),
            building.get().getLandlord().getPhoneNumber(),
            building.get().getLandlord().getEmail(),
            new AddressDto(
                building.get().getLandlord().getAddress().getStreet(),
                building.get().getLandlord().getAddress().getZip(),
                building.get().getLandlord().getAddress().getCity(),
                building.get().getLandlord().getAddress().getCountry(),
                building.get().getLandlord().getAddress().getState())
        ));
  }

}
