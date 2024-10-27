package de.vermity.model.dto;

import java.io.Serializable;

public record GetInvoiceDto(
    int invoiceId,
    String pdf,
    int buildingId,
    int flatId,
    boolean paidStatus
) implements Serializable {

}
