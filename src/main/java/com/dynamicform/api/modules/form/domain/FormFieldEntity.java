package com.dynamicform.api.modules.form.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_fields", indexes = {
        @Index(name = "idx_field_section", columnList = "form_section_id"),
        @Index(name = "idx_field_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_section_id", nullable = false)
    private FormSectionEntity formSection;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "helper_description", columnDefinition = "TEXT")
    private String helperDescription;

    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private boolean visibility = true;

    @Column(name = "placeholder", length = 255)
    private String placeholder;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "multiple", nullable = false)
    @Builder.Default
    private boolean multiple = false;

    @Column(name = "max_files")
    private Integer maxFiles;

    @Column(name = "max_size_in_bytes", nullable = false)
    @Builder.Default
    private long maxSizeInBytes = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "formField", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FieldOptionEntity> options = new ArrayList<>();

    @OneToMany(mappedBy = "formField", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FieldValidationEntity> validations = new ArrayList<>();

    @OneToMany(mappedBy = "formField", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileTypeConfigEntity> allowedTypes = new ArrayList<>();

    // public void addOption(FieldOptionEntity option) {
    //     options.add(option);
    //     option.setFormField(this);
    // }

    // public void removeOption(FieldOptionEntity option) {
    //     options.remove(option);
    //     option.setFormField(null);
    // }

    // public void addValidation(FieldValidationEntity validation) {
    //     validations.add(validation);
    //     validation.setFormField(this);
    // }

    // public void removeValidation(FieldValidationEntity validation) {
    //     validations.remove(validation);
    //     validation.setFormField(null);
    // }

    // public void addAllowedType(FileTypeConfigEntity allowedType) {
    //     allowedTypes.add(allowedType);
    //     allowedType.setFormField(this);
    // }

    // public void removeAllowedType(FileTypeConfigEntity allowedType) {
    //     allowedTypes.remove(allowedType);
    //     allowedType.setFormField(null);
    // }
}
