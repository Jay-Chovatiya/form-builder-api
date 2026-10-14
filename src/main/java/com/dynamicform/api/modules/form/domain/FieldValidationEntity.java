package com.dynamicform.api.modules.form.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "field_validations", indexes = {
        @Index(name = "idx_validation_field", columnList = "form_field_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_field_id", nullable = false)
    private FormFieldEntity formField;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "val", length = 255)
    private String value;
}
