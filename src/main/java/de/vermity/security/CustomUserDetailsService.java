package de.vermity.security;

import de.vermity.persistence.PersonRepository;
import de.vermity.util.enums.Role;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService for Spring Security. This class is used to load a user by its email.
 * The user is then mapped to a UserDetails object. The UserDetails object is used by Spring
 * Security to authenticate the user.
 *
 * @author Cedric Stumpf
 * @see UserDetailsService
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final PersonRepository personRepository;


  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var user = personRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Email not found"));
    return new User(user.getEmail(), user.getPassword(), mapRolesToAuthorities(user.getRoleList()));
  }

  private Collection<GrantedAuthority> mapRolesToAuthorities(List<Role> roles) {
    return roles.stream().map(role -> new SimpleGrantedAuthority(role.name()))
        .collect(Collectors.toList());
  }
}
