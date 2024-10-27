package de.vermity.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;


/**
 * Entity for an invoice
 *
 * @author Jan Tiedt
 */
@Entity
@Builder
@AllArgsConstructor
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Invoice extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Integer id;

  @ManyToOne
  private Flat flat;

  @ManyToOne
  private Building building;

  @Builder.Default
  private boolean paid = false;

  @ElementCollection
  private Map<Meter, Integer> meterDifference;

  @ElementCollection
  private Map<Meter, Double> meterTotalCost;

  @ElementCollection
  private Map<AdditionalCost, Double> operatingCostPerDistributionKey;

  private double totalWarmRentPaid;
  private double totalColdRent;
  private double totalCost;
  private int totalSquareMeters;
  private int invoiceForYear;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String pdf;

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
    Invoice invoice = (Invoice) o;
    return getId() != null && Objects.equals(getId(), invoice.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer()
        .getPersistentClass().hashCode() : getClass().hashCode();
  }
}