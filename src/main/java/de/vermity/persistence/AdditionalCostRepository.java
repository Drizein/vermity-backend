package de.vermity.persistence;

import de.vermity.model.entity.AdditionalCost;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Cedric Stumpf
 */
public interface AdditionalCostRepository extends JpaRepository<AdditionalCost, Integer> {

}
