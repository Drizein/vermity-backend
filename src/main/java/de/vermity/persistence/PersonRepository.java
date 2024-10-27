package de.vermity.persistence;

import de.vermity.model.entity.Person;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Cedric Stumpf
 */
public interface PersonRepository extends JpaRepository<Person, Integer> {

  Optional<Person> findByEmail(String email);
}