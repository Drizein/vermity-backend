package de.vermity.model.dto;

import java.io.Serializable;

/**
 * @param paid
 * @param pdf
 * @author Jan Tiedt
 */
public record InvoiceDto(
    int invoiceId,
    Boolean paid,
    String pdf
)
    implements Serializable {

}
