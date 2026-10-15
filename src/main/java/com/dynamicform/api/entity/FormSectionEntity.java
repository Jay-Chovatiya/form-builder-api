package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
public class FormSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormDefinitionId", nullable = false)
    private FormEntity form;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "Theme", length = 50)
    private String theme;

    @Column(name = "Visibility", nullable = false)
    private boolean visibility = true;

    @Column(name = "DisplayOrder", nullable = false)
    private int displayOrder = 0;

    @Column(name = "IsDeleted", nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "formSection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<FormFieldEntity> fields = new ArrayList<>();
}
