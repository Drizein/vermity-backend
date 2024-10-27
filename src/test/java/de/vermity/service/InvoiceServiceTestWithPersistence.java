package de.vermity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.vermity.model.dto.AddressDto;
import de.vermity.model.dto.BuildingDto;
import de.vermity.model.dto.FlatDto;
import de.vermity.model.dto.GetInvoiceDto;
import de.vermity.model.dto.LoginDto;
import de.vermity.model.dto.MeterDto;
import de.vermity.model.dto.ModifyFlatDto;
import de.vermity.model.dto.PersonDto;
import de.vermity.model.dto.RegisterDto;
import de.vermity.model.dto.UpdateMeterReadingDto;
import de.vermity.model.entity.AdditionalCost;
import de.vermity.util.enums.Distribution;
import de.vermity.util.enums.Frequency;
import de.vermity.util.enums.Gender;
import de.vermity.util.enums.MeterType;
import de.vermity.util.enums.Role;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for {@link InvoiceService}.
 *
 * @author Cedric Stumpf
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class InvoiceServiceTestWithPersistence {

  @InjectMocks
  @Autowired
  private InvoiceService invoiceService;
  @Autowired
  private PersonService personService;
  @Autowired
  private BuildingService buildingService;
  @Autowired
  private FlatService flatService;
  private PersonDto landlordLogin;
  private PersonDto tenantLogin;
  private List<BuildingDto> building;

  @BeforeEach
  void setUp() {

    RegisterDto landlordRegisterDto = new RegisterDto("Landlord", "Land", Gender.DIVERSE,
        "+49648156456", "land@lord.de", LocalDate.of(1990, 1, 1), "Password123!",
        List.of(Role.LANDLORD));
    RegisterDto tenantRegisterDto = new RegisterDto("Tenant", "Living", Gender.FEMALE,
        "+97636247", "Tenant@lord.de", LocalDate.of(1988, 1, 1), "Password123!",
        List.of(Role.TENANT));

    personService.createUser(landlordRegisterDto);
    personService.createUser(tenantRegisterDto);

    LoginDto landlordLoginDto = new LoginDto("land@lord.de", "Password123!");
    LoginDto tenantLoginDto = new LoginDto("Tenant@lord.de", "Password123!");

    landlordLogin = (PersonDto) personService.loginUser(landlordLoginDto).getBody();
    tenantLogin = (PersonDto) personService.loginUser(tenantLoginDto).getBody();

    buildingService.createBuilding(createTestBuildingDto(), landlordLogin.token());
    building = (List<BuildingDto>) buildingService.getAllBuildings(landlordLogin.token())
        .getBody();

    ModifyFlatDto modifyFlatDto = new ModifyFlatDto(building.getFirst().buildingId(),
        building.getFirst().flatList().getFirst()
            .flatId(), tenantRegisterDto.email(), "", "", 1);
    flatService.updateTenant(modifyFlatDto, landlordLogin.token());

    flatService.updateMeterReading(
        new UpdateMeterReadingDto(building.getFirst().flatList().getFirst().flatId(),
            building.getFirst().flatList().getFirst().meterList().getFirst()
                .meterId(), 2000), tenantLogin.token());

    flatService.updateMeterReading(
        new UpdateMeterReadingDto(building.getFirst().flatList().getFirst().flatId(),
            building.getFirst().flatList().getFirst().meterList().get(1)
                .meterId(), 6500),
        tenantLogin.token());

    flatService.updateMeterReading(
        new UpdateMeterReadingDto(building.getFirst().flatList().getFirst().flatId(),
            building.getFirst().flatList().getFirst().meterList().get(2)
                .meterId(), 15000),
        tenantLogin.token());

  }

  private BuildingDto createTestBuildingDto() {
    // Create and return a real BuildingDto object with the necessary test data
    AddressDto addressDto = new AddressDto("Street", 12345, "City", "Country", "State");

    List<MeterDto> meterDtoList = List.of(
        new MeterDto(1, 1000, "12345", MeterType.GAS, 0.48, 12.50),
        new MeterDto(2, 5000, "455735", MeterType.WASSERWARM, 1.48, 1.50),
        new MeterDto(3, 12000, "99272", MeterType.STROM, 0.28, 22.50)
    );

    List<AdditionalCost> additionList = List.of(
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.MONTHLY),
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.YEARLY),
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.QUARTERLY)
    );

    FlatDto flatDto = new FlatDto(0, meterDtoList, "1.OG", 3, 100, 2, additionList, 300, 500,
        List.of());
    FlatDto flatDto2 = new FlatDto(0, List.of(), "1.OG rechts", 4, 400, 6, additionList, 1300, 1500,
        List.of());
    List<FlatDto> flatList = List.of(flatDto, flatDto2);

    List<AdditionalCost> operatingCosts = List.of(
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.FLAT, Frequency.MONTHLY),
        new AdditionalCost(0, "What I know", "123", 4, Distribution.PERSON, Frequency.QUARTERLY),
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.SQUARE_METERS,
            Frequency.YEARLY),
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.NONE, Frequency.MONTHLY)
    );

    return new BuildingDto(0, flatList, operatingCosts, addressDto);
  }

  @ParameterizedTest
  @CsvSource({
      "ACCEPTED, 'Rechnung erstellt', 500",
  })
  void testCreateInvoiceValidData(
      HttpStatus expectedStatus,
      String expectedMessage,
      double totalRentPaid
  ) {
    var response = invoiceService.createInvoice(landlordLogin.token(),
        building.getFirst().flatList().getFirst().flatId(), totalRentPaid);

    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());

  }

  @ParameterizedTest
  @CsvSource({
      "OK, 500",
  })
  void testGetAllInvoicesForAllBuildings(
      HttpStatus expectedStatus,
      double totalRentPaid
  ) {
    invoiceService.createInvoice(landlordLogin.token(),
        building.getFirst().flatList().getFirst().flatId(), totalRentPaid);

    var response = invoiceService.getAllInvoicesForAllBuildings(landlordLogin.token());

    assertEquals(expectedStatus, response.getStatusCode());
  }

  @ParameterizedTest
  @CsvSource({
      "OK, 'Zahlungstatus geändert, und ist nun: true', 500",
  })
  void testUpdateInvoicePaidStatus(
      HttpStatus expectedStatus,
      String expectedMessage,
      double totalRentPaid
  ) {
    invoiceService.createInvoice(landlordLogin.token(),
        building.getFirst().flatList().getFirst().flatId(), totalRentPaid);
    var invoices = (List<GetInvoiceDto>) invoiceService.getAllInvoicesForAllBuildings(
        landlordLogin.token()).getBody();

    assertNotNull(invoices);
    var response = invoiceService.updateInvoicePaidStatus(invoices.getFirst().invoiceId(),
        invoices.getFirst().buildingId(), landlordLogin.token());

    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

  @ParameterizedTest
  @CsvSource({
      "OK, 500",
  })
  void testGetAllInvoicesForFlat(
      HttpStatus expectedStatus,
      double totalRentPaid
  ) {
    invoiceService.createInvoice(landlordLogin.token(), building.getFirst().flatList().getFirst()
        .flatId(), totalRentPaid);

    var response = invoiceService.getAllInvoicesForFlat(tenantLogin.token());

    assertEquals(expectedStatus, response.getStatusCode());
  }

}