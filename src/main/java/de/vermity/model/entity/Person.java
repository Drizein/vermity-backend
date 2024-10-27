package de.vermity.model.entity;

import de.vermity.util.enums.Gender;
import de.vermity.util.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.proxy.HibernateProxy;

/**
 * Entity for a person
 *
 * @author Cedric Stumpf
 */
@Entity
@Builder
@AllArgsConstructor
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Person extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  private String firstName;
  private String lastName;
  private Gender gender;
  private String phoneNumber;
  private String email;
  private LocalDate birthDate;
  @Column(length = 60)
  private String password;

  @Builder.Default
  private boolean enabled = false;

  @ManyToOne
  private Address address;

  @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private List<Role> roleList = new ArrayList<>();

  @OneToMany(mappedBy = "tenant")
  @Default
  @Exclude
  private List<Flat> flatList = new ArrayList<>();

  @OneToMany(mappedBy = "person")
  @Default
  @Exclude
  private List<Update> updates = new ArrayList<>();

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Class<?> oEffectiveClass = o instanceof HibernateProxy
        ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
        : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy
        ? ((HibernateProxy) this).getHibernateLazyInitializer()
        .getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) {
      return false;
    }
    Person person = (Person) o;
    return getId() != null && Objects.equals(getId(), person.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
        .getPersistentClass().hashCode() : getClass().hashCode();
  }
}
