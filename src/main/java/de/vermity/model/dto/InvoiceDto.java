package de.vermity.model.dto;

import java.io.Serializable;

/**
 * @param paid
 * @param pdf
 * @author Drizein
 */
public record InvoiceDto(
        int invoiceId,
        Boolean paid,
        String pdf
)
        implements Serializable {

}
