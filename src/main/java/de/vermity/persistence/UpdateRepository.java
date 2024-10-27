package de.vermity.persistence;

import de.vermity.model.entity.Update;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the entity Update.
 *
 * @author Cedric Stumpf
 */
public interface UpdateRepository extends JpaRepository<Update, Integer> {

}