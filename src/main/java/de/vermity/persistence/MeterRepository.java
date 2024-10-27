package de.vermity.persistence;

import de.vermity.model.entity.Meter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the entity Meter.
 *
 * @author Cedric Stumpf
 */
public interface MeterRepository extends JpaRepository<Meter, Integer> {

  List<Meter> findByMeterNrIn(List<String> meterNrList);
}