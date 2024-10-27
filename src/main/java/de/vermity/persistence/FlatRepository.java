package de.vermity.persistence;

import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Person;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * Repository for the entity Flat.
 *
 * @author Cedric Stumpf
 */
public interface FlatRepository extends JpaRepository<Flat, Integer> {

  List<Flat> findByTenant(Person tenant);
}