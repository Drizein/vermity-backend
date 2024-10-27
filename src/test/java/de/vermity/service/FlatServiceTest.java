package de.vermity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import de.vermity.model.dto.AddressDto;
import de.vermity.model.dto.FlatDto;
import de.vermity.model.dto.InvoiceDto;
import de.vermity.model.dto.LandlordViewPersonDto;
import de.vermity.model.dto.MeterDto;
import de.vermity.model.dto.ModifyFlatDto;
import de.vermity.model.dto.UpdateMeterReadingDto;
import de.vermity.model.entity.AdditionalCost;
import de.vermity.model.entity.Address;
import de.vermity.model.entity.Building;
import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Invoice;
import de.vermity.model.entity.Meter;
import de.vermity.model.entity.Person;
import de.vermity.model.entity.Update;
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
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Test class for the FlatService class.
 *
 * @author Cedric Stumpf
 */
@ExtendWith(MockitoExtension.class)
class FlatServiceTest {

  @Mock
  private PersonRepository personRepository;

  @Mock
  private BuildingRepository buildingRepository;

  @Mock
  private FlatRepository flatRepository;

  @Mock
  private MeterRepository meterRepository;

  @Mock
  private UpdateRepository updateRepository;

  @Mock
  private UserUtil userUtil;

  @Mock
  private JWTGenerator jwtGenerator;

  @InjectMocks
  private FlatService flatService;


  private static Stream<Arguments> provideUpdateTenant() {
    return Stream.of(
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "newTenantFirstName", "newTenantLastName", 1),
            "token",
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(Building.builder().id(2).landlord(
                    Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build())
                .flatList(List.of(Flat.builder().id(1).build())).build()),
            HttpStatus.OK,
            "Mieter aktualisiert",
            null,
            Optional.of(Person.builder().email("newTenantEmail").firstName("newTenantFirstName")
                .lastName("newTenantLastName").build()),
            null
        ),
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "newTenantFirstName", "newTenantLastName", 0),
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Landlord@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!",
            new JwtException("Can't verify user"),
            null,
            null
        ),
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "newTenantFirstName", "newTenantLastName", 0),
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Landlord@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!",
            new IllegalArgumentException("Can't verify user"),
            null,
            null
        ),
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "newTenantFirstName", "newTenantLastName", 0),
            "token",
            Optional.empty(),
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!",
            null,
            null,
            null
        ),
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "newTenantFirstName", "newTenantLastName", 0),
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Landlord@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.empty(),
            HttpStatus.BAD_REQUEST,
            "Gebäude nicht gefunden",
            null,
            null,
            null
        ),
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "", "", 0),
            "token",
            Optional.of(
                Person.builder().id(2).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(Building.builder().id(1).landlord(Person.builder().id(1).build())
                .flatList(List.of(Flat.builder().id(1).build())).build()),
            HttpStatus.UNAUTHORIZED,
            "Du bist nicht der Vermieter dieses Gebäudes",
            null,
            null,
            null
        ),
        Arguments.of(
            new ModifyFlatDto(1, 1, "newTenantEmail", "", "", 0),
            "token",
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(Building.builder().id(2).landlord(
                    Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build())
                .flatList(List.of(Flat.builder().id(1).build())).build()),
            HttpStatus.BAD_REQUEST,
            "Neuen Mieter nicht gefunden. Bitte gib eine Vor- und Nachnamen an.",
            null,
            Optional.empty(),
            null
        ),
        Arguments.of(
            new ModifyFlatDto(
                1, 1, "", "new Name", "Tenant", 1),
            "token",
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(Building.builder().id(2).landlord(
                    Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build())
                .flatList(List.of(Flat.builder().id(1).build())).build()),
            HttpStatus.OK,
            "Mieter aktualisiert",
            null,
            null,
            Person.builder().firstName("new Name").lastName("Tenant").build()
        ),
        Arguments.of(new ModifyFlatDto(1, 1, "", "NotWorking", "", 1), "token",
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(
                Building.builder().id(2).landlord(
                        Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                            .gender(Gender.MALE)
                            .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord")
                            .build())
                    .flatList(List.of(Flat.builder().id(1).build())).build()),
            HttpStatus.BAD_REQUEST,
            "Keinen neuen Mieter angegeben",
            null,
            null,
            null
        ),
        Arguments.of(new ModifyFlatDto(1, 1, "", "", "", 0), "token",
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(
                Building.builder().id(2).landlord(
                        Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                            .gender(Gender.MALE)
                            .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord")
                            .build())
                    .flatList(List.of(Flat.builder().id(1).build())).build()),
            HttpStatus.BAD_REQUEST,
            "Keinen neuen Mieter angegeben",
            null,
            null,
            null
        ),
        Arguments.of(new ModifyFlatDto(1, 1, "newTenant", "", "", 0), "token",
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build()),
            Optional.of(Building.builder().id(2).landlord(
                    Person.builder().id(1).phoneNumber("+49000000").email("Landlord@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").build())
                .flatList(List.of()).build()),
            HttpStatus.BAD_REQUEST,
            "Wohnung nicht gefunden",
            null,
            Optional.of(Person.builder().email("newTenant").build()),
            null
        )
    );
  }

  private static Stream<Arguments> provideGetFlat() {
    return Stream.of(
        Arguments.of(
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            List.of(
                Flat.builder().id(1).tenant(
                        Person.builder().phoneNumber("+49000000").email("Tenant@t.de")
                            .gender(Gender.MALE)
                            .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                    .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                        MeterType.GAS).reading(98765).costPerUnit(0.48).baseCost(12.50).build()))
                    .location("1.OG")
                    .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                    .additionList(List.of(
                        AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                            .distribution(
                                Distribution.NONE).description("Garage").id(1).build())).build()),
            HttpStatus.OK,
            List.of(new FlatDto(1, List.of(new MeterDto(1, 98765, "12345", MeterType.GAS, 0.48, 12.50)),
                "1.OG",
                3, 100, 3,
                List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY).distribution(
                        Distribution.NONE).description("Garage").id(1).build()), 300, 500,
                List.of()))
        ),
        Arguments.of(
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            List.of(
                Flat.builder().id(1).tenant(
                        Person.builder().phoneNumber("+49000000").email("Tenant@t.de")
                            .gender(Gender.MALE)
                            .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                    .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                        MeterType.GAS).reading(98765).costPerUnit(0.48).baseCost(12.50).build()))
                    .location("1.OG")
                    .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                    .additionList(List.of(
                        AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                            .distribution(
                                Distribution.NONE).description("Garage").id(1).build()))
                    .invoiceList(List.of(
                        Invoice.builder().id(1).paid(false).pdf("pdf").build(),
                        Invoice.builder().id(2).paid(true).pdf("abc").build()))
                    .build()),
            HttpStatus.OK,
            List.of(new FlatDto(1, List.of(new MeterDto(1, 98765, "12345", MeterType.GAS, 0.48, 12.50)),
                "1.OG",
                3, 100, 3,
                List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY).distribution(
                        Distribution.NONE).description("Garage").id(1).build()), 300, 500,
                List.of(
                    new InvoiceDto(1, false, "pdf"),
                    new InvoiceDto(2, true, "abc")
                )))
        ),
        Arguments.of(
            "token",
            Optional.empty(),
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            new JwtException("Can't verify user"),
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            new IllegalArgumentException("Can't verify user"),
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            List.of(),
            HttpStatus.BAD_REQUEST,
            "Wohnung nicht gefunden"
        )
    );
  }

  private static Stream<Arguments> provideUpdateMeterReading() {
    return Stream.of(
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 123456),
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            Optional.of(Meter.builder().id(1).meterNr("12345").type(
                MeterType.GAS).reading(98765).costPerUnit(0.48).build()),
            Update.builder()
                .meter(Meter.builder().id(1).meterNr("12345").type(MeterType.GAS).reading(98765)
                    .costPerUnit(0.48).build()).person(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .reading(12345).build(),
            HttpStatus.OK,
            "Zählerstand aktualisiert"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.empty(),
            null,
            null,
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            new JwtException("Can't verify user"),
            null,
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            new IllegalArgumentException("Can't verify user"),
            null,
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.empty(),
            null,
            null,
            HttpStatus.BAD_REQUEST,
            "Wohnung nicht gefunden"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 123345),
            Optional.of(
                Person.builder().id(2).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Du bist nicht der Mieter dieser Wohnung"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            Optional.empty(),
            null,
            HttpStatus.BAD_REQUEST,
            "Zähler nicht gefunden"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of()).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            Optional.of(Meter.builder().id(1).meterNr("12345").type(
                MeterType.GAS).reading(98765).costPerUnit(0.48).build()),
            null,
            HttpStatus.BAD_REQUEST,
            "Zähler nicht in Wohnung gefunden"
        ),
        Arguments.of(
            "token",
            new UpdateMeterReadingDto(1, 1, 12345),
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            Optional.of(Meter.builder().id(1).meterNr("12345").type(
                MeterType.GAS).reading(98765).costPerUnit(0.48).build()),
            null,
            HttpStatus.BAD_REQUEST,
            "Neuer Zählerstand ist niedriger als der aktuelle Zählerstand"
        )
    );

  }

  public static Stream<Arguments> provideGetLandlordByFlat() {
    return Stream.of(
        Arguments.of(
            "token",
            1,
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            Optional.of(Building.builder().landlord(
                    Person.builder().phoneNumber("+49000000").email("Landlord@t.de").gender(Gender.MALE)
                        .roleList(List.of(Role.LANDLORD)).firstName("Land").lastName("Lord").address(
                            Address.builder().city("city").zip(12345).state("state").country("country")
                                .street("street").build()
                        ).build())
                .flatList(List.of()).build()),
            HttpStatus.OK,
            new LandlordViewPersonDto("Land", "Lord", Gender.MALE, "+49000000",
                "Landlord@t.de", new AddressDto("street", 12345, "city", "country", "state"))
        ),
        Arguments.of(
            "token",
            1,
            Optional.empty(),
            null,
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            1,
            Optional.empty(),
            new JwtException("Can't verify user"),
            null,
            null,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            1,
            Optional.of(
                Person.builder().phoneNumber("+49000000").email("Tenant@t.de").gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.empty(),
            null,
            HttpStatus.BAD_REQUEST,
            "Wohnung nicht gefunden"
        ),
        Arguments.of(
            "token",
            1,
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(8384).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            null,
            HttpStatus.UNAUTHORIZED,
            "Du bist nicht der Mieter dieser Wohnung"
        ),
        Arguments.of(
            "token",
            1,
            Optional.of(
                Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                    .gender(Gender.MALE)
                    .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build()),
            null,
            Optional.of(Flat.builder().id(1).tenant(
                    Person.builder().id(1).phoneNumber("+49000000").email("Tenant@t.de")
                        .gender(Gender.MALE)
                        .roleList(List.of(Role.TENANT)).firstName("Ten").lastName("Ant").build())
                .meterList(List.of(Meter.builder().id(1).meterNr("12345").type(
                    MeterType.GAS).reading(98765).costPerUnit(0.48).build())).location("1.OG")
                .rooms(3).squareMeter(100).residents(3).coldRent(300).warmRent(500)
                .additionList(List.of(
                    AdditionalCost.builder().amount(30).frequency(Frequency.MONTHLY)
                        .distribution(
                            Distribution.NONE).description("Garage").id(1).build())).build()),
            Optional.empty(),
            HttpStatus.BAD_REQUEST,
            "Gebäude nicht gefunden"
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideUpdateTenant")
  void updateTenant(ModifyFlatDto modifyFlatDto,
      String token,
      Optional<Person> landlordOptional,
      Optional<Building> buildingOptional,
      HttpStatus expectedHttpStatus,
      String expectedMessage,
      Exception expectedException,
      Optional<Person> tenantOptional,
      Person tenant
  ) {

    if (expectedException != null) {
      when(userUtil.verifyUser(anyString())).thenThrow(expectedException);
    } else {
      when(userUtil.verifyUser(anyString())).thenReturn(landlordOptional);
    }
    lenient().when(buildingRepository.findById(anyInt())).thenReturn(buildingOptional);
    lenient().when(personRepository.findByEmail(anyString())).thenReturn(tenantOptional);
    if (tenantOptional == null) {
      lenient().when(personRepository.saveAndFlush(any())).thenReturn(tenant);
    } else {
      lenient().when(personRepository.saveAndFlush(any())).thenReturn(tenantOptional);
    }

    var response = flatService.updateTenant(modifyFlatDto, token);

    assertEquals(expectedHttpStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

  @ParameterizedTest
  @MethodSource("provideGetFlat")
  void getFlat(String token,
      Optional<Person> personOptional,
      Exception exception,
      List<Flat> flatList,
      HttpStatus expectedHttpStatus,
      Object expectedMessage) {

    if (exception != null) {
      when(userUtil.verifyUser(anyString())).thenThrow(exception);
    } else {
      when(userUtil.verifyUser(anyString())).thenReturn(personOptional);
    }
    lenient().when(flatRepository.findByTenant(any())).thenReturn(flatList);

    var response = flatService.getFlat(token);

    assertEquals(expectedHttpStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

  @ParameterizedTest
  @MethodSource("provideUpdateMeterReading")
  void updateMeterReading(String token,
      UpdateMeterReadingDto updateMeterReadingDto,
      Optional<Person> personOptional,
      Exception exception,
      Optional<Flat> flatOptional,
      Optional<Meter> meterOptional,
      Update update,
      HttpStatus expectedHttpStatus,
      String expectedMessage) {

    if (exception != null) {
      when(userUtil.verifyUser(anyString())).thenThrow(exception);
    } else {
      when(userUtil.verifyUser(anyString())).thenReturn(personOptional);
    }
    lenient().when(flatRepository.findById(anyInt())).thenReturn(flatOptional);
    lenient().when(meterRepository.findById(anyInt())).thenReturn(meterOptional);
    lenient().when(updateRepository.saveAndFlush(any())).thenReturn(update);

    var response = flatService.updateMeterReading(updateMeterReadingDto, token);

    assertEquals(expectedHttpStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

  @ParameterizedTest
  @MethodSource("provideGetLandlordByFlat")
  void getLandlordByFlat(String token,
      int flatId,
      Optional<Person> personOptional,
      Exception exception,
      Optional<Flat> flatOptional,
      Optional<Building> buildingOptional,
      HttpStatus expectedHttpStatus,
      Object expectedMessage) {

    if (exception != null) {
      when(userUtil.verifyUser(anyString())).thenThrow(exception);
    } else {
      when(userUtil.verifyUser(anyString())).thenReturn(personOptional);
    }
    lenient().when(flatRepository.findById(anyInt())).thenReturn(flatOptional);
    lenient().when(buildingRepository.findByFlatListContaining(any())).thenReturn(buildingOptional);

    var response = flatService.getLandlordByFlat(flatId, token);

    assertEquals(expectedHttpStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());
  }

}