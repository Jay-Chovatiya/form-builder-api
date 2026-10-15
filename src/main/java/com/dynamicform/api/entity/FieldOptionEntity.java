package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "field_options")
@Getter
@Setter
@NoArgsConstructor
public class FieldOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormFieldId", nullable = false)
    private FormFieldEntity formField;

    @Column(name = "Label", nullable = false, length = 200)
    private String label;

    @Column(name = "Value", nullable = false, length = 200)
    private String value;

    @Column(name = "DisplayOrder", nullable = false)
    private int displayOrder = 0;
}
