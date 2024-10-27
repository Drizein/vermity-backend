package de.vermity.service;

import de.vermity.model.dto.GetInvoiceDto;
import de.vermity.model.entity.AdditionalCost;
import de.vermity.model.entity.BaseEntity;
import de.vermity.model.entity.Building;
import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Invoice;
import de.vermity.model.entity.Meter;
import de.vermity.model.entity.Person;
import de.vermity.model.entity.Update;
import de.vermity.persistence.BuildingRepository;
import de.vermity.persistence.FlatRepository;
import de.vermity.persistence.InvoiceRepository;
import de.vermity.security.JWTGenerator;
import de.vermity.util.UserUtil;
import de.vermity.util.enums.MeterType;
import io.jsonwebtoken.JwtException;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Util class for creating invoices
 *
 * @author Cedric Stumpf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

  public static final double GASFACTORWARMWATER = 58.15;
  private final UserUtil userUtil;
  private final BuildingRepository buildingRepository;
  private final JWTGenerator jwtGenerator;
  private final InvoiceRepository invoiceRepository;
  private final FlatRepository flatRepository;
  private final SpringTemplateEngine templateEngine;


  /**
   * Calculate the cost per distribution key
   *
   * @param building Building of the flat
   * @param flat     Flat to calculate the cost for
   * @return Map with the cost per distribution key
   * @author Cedric Stumpf
   */
  private static Map<AdditionalCost, Double> calculateCostPerDistributionKey(Building building,
      Flat flat) {
    Map<AdditionalCost, Double> costPerDistributionKey = new HashMap<>();
    // calculate the cost for the flat
    flat.getAdditionList().forEach(operatingCost -> {
      double amount = operatingCost.getAmount() * operatingCost.getFrequency().getFactor();
      costPerDistributionKey.put(operatingCost, amount);
    });

    // calculate the cost for the building
    building.getOperatingCosts().forEach(operatingCost -> {
      double amount = 0;

      if (operatingCost.getDistribution() == null) {
        log.warn("Distribution key for operating cost {} is null", operatingCost);
        return;
      }
      switch (operatingCost.getDistribution()) {
        case FLAT -> {
          log.info("Operating cost {} is distributed by flat", operatingCost);
          amount = (operatingCost.getAmount() / building.getFlatList().size())
              * operatingCost.getFrequency().getFactor();
        }
        case SQUARE_METERS -> {
          log.info("Operating cost {} is distributed by square meters", operatingCost);
          AtomicInteger squareMeterAbsolut = new AtomicInteger();
          building.getFlatList().forEach(f -> squareMeterAbsolut.addAndGet(f.getSquareMeter()));
          amount = operatingCost.getAmount() / squareMeterAbsolut.get() * flat.getSquareMeter()
              * operatingCost.getFrequency().getFactor();
        }
        case PERSON -> {
          log.info("Operating cost {} is distributed by person", operatingCost);
          AtomicInteger personCountAbsolut = new AtomicInteger();
          building.getFlatList().forEach(f -> personCountAbsolut.addAndGet(f.getResidents()));
          amount =
              operatingCost.getAmount() / personCountAbsolut.doubleValue() * flat.getResidents()
                  * operatingCost.getFrequency().getFactor();
        }
        case NONE -> {
          log.info("Operating cost {} is not distributed", operatingCost);
          amount = operatingCost.getAmount() * operatingCost.getFrequency().getFactor();
        }
      }
      costPerDistributionKey.put(operatingCost, amount);
    });
    return costPerDistributionKey;
  }

  /**
   * Calculate the difference of the meters of a flat
   *
   * @param meterList List of meters of the flat
   * @return Map with the meter and the difference
   * @author Cedric Stumpf
   */
  private static Map<Meter, Integer> calculateMeterDifference(List<Meter> meterList) {
    Map<Meter, Integer> meterDifferenceMap = new HashMap<>();
    LocalDateTime startDate = LocalDateTime.now().minusMonths(12);
    LocalDateTime endDate = LocalDateTime.now();

    for (Meter meter : meterList) {
      List<Update> updatesInRange = meter.getUpdateList().stream().filter(
          update -> !update.getCreatedAt().isBefore(startDate) && !update.getCreatedAt()
              .isAfter(endDate)).sorted(Comparator.comparing(BaseEntity::getCreatedAt)).toList();

      if (!updatesInRange.isEmpty()) {
        int startReading = updatesInRange.getFirst().getReading();
        int endReading = updatesInRange.getLast().getReading();
        int difference = endReading - startReading;
        meterDifferenceMap.put(meter, difference);
      }
    }

    return meterDifferenceMap;
  }

  /**
   * Get a list of GetInvoiceDto from a list of invoices
   *
   * @param invoiceList List of invoices
   * @return List of GetInvoiceDto from the invoices list
   * @author Cedric Stumpf
   */
  private static ArrayList<GetInvoiceDto> getGetInvoiceDtos(List<Invoice> invoiceList) {
    var invoiceListDto = new ArrayList<GetInvoiceDto>();
    for (Invoice invoice : invoiceList) {
      invoiceListDto.add(new GetInvoiceDto(invoice.getId(), invoice.getPdf(),
          invoice.getBuilding().getId(), invoice.getFlat().getId(), invoice.isPaid()));
    }
    return invoiceListDto;
  }

  /**
   * Create an invoice for a flat
   *
   * @param building Building of the flat
   * @param flat     Flat to create the invoice for
   * @param totalRentPaid Total rent paid by the tenant
   * @author Cedric Stumpf
   */
  public void createInvoiceForFlat(Building building, Flat flat, double totalRentPaid) {
    log.info("Creating invoice for flat: {}", flat.getId());
    log.info("Tenant: {}", flat.getTenant().getId());

    int totalSquareMeters = building.getFlatList().stream().mapToInt(Flat::getSquareMeter)
        .sum();
    // create invoice
    Invoice invoice = Invoice.builder()
        .building(building)
        .flat(flat)
        .totalColdRent(flat.getColdRent() * 12)
        .invoiceForYear(LocalDate.now().minusYears(1).getYear())
        .operatingCostPerDistributionKey(calculateCostPerDistributionKey(building, flat))
        .totalSquareMeters(totalSquareMeters)
        .totalWarmRentPaid(totalRentPaid)
        .meterDifference(calculateMeterDifference(flat.getMeterList())).build();
    invoice.setMeterTotalCost(calculateMeterTotalCost(invoice.getMeterDifference()));
    invoice.setTotalCost(
        invoice.getMeterTotalCost().values().stream().mapToDouble(Double::doubleValue)
            .sum() + invoice.getOperatingCostPerDistributionKey().values().stream()
            .mapToDouble(Double::doubleValue).sum() + invoice.getTotalColdRent());

    Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
    flat.getInvoiceList().add(savedInvoice);
    flatRepository.saveAndFlush(flat);
    log.info("Invoice {} created", savedInvoice.getId());

    savedInvoice.setPdf(
        Base64.getEncoder().encodeToString(renderInvoiceToHtmlToPDF(savedInvoice).toByteArray()));
    invoiceRepository.saveAndFlush(savedInvoice);
    log.debug("Invoice {} PDF created", savedInvoice.getId());
  }

  /**
   * Render the invoice to HTML and then to PDF
   *
   * @param savedInvoice Invoice to render
   * @return ByteArrayOutputStream with the PDF of the invoice
   * @author Cedric Stumpf
   */
  private ByteArrayOutputStream renderInvoiceToHtmlToPDF(Invoice savedInvoice) {
    Context context = new Context();
    context.setVariable("invoice", savedInvoice);
    context.setLocale(java.util.Locale.GERMANY);

    String htmlContent = templateEngine.process("invoice", context);

    ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
    ITextRenderer renderer = new ITextRenderer();
    renderer.setDocumentFromString(htmlContent);
    renderer.layout();
    renderer.createPDF(pdfOutputStream);
    return pdfOutputStream;
  }

  /**
   * Calculate the total cost of the meters
   *
   * @param meterDifference Map with the meter and the difference
   * @return Map with the meter and the total cost of the meter
   * @author Cedric Stumpf
   */
  private Map<Meter, Double> calculateMeterTotalCost(Map<Meter, Integer> meterDifference) {
    Map<Meter, Double> meterTotalCostMap = new HashMap<>();
    meterDifference.forEach((key, value) -> {
      double totalCost;
      if (key.getType().equals(MeterType.WASSERWARM)) {
        totalCost = key.getBaseCost() + (value * GASFACTORWARMWATER * key.getCostPerUnit());
      } else {
        totalCost = key.getBaseCost() + (value * key.getCostPerUnit());
      }
      meterTotalCostMap.put(key, totalCost);
    });
    return meterTotalCostMap;
  }

  /**
   * Create invoices for all tenants
   *
   * @param bearerToken Token of the Landlord
   * @return ResponseEntity with the status of the invoices creation
   * @author Cedric Stumpf
   */
  @Transactional
  public ResponseEntity<?> createInvoice(String bearerToken, int flatId, double totalRentPaid) {
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
    var flat = flatRepository.findById(flatId);
    if (flat.isEmpty()) {
      log.warn("No flat found for id '{}'", flatId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wohnung nicht gefunden");
    }
    if (flat.get().getTenant() == null) {
      log.warn("No tenant found for flat '{}'....Skipping", flatId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Keinen Mieter gefunden");
    }
    var building = buildingRepository.findByFlatListContaining(flat.get());
    if (building.isEmpty()) {
      log.warn("No building found for flat '{}'", flatId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }

    createInvoiceForFlat(building.get(), flat.get(), totalRentPaid);

    return ResponseEntity.status(HttpStatus.ACCEPTED).body("Rechnung erstellt");
  }

  /**
   * Get all invoices per building
   *
   * @param bearerToken Token of the Landlord
   * @return ResponseEntity with the invoices for the buildings of the landlord
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getAllInvoicesForAllBuildings(String bearerToken) {
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
    var buildingList = buildingRepository.findByLandlord(user.get());
    if (buildingList.isEmpty()) {
      log.warn("No buildings found for landlord '{}'", user.get().getEmail());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }
    List<Invoice> invoiceList = new ArrayList<>();
    for (Building building : buildingList) {
      log.info("Getting invoices for building: '{}'", building.getId());
      for (Flat flat : building.getFlatList()) {
        if (flat.getInvoiceList().isEmpty()) {
          continue;
        }
        invoiceRepository.findById(flat.getInvoiceList().getLast().getId())
            .ifPresent(invoiceList::add);
      }
    }
    if (invoiceList.isEmpty()) {
      log.warn("No invoices found for landlord '{}'", user.get().getEmail());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Keine Rechnungen gefunden");
    }
    return ResponseEntity.status(HttpStatus.OK).body(getGetInvoiceDtos(invoiceList));
  }

  /**
   * Update the paid status of an invoice
   *
   * @param invoiceId   Id of the invoice
   * @param buildingId  Id of the building
   * @param bearerToken Token of the Landlord
   * @return ResponseEntity with the status of the invoice
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> updateInvoicePaidStatus(int invoiceId, int buildingId,
      String bearerToken) {
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
    var building = buildingRepository.findById(buildingId);
    if (building.isEmpty()) {
      log.warn("Building not found for landlord '{}'", user.get().getEmail());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Gebäude nicht gefunden");
    }

    var invoice = invoiceRepository.findById(invoiceId);
    if (invoice.isEmpty()) {
      log.warn("Invoice not found");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Rechnung nicht gefunden");
    }

    var flat = building.get().getFlatList().stream()
        .filter(f -> f.getInvoiceList().contains(invoice.get())).findFirst();
    if (flat.isEmpty()) {
      log.warn("Invoice not found in buildings flat list");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Angegebene Rechnung nicht gefunden");
    }

    invoice.get().setPaid(!invoice.get().isPaid());
    invoiceRepository.saveAndFlush(invoice.get());

    return ResponseEntity.status(HttpStatus.OK)
        .body("Zahlungstatus geändert, und ist nun: " + invoice.get().isPaid());
  }

  /**
   * Get all invoices for a flat
   *
   * @param bearerToken Token of the Tenant
   * @return ResponseEntity with the invoices for the flat of the tenant
   * @author Cedric Stumpf
   */
  public ResponseEntity<?> getAllInvoicesForFlat(String bearerToken) {
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
    var flat = flatRepository.findByTenant(user.get());
    if (flat.isEmpty()) {
      log.warn("No flat found for tenant '{}'", user.get().getEmail());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wohnung nicht gefunden");
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(getGetInvoiceDtos(flat.stream().flatMap(f -> f.getInvoiceList().stream()).toList()));
  }
}