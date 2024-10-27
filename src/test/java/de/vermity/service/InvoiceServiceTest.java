package de.vermity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import de.vermity.model.entity.Address;
import de.vermity.model.entity.Building;
import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Invoice;
import de.vermity.model.entity.Person;
import de.vermity.persistence.BuildingRepository;
import de.vermity.persistence.FlatRepository;
import de.vermity.persistence.InvoiceRepository;
import de.vermity.security.JWTGenerator;
import de.vermity.util.UserUtil;
import de.vermity.util.enums.Gender;
import io.jsonwebtoken.JwtException;
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
 * Test class for {@link InvoiceService}.
 *
 * @author Cedric Stumpf
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

  @Mock
  private UserUtil userUtil;

  @Mock
  private BuildingRepository buildingRepository;

  @Mock
  private JWTGenerator jwtGenerator;

  @Mock
  private InvoiceRepository invoiceRepository;

  @Mock
  private FlatRepository flatRepository;

  @InjectMocks
  private InvoiceService invoiceService;

  public static Stream<Arguments> provideInvoiceData() {
    return Stream.of(
        Arguments.of(
            "token",
            Optional.empty(),
            Optional.empty(),
            null,
            null,
            null,
            0,
            0,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            Optional.empty(),
            Optional.empty(),
            new JwtException("Invalid token"),
            null,
            null,
            0,
            0,
            HttpStatus.UNAUTHORIZED,
            "Bitte melde dich zuerst an!"
        ),
        Arguments.of(
            "token",
            Optional.of(Person.builder()
                .id(1)
                .email("Landlord@email.com")
                .gender(Gender.FEMALE)
                .phoneNumber("+49123456789")
                .firstName("female")
                .lastName("Landlord")
                .address(
                    Address.builder()
                        .street("Landlord street 1A")
                        .zip(12345)
                        .city("Landlord city")
                        .country("Landlord country")
                        .build()
                )
                .build()),
            Optional.empty(),
            null,
            Optional.empty(),
            Optional.empty(),
            0,
            0,
            HttpStatus.BAD_REQUEST,
            "Wohnung nicht gefunden"
        ),
        Arguments.of(
            "token",
            Optional.of(Person.builder()
                .id(1)
                .email("Landlord@email.com")
                .gender(Gender.FEMALE)
                .phoneNumber("+49123456789")
                .firstName("female")
                .lastName("Landlord")
                .address(
                    Address.builder()
                        .street("Landlord street 1A")
                        .zip(12345)
                        .city("Landlord city")
                        .country("Landlord country")
                        .build()
                )
                .build()),
            Optional.empty(),
            null,
            Optional.empty(),
            Optional.of(
                Flat.builder()
                    .tenant(Person.builder().build())
                    .build()
            ),
            0,
            0,
            HttpStatus.BAD_REQUEST,
            "Gebäude nicht gefunden"
        ),
        Arguments.of(
            "token",
            Optional.of(Person.builder()
                .id(1)
                .email("Landlord@email.com")
                .gender(Gender.FEMALE)
                .phoneNumber("+49123456789")
                .firstName("female")
                .lastName("Landlord")
                .address(
                    Address.builder()
                        .street("Landlord street 1A")
                        .zip(12345)
                        .city("Landlord city")
                        .country("Landlord country")
                        .build()
                )
                .build()),
            Optional.empty(),
            null,
            Optional.empty(),
            Optional.of(Flat.builder().build()),
            0,
            0,
            HttpStatus.BAD_REQUEST,
            "Keinen Mieter gefunden"
        )
    );
  }

  @ParameterizedTest
  @MethodSource("provideInvoiceData")
  void testCreateInvoiceInvalid(
      String token,
      Optional<Person> landlordOptional,
      Optional<Invoice> invoiceOptional,
      Exception expectedException,
      Optional<Building> buildingOptional,
      Optional<Flat> flatOptional,
      int flatId,
      double totalRentPaid,
      HttpStatus expectedStatus,
      String expectedMessage
  ) {
    if (expectedException != null) {
      lenient().when(userUtil.verifyUser(anyString())).thenThrow(expectedException);
    } else {
      lenient().when(userUtil.verifyUser(anyString())).thenReturn(landlordOptional);
    }
    lenient().when(flatRepository.findById(any())).thenReturn(flatOptional);
    lenient().when(buildingRepository.findByFlatListContaining(any())).thenReturn(buildingOptional);
    lenient().when(invoiceRepository.saveAndFlush(any())).thenReturn(invoiceOptional);

    var response = invoiceService.createInvoice(token, flatId, totalRentPaid);

    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedMessage, response.getBody());

  }
}