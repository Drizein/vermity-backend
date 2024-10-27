package de.vermity.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import de.vermity.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(name = "Invoice", description = "handles all invoice related stuff")
@RequiredArgsConstructor
@RequestMapping("/auth")
public class InvoiceController {

  private final InvoiceService invoiceService;

  /**
   * @return Error or success messages
   * @author Cedric Stumpf
   */
  @PostMapping(value = "createInvoice",consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @Operation(summary = "create invoice per flat")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Invoice created"),
      @ApiResponse(responseCode = "400", description = "flat not found"),
      @ApiResponse(responseCode = "401", description = "Not authorized")

  })
  ResponseEntity<?> createInvoice(@RequestHeader String Authorization, @RequestHeader int flatId, @RequestHeader double totalRentPaid) {
    return invoiceService.createInvoice(Authorization, flatId, totalRentPaid);
  }

  /**
   * @return Error messages or all invoices for all buildings
   * @author Cedric Stumpf
   */
  @GetMapping(value = "getAllInvoicesForAllBuildings")
  @Operation(summary = "get all invoices per building, only for landlord")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "202", description = "Invoices found"),
      @ApiResponse(responseCode = "400", description = "Building, Invoices found"),
      @ApiResponse(responseCode = "401", description = "Not authorized")

  })
  ResponseEntity<?> getAllInvoicesForAllBuildings(@RequestHeader String Authorization) {
    return invoiceService.getAllInvoicesForAllBuildings(Authorization);
  }

  /**
   * @return Error messages or all invoices for a flat
   * @author Cedric Stumpf
   */
  @GetMapping(value = "getAllInvoicesForFlat")
  @Operation(summary = "get all invoices per flat, only for tenant")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Invoices found"),
      @ApiResponse(responseCode = "400", description = "Flat not found"),
      @ApiResponse(responseCode = "401", description = "Not authorized")

  })
  ResponseEntity<?> getAllInvoicesForFlat(@RequestHeader String Authorization) {
    return invoiceService.getAllInvoicesForFlat(Authorization);
  }

  /**
   * @return Error or success messages with updated invoice paid status
   * @author Jan Tiedt
   */
  @GetMapping(value = "updateInvoicePaidStatus")
  @Operation(summary = "update invoice paid status. 1 = paid, 0 = not paid")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Invoice updated"),
      @ApiResponse(responseCode = "400", description = "Building, Invoice or Invoice not in building/flat"),
      @ApiResponse(responseCode = "401", description = "Invalid user")

  })
  ResponseEntity<?> updateInvoicePaidStatus(@RequestHeader int invoiceId,
      @RequestHeader int buildingId, @RequestHeader String Authorization) {
    return invoiceService.updateInvoicePaidStatus(invoiceId, buildingId, Authorization);
  }
}
