package de.vermity.persistence;

import de.vermity.model.entity.Address;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Cedric Stumpf
 */
public interface AddressRepository extends JpaRepository<Address, Integer> {

  Optional<Address> findByStateAndCityAndStreetAndZipAndCountry(String state, String city,
      String street, int zip, String country);
}