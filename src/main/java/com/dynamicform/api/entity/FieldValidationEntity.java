package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "field_validations")
@Getter
@Setter
@NoArgsConstructor
public class FieldValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormFieldId", nullable = false)
    private FormFieldEntity formField;

    @Column(name = "Type", nullable = false, length = 50)
    private String type;

    @Column(name = "Value", length = 255)
    private String value;
}
