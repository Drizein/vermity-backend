package de.vermity.persistence;

import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * Repository for the entity Flat.
 *
 * @author Drizein
 */
public interface FlatRepository extends JpaRepository<Flat, Integer> {

    List<Flat> findByTenant(Person tenant);
}