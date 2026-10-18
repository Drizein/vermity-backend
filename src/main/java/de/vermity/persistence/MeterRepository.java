package de.vermity.persistence;

import de.vermity.model.entity.Meter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for the entity Meter.
 *
 * @author Drizein
 */
public interface MeterRepository extends JpaRepository<Meter, Integer> {

    List<Meter> findByMeterNrIn(List<String> meterNrList);
}