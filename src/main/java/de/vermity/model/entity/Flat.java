package de.vermity.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Entity for a flat
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
public class Flat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne
    private Person tenant;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Meter> meterList = new ArrayList<>();

    private String location;
    private int    squareMeter;
    private int    rooms;
    private int    residents;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    @Builder.Default
    private List<AdditionalCost> additionList = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Invoice> invoiceList = new ArrayList<>();

    private double coldRent;
    private double warmRent;

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
        Flat flat = (Flat) o;
        return getId() != null && Objects.equals(getId(), flat.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
                                                .getPersistentClass().hashCode() : getClass().hashCode();
    }
}
