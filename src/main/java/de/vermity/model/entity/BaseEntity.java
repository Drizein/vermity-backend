package de.vermity.model.entity;

import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author Cedric Stumpf
 */
@Getter
@MappedSuperclass
public class BaseEntity {

  private LocalDateTime createdAt = LocalDateTime.now();
  @UpdateTimestamp
  private LocalDateTime updatedAt;


  public void setUpdatedAt() {
    this.updatedAt = LocalDateTime.now();
  }

}
