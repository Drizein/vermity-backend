package de.vermity.persistence;

import de.vermity.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Drizein
 */
public interface AddressRepository extends JpaRepository<Address, Integer> {

    Optional<Address> findByStateAndCityAndStreetAndZipAndCountry(String state, String city,
                                                                  String street, int zip, String country);
}