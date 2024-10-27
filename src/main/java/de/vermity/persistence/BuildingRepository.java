package de.vermity.persistence;

import de.vermity.model.entity.Building;
import de.vermity.model.entity.Flat;
import de.vermity.model.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the Building entity.
 *
 * @author Cedric Stumpf
 */
public interface BuildingRepository extends JpaRepository<Building, Integer> {

  Optional<Building> findByAddress_StreetAndAddress_CityAndAddress_StateAndAddress_ZipAndAddress_Country(
      String street, String city, String state, int zip, String country);

  List<Building> findByLandlord(Person landlord);

  Optional<Building> findByFlatListContaining(Flat flat);
}