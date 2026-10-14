package com.dynamicform.api.modules.form.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_sections", indexes = {
        @Index(name = "idx_section_form", columnList = "form_definition_id"),
        @Index(name = "idx_section_order", columnList = "display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_definition_id", nullable = false)
    private FormDefinitionEntity formDefinition;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "theme", length = 50)
    private String theme;

    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private boolean visibility = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "formSection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FormFieldEntity> fields = new ArrayList<>();

    // public void addField(FormFieldEntity field) {
    //     fields.add(field);
    //     field.setFormSection(this);
    // }

    // public void removeField(FormFieldEntity field) {
    //     fields.remove(field);
    //     field.setFormSection(null);
    // }
}
