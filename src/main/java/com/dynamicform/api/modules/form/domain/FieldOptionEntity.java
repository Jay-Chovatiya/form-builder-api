package com.dynamicform.api.modules.form.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "field_options", indexes = {
        @Index(name = "idx_option_field", columnList = "form_field_id"),
        @Index(name = "idx_option_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_field_id", nullable = false)
    private FormFieldEntity formField;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "value", nullable = false, length = 200)
    private String value;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;
}
