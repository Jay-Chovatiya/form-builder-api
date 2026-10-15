package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fields")
@Getter
@Setter
@NoArgsConstructor
public class FormFieldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormSectionId", nullable = false)
    private FormSectionEntity formSection;

    @Column(name = "Name", nullable = false, length = 100)
    private String name;

    @Column(name = "Type", nullable = false, length = 50)
    private String type;

    @Column(name = "Label", nullable = false, length = 200)
    private String label;

    @Column(name = "HelperDescription", columnDefinition = "TEXT")
    private String helperDescription;

    @Column(name = "Visibility", nullable = false)
    private boolean visibility = true;

    @Column(name = "Placeholder", length = 255)
    private String placeholder;

    @Column(name = "DefaultValue", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "Icon", length = 100)
    private String icon;

    @Column(name = "DisplayOrder", nullable = false)
    private int displayOrder = 0;

    @Column(name = "Multiple", nullable = false)
    private boolean multiple = false;

    @Column(name = "MaxFiles")
    private Integer maxFiles;

    @Column(name = "MaxSizeInBytes", nullable = false)
    private long maxSizeInBytes = 0;

    @Column(name = "IsDeleted", nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "formField", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<FieldOptionEntity> options = new ArrayList<>();

    @OneToMany(mappedBy = "formField", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FieldValidationEntity> validations = new ArrayList<>();

    @OneToMany(mappedBy = "formField", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileTypeConfigEntity> allowedTypes = new ArrayList<>();
}
