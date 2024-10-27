package de.vermity.persistence;

import de.vermity.model.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the entity Invoice.
 *
 * @author Cedric Stumpf
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

}