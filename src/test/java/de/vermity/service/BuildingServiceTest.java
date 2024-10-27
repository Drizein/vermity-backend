package de.vermity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.vermity.model.dto.AddressDto;
import de.vermity.model.dto.BuildingDto;
import de.vermity.model.dto.FlatDto;
import de.vermity.model.dto.MeterDto;
import de.vermity.model.dto.ModifyBuildingDto;
import de.vermity.model.entity.AdditionalCost;
import de.vermity.model.entity.Address;
import de.vermity.model.entity.Building;
import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Meter;
import de.vermity.model.entity.Person;
import de.vermity.persistence.AdditionalCostRepository;
import de.vermity.persistence.AddressRepository;
import de.vermity.persistence.BuildingRepository;
import de.vermity.persistence.FlatRepository;
import de.vermity.persistence.MeterRepository;
import de.vermity.persistence.PersonRepository;
import de.vermity.persistence.UpdateRepository;
import de.vermity.security.JWTGenerator;
import de.vermity.util.UserUtil;
import de.vermity.util.enums.Distribution;
import de.vermity.util.enums.Frequency;
import de.vermity.util.enums.Gender;
import de.vermity.util.enums.MeterType;
import de.vermity.util.enums.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for the BuildingService class.
 *
 * @author Cedric Stumpf
 */
class BuildingServiceTest {

  @Mock
  private UserUtil userUtil;

  @Mock
  private BuildingRepository buildingRepository;

  @Mock
  private MeterRepository meterRepository;

  @Mock
  private AddressRepository addressRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private JWTGenerator jwtGenerator;

  @Mock
  private AdditionalCostRepository additionalCostRepository;

  @Mock
  private FlatRepository flatRepository;

  @Mock
  private UpdateRepository updateRepository;

  @InjectMocks
  private BuildingService buildingService;

  private String bearerToken;
  private Person user;
  private BuildingDto buildingDto;
  private BuildingDto buildingDtoMoreFlats;
  private ModifyBuildingDto modifyBuildingDto;
  private Building building;
  private Building buildingMoreFlats;
  private ModifyBuildingDto modifyBuildingDtoWithMoreFlats;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    bearerToken = "validToken";
    user = Person.builder().firstName("Person").lastName("Tester").email("tester@email.com")
        .gender(Gender.MALE).roleList(List.of(Role.LANDLORD)).build();
    buildingDto = createTestBuildingDto();
    buildingDtoMoreFlats = createTestBuildingDtoMoreFlats();
    modifyBuildingDto = createTestModifyBuildingDto();
    modifyBuildingDtoWithMoreFlats = createTestModifyBuildingDtoWithMoreFlats();
    building = createTestBuilding();
    buildingMoreFlats = createTestBuildingMoreFlats();
    when(userUtil.verifyUser(bearerToken)).thenReturn(Optional.of(user));
  }

  private BuildingDto createTestBuildingDto() {
    // Create and return a real BuildingDto object with the necessary test data
    AddressDto addressDto = new AddressDto("Street", 12345, "City", "Country", "State");
    List<MeterDto> meterDtoList = List.of(
        new MeterDto(1, 1000, "12345", MeterType.GAS, 0.48, 12.50));
    List<AdditionalCost> additionList = List.of(
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.MONTHLY));
    FlatDto flatDto = new FlatDto(0, meterDtoList, "1.OG", 3, 100, 2, additionList, 300, 500,
        List.of());
    List<FlatDto> flatList = List.of(flatDto);

    List<AdditionalCost> operatingCosts = List.of(
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.FLAT, Frequency.MONTHLY));

    return new BuildingDto(0, flatList, operatingCosts, addressDto);
  }

  private BuildingDto createTestBuildingDtoMoreFlats() {
    // Create and return a real BuildingDto object with the necessary test data
    AddressDto addressDto = new AddressDto("Street", 12345, "City", "Country", "State");
    List<MeterDto> meterDtoList = List.of(
        new MeterDto(1, 1000, "12345", MeterType.GAS, 0.48, 12.50));
    List<AdditionalCost> additionList = List.of(
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.MONTHLY));
    FlatDto flatDto1 = new FlatDto(0, meterDtoList, "1.OG", 1, 100, 2, additionList, 300, 500,
        List.of());
    FlatDto flatDto2 = new FlatDto(1, meterDtoList, "2.OG", 2, 200, 4, additionList, 300, 500,
        List.of());
    FlatDto flatDto3 = new FlatDto(2, meterDtoList, "3.OG", 3, 300, 5, additionList, 300, 500,
        List.of());
    FlatDto flatDto4 = new FlatDto(3, meterDtoList, "4.OG", 4, 400, 6, additionList, 300, 500,
        List.of());
    List<FlatDto> flatList = List.of(flatDto1, flatDto2, flatDto3, flatDto4);

    List<AdditionalCost> operatingCosts = List.of(
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.FLAT, Frequency.MONTHLY));

    return new BuildingDto(0, flatList, operatingCosts, addressDto);
  }

  private ModifyBuildingDto createTestModifyBuildingDto() {
    // Create and return a real BuildingDto object with the necessary test data
    AddressDto addressDto = new AddressDto("Street", 12345, "City", "Country", "State");
    List<MeterDto> meterDtoList = List.of(
        new MeterDto(1, 1000, "12345", MeterType.GAS, 0.48, 12.50));
    List<AdditionalCost> additionList = List.of(
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.MONTHLY));
    FlatDto flatDto = new FlatDto(0, meterDtoList, "1.OG", 3, 100, 2, additionList, 300, 500,
        List.of());
    List<FlatDto> flatList = List.of(flatDto);

    List<AdditionalCost> operatingCosts = List.of(
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.FLAT, Frequency.MONTHLY));

    return new ModifyBuildingDto(0, flatList, operatingCosts, addressDto);
  }

  private ModifyBuildingDto createTestModifyBuildingDtoWithMoreFlats() {
    // Create and return a real BuildingDto object with the necessary test data
    AddressDto addressDto = new AddressDto("Street", 12345, "City", "Country", "State");
    List<MeterDto> meterDtoList = List.of(
        new MeterDto(1, 1000, "12345", MeterType.GAS, 0.48, 12.50));
    List<AdditionalCost> additionList = List.of(
        new AdditionalCost(0, "Garage", "Rent of Garage", 25, Distribution.NONE,
            Frequency.MONTHLY));
    FlatDto flatDto1 = new FlatDto(0, meterDtoList, "1.OG", 1, 100, 2, additionList, 300, 500,
        List.of());
    FlatDto flatDto2 = new FlatDto(1, meterDtoList, "2.OG", 2, 200, 4, additionList, 300, 500,
        List.of());
    FlatDto flatDto3 = new FlatDto(2, meterDtoList, "3.OG", 3, 300, 5, additionList, 300, 500,
        List.of());
    FlatDto flatDto4 = new FlatDto(3, meterDtoList, "4.OG", 4, 400, 6, additionList, 300, 500,
        List.of());
    List<FlatDto> flatList = List.of(flatDto1, flatDto2, flatDto3, flatDto4);

    List<AdditionalCost> operatingCosts = List.of(
        new AdditionalCost(0, "Garden", "Gardening", 25, Distribution.FLAT, Frequency.MONTHLY));

    return new ModifyBuildingDto(0, flatList, operatingCosts, addressDto);
  }

  private Building createTestBuilding() {
    Person tenant = Person.builder().firstName("Ten").lastName("Ant")
        .address(Address.builder().street("Street").zip(12345).city("City")
            .country("Country").state("State").build()).gender(Gender.FEMALE)
        .phoneNumber("+149667961").email("email@tester.com").build();
    Person landlord = Person.builder().firstName("Person").lastName("Tester")
        .email("tester@email.com").gender(Gender.MALE).roleList(List.of(Role.LANDLORD)).build();
    List<Meter> meterlist = List.of(
        Meter.builder().id(1).meterNr("12345").reading(1000).costPerUnit(0.48).type(MeterType.GAS)
            .baseCost(12.50)
            .build());
    List<AdditionalCost> additionList = List.of(
        AdditionalCost.builder().id(0).name("Garage").description("Rent of Garage").amount(25)
            .distribution(Distribution.NONE).frequency(Frequency.MONTHLY).build());
    List<AdditionalCost> operatingCost = List.of(
        AdditionalCost.builder().id(0).name("Garden").description("Gardening").amount(25)
            .distribution(Distribution.FLAT).frequency(Frequency.MONTHLY).build());
    List<Flat> flatList = List.of(
        Flat.builder().id(0).tenant(tenant).meterList(meterlist).location("1.OG").squareMeter(100)
            .rooms(3).residents(2).additionList(additionList).coldRent(300).warmRent(500).build());
    Address address = Address.builder().street("Street").zip(12345).city("City")
        .country("Country").state("State").build();

    return Building.builder().id(0).flatList(flatList).landlord(landlord)
        .operatingCosts(operatingCost).address(address).build();
  }

  private Building createTestBuildingMoreFlats() {
    Person tenant = Person.builder().firstName("Ten").lastName("Ant")
        .address(Address.builder().street("Street").zip(12345).city("City")
            .country("Country").state("State").build()).gender(Gender.FEMALE)
        .phoneNumber("+149667961").email("email@tester.com").build();
    Person landlord = Person.builder().firstName("Person").lastName("Tester")
        .email("tester@email.com").gender(Gender.MALE).roleList(List.of(Role.LANDLORD)).build();
    List<Meter> meterlist = List.of(
        Meter.builder().id(1).meterNr("12345").reading(1000).costPerUnit(0.48).type(MeterType.GAS)
            .baseCost(12.50)
            .build());
    List<AdditionalCost> additionList = List.of(
        AdditionalCost.builder().id(0).name("Garage").description("Rent of Garage").amount(25)
            .distribution(Distribution.NONE).frequency(Frequency.MONTHLY).build());
    List<AdditionalCost> operatingCost = List.of(
        AdditionalCost.builder().id(0).name("Garden").description("Gardening").amount(25)
            .distribution(Distribution.FLAT).frequency(Frequency.MONTHLY).build());
    List<Flat> flatList = List.of(
        Flat.builder().id(0).tenant(tenant).meterList(meterlist).location("1.OG").squareMeter(100)
            .rooms(1).residents(2).additionList(additionList).coldRent(300).warmRent(500).build(),
        Flat.builder().id(1).meterList(meterlist).location("2.OG").rooms(2).squareMeter(200)
            .residents(4).additionList(additionList).coldRent(300).warmRent(500).build(),
        Flat.builder().id(2).meterList(meterlist).location("3.OG").rooms(3).squareMeter(300)
            .residents(5).additionList(additionList).coldRent(300).warmRent(500).build(),
        Flat.builder().id(3).meterList(meterlist).location("4.OG").rooms(4).squareMeter(400)
            .residents(6).additionList(additionList).coldRent(300).warmRent(500).build()
    );
    Address address = Address.builder().street("Street").zip(12345).city("City")
        .country("Country").state("State").build();

    return Building.builder().id(0).flatList(flatList).landlord(landlord)
        .operatingCosts(operatingCost).address(address).build();
  }

  @ParameterizedTest
  @CsvSource({
      "invalidToken, , , UNAUTHORIZED, Bitte melde dich zuerst an!",
      "validToken, true, , BAD_REQUEST, Gebäude an dieser Adresse existiert bereits.",
      "validToken, false, true, BAD_REQUEST, Diese Zählernummer existiert bereits.",
      "throwException, , , UNAUTHORIZED, Bitte melde dich zuerst an!",
      "validToken, false, false, OK, Gebäude erstellt."})
  void createBuilding_VariousScenarios(String token, Boolean duplicateBuilding,
      Boolean duplicateMeter, HttpStatus expectedStatus, String expectedMessage) {
    // Arrange
    if ("invalidToken".equals(token)) {
      when(userUtil.verifyUser(token)).thenReturn(Optional.empty());
    } else if ("throwException".equals(token)) {
      when(userUtil.verifyUser(token)).thenThrow(IllegalArgumentException.class);
    } else {
      when(userUtil.verifyUser(token)).thenReturn(Optional.of(user));
      when(
          buildingRepository.findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
              anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(
          duplicateBuilding != null && duplicateBuilding ? Optional.of(new Building())
              : Optional.empty());

      if (duplicateBuilding != null && !duplicateBuilding && duplicateMeter != null
          && duplicateMeter) {
        when(meterRepository.findByMeterNrIn(anyList())).thenReturn(List.of(new Meter()));
      }
    }

    // Act
    ResponseEntity<String> response = buildingService.createBuilding(buildingDto, token);

    // Assert
    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {"LANDLORD"}, mode = EnumSource.Mode.EXCLUDE)
  void createBuilding_AssignLandlordRole(Role existingRole) {
    // Arrange
    mockBuildingAndMeter(false, false);
    user.setRoleList(List.of(existingRole));

    // Act
    ResponseEntity<String> response = buildingService.createBuilding(buildingDto, bearerToken);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Gebäude erstellt und dir die Rolle 'Vermieter zugewiesen.", response.getBody());
    verify(personRepository).saveAndFlush(user);
  }

  @ParameterizedTest
  @CsvSource({
      "invalidToken, UNAUTHORIZED, Bitte melde dich zuerst an!",
      "throwException, UNAUTHORIZED, Bitte melde dich zuerst an!",
      "validToken, OK,"})
  void getAllBuildings_VariousScenarios(String token, HttpStatus expectedStatus,
      String expectedMessage) {
    // Arrange
    if ("invalidToken".equals(token)) {
      when(userUtil.verifyUser(token)).thenReturn(Optional.empty());
    } else if ("throwException".equals(token)) {
      when(userUtil.verifyUser(token)).thenThrow(IllegalArgumentException.class);
    } else {
      when(userUtil.verifyUser(token)).thenReturn(Optional.of(user));
      when(buildingRepository.findAll()).thenReturn(List.of(building));
    }

    // Act
    ResponseEntity<?> response = buildingService.getAllBuildings(token);

    // Assert
    assertEquals(expectedStatus, response.getStatusCode());
    if (expectedStatus == HttpStatus.OK) {
      assertEquals(List.of(buildingDto), response.getBody());
    } else {
      assertEquals(expectedMessage, response.getBody());
    }
  }

  @ParameterizedTest
  @CsvSource({
      "invalidToken, UNAUTHORIZED, Bitte melde dich zuerst an!, false, false",
      "throwException, UNAUTHORIZED, Bitte melde dich zuerst an!, false, false",
      "validToken, OK, Gebäude gelöscht., true, true",
      "validToken, BAD_REQUEST, Gebäude nicht gefunden, false, false",
      "validToken, UNAUTHORIZED, Dies ist nicht dein Gebäude!, true, false",})
  void deleteBuilding_VariousScenarios(String token, HttpStatus expectedStatus,
      String expectedMessage, Boolean buildingExists, Boolean isOwner) {
    // Arrange
    if ("invalidToken".equals(token)) {
      when(userUtil.verifyUser(token)).thenReturn(Optional.empty());
    } else if ("throwException".equals(token)) {
      when(userUtil.verifyUser(token)).thenThrow(IllegalArgumentException.class);
    } else {
      when(userUtil.verifyUser(token)).thenReturn(Optional.of(user));
    }
    if (buildingExists) {
      when(
          buildingRepository.findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
              anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(
          Optional.of(building));
    } else {
      when(
          buildingRepository.findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
              anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(
          Optional.empty());
    }
    if (isOwner) {
      building.setLandlord(user);
    } else {
      building.setLandlord(Person.builder().firstName("Other").lastName("Person").build());
    }

    // Act
    ResponseEntity<?> response = buildingService.deleteBuilding(buildingDto, token);

    // Assert
    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

  @ParameterizedTest
  @CsvSource({
      "invalidToken, UNAUTHORIZED, Bitte melde dich zuerst an!, false, false, false, false",
      "throwException, UNAUTHORIZED, Bitte melde dich zuerst an!, false, false, false, false",
      "validToken, OK, , true, true, false, false",
      "validToken, OK, , true, true, false, false",
      "validToken, BAD_REQUEST, Gebäude nicht gefunden, false, false, false, false",
      "validToken, UNAUTHORIZED, Dies ist nicht dein Gebäude!, true, false, false, false",
      "validToken, UNAUTHORIZED, An dieser Adresse existiert bereits ein Gebäude!, true, true, true, true",
      "validToken, UNAUTHORIZED, An dieser Adresse existiert bereits ein Gebäude!, true, true, true, false",
  })
  void modifyBuilding_VariousScenarios(String token, HttpStatus expectedStatus,
      String expectedMessage, Boolean buildingExists, Boolean isOwner, Boolean newAddressExists,
      Boolean addressMatchesBuilding) {
    // Arrange
    if ("invalidToken".equals(token)) {
      when(userUtil.verifyUser(token)).thenReturn(Optional.empty());
    } else if ("throwException".equals(token)) {
      when(userUtil.verifyUser(token)).thenThrow(IllegalArgumentException.class);
    } else {
      when(userUtil.verifyUser(token)).thenReturn(Optional.of(user));
    }

    if (buildingExists) {
      when(buildingRepository.findById(anyInt())).thenReturn(Optional.of(building));
    } else {
      when(buildingRepository.findById(anyInt())).thenReturn(Optional.empty());
    }
    if (isOwner) {
      building.setLandlord(user);
    } else {
      building.setLandlord(Person.builder().firstName("Other").lastName("Person").build());
    }

    if (newAddressExists && !addressMatchesBuilding) {
      var address = Address.builder().build();
      address.setCity("New City");
      address.setId(2);
      when(addressRepository.findByStateAndCityAndStreetAndZipAndCountry(anyString(), anyString(),
          anyString(), anyInt(), anyString())).thenReturn(Optional.of(address));

    } else if (newAddressExists) {
      when(addressRepository.findByStateAndCityAndStreetAndZipAndCountry(anyString(), anyString(),
          anyString(), anyInt(), anyString())).thenReturn(Optional.of(new Address()));
    } else {
      when(addressRepository.findByStateAndCityAndStreetAndZipAndCountry(anyString(), anyString(),
          anyString(), anyInt(), anyString())).thenReturn(Optional.empty());
      when(addressRepository.saveAndFlush(any())).thenReturn(
          Address.builder().street("Street").zip(12345).city("City")
              .country("Country").state("State").build());
      when(buildingRepository.saveAndFlush(any())).thenReturn(building);
      when(additionalCostRepository.saveAllAndFlush(any())).thenReturn(building.getFlatList().getFirst().getAdditionList());
    }

    // Act
    var response = buildingService.modifyBuilding(modifyBuildingDto, token);

    // Assert
    assertEquals(expectedStatus, response.getStatusCode());
    if (expectedStatus == HttpStatus.OK) {
      assertEquals(buildingDto, response.getBody());
    } else {
      assertEquals(expectedMessage, response.getBody());
    }
  }

  @ParameterizedTest
  @CsvSource({
      "invalidToken, UNAUTHORIZED, Bitte melde dich zuerst an!, false, false",
      "throwException, UNAUTHORIZED, Bitte melde dich zuerst an!, false, false",
      "validToken, OK, , true, true",
      "validToken, OK, , true, false",
  })
  void getAllBuildingsLandlordView_VariousScenarios(String token, HttpStatus expectedStatus,
      String expectedMessage, Boolean buildingExists, Boolean tenantExists) {
    // Arrange
    if ("invalidToken".equals(token)) {
      when(userUtil.verifyUser(token)).thenReturn(Optional.empty());
    } else if ("throwException".equals(token)) {
      when(userUtil.verifyUser(token)).thenThrow(IllegalArgumentException.class);
    } else {
      when(userUtil.verifyUser(token)).thenReturn(Optional.of(user));
    }
    if (buildingExists) {
      when(buildingRepository.findByLandlord(user)).thenReturn(List.of(building));
    }
    if (!tenantExists) {
      building.getFlatList().getFirst().setTenant(null);
    }

    // Act
    ResponseEntity<?> response = buildingService.getAllBuildingsLandlordView(token);

    // Assert
    assertEquals(expectedStatus, response.getStatusCode());
    if (expectedStatus == HttpStatus.OK) {
      assertNotNull(response.getBody());
    } else {
      assertEquals(expectedMessage, response.getBody());
    }
  }


  private void mockBuildingAndMeter(boolean buildingExists, boolean meterExists) {
    when(
        buildingRepository.findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
            anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(
        buildingExists ? Optional.of(Building.builder().id(1).flatList(List.of(Flat.builder().id(1)
                .tenant(Person.builder().firstName("Ten").lastName("Ant").gender(Gender.MALE)
                    .email("+491234566").birthDate(LocalDate.of(1988, 12, 12))
                    .roleList(List.of(Role.TENANT)).build()).meterList(List.of(
                    Meter.builder().meterNr("12345").costPerUnit(0.48).type(MeterType.GAS)
                        .reading(1111).build())).location("1.OG").squareMeter(100).rooms(4)
                .residents(3).additionList(List.of(
                    AdditionalCost.builder().name("Garage").description("Rent of Garage")
                        .distribution(Distribution.NONE).frequency(Frequency.MONTHLY).build()))
                .coldRent(300).warmRent(500).build()))
            .landlord(Person.builder().firstName("Land").lastName("Lord").build())
            .operatingCosts(List.of(AdditionalCost.builder().build())).address(
                Address.builder().city("city").country("country").zip(12345).state("state")
                    .street("street").build()).build()) : Optional.empty());
    when(meterRepository.findByMeterNrIn(anyList())).thenReturn(meterExists ? List.of(
        Meter.builder().meterNr("12345").costPerUnit(0.48).type(MeterType.GAS).reading(1111)
            .build()) : List.of());
  }
}
