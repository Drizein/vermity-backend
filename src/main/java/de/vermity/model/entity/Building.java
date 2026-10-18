package de.vermity.model.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;
import lombok.ToString.Exclude;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Entity for a building
 *
 * @author Drizein
 */
@Entity
@Builder
@AllArgsConstructor
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Building extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @OneToMany(cascade = CascadeType.REMOVE)
    @Default
    @Exclude
    private List<Flat> flatList = new ArrayList<>();

    @ManyToOne
    private Person landlord;

    @OneToMany(cascade = CascadeType.REMOVE)
    @Default
    @Exclude
    private List<AdditionalCost> operatingCosts = new ArrayList<>();

    @OneToOne
    private Address address;

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
        Building building = (Building) o;
        return getId() != null && Objects.equals(getId(), building.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                                                .getPersistentClass().hashCode() : getClass().hashCode();
    }
}
