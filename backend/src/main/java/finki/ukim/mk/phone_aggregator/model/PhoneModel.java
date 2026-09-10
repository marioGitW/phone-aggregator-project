package finki.ukim.mk.phone_aggregator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A canonical phone device (brand + model + storage/RAM variant), independent of which
 * sites sell it. Distinct {@link Offer} rows from different sources point at the same
 * PhoneModel when they represent the same device.
 */
@Entity
@Table(name = "phone_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(name = "model_key", nullable = false, columnDefinition = "TEXT")
    private String modelKey;

    @Column(name = "storage_gb")
    private Integer storageGb;

    @Column(name = "ram_gb")
    private Integer ramGb;

    @Column(name = "model_code")
    private String modelCode;

    @Column(name = "display_name", nullable = false, columnDefinition = "TEXT")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
