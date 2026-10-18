package de.vermity.model.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * @author Drizein
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
