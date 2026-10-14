package com.dynamicform.api.modules.form.domain;

import com.dynamicform.api.common.audit.BaseAuditableEntity;
import com.dynamicform.api.modules.submission.domain.FormSubmissionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_definitions", indexes = {
        @Index(name = "idx_form_code", columnList = "code", unique = true),
        @Index(name = "idx_form_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormDefinitionEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "allow_multiple_submissions", nullable = false)
    @Builder.Default
    private boolean allowMultipleSubmissions = true;

    @Column(name = "allow_save_as_draft", nullable = false)
    @Builder.Default
    private boolean allowSaveAsDraft = true;

    @Column(name = "confirmation_message", columnDefinition = "TEXT")
    private String confirmationMessage;

    @Column(name = "submit_button_text", length = 50)
    private String submitButtonText;

    @Column(name = "cancel_button_text", length = 50)
    private String cancelButtonText;

    @Column(name = "theme", length = 50)
    private String theme;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "header_text", length = 255)
    private String headerText;

    @Column(name = "footer_text", length = 255)
    private String footerText;

    @OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FormSectionEntity> sections = new ArrayList<>();

    @OneToMany(mappedBy = "formDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormSubmissionEntity> submissions = new ArrayList<>();

    // public void addSection(FormSectionEntity section) {
    //     sections.add(section);
    //     section.setFormDefinition(this);
    // }

    // public void removeSection(FormSectionEntity section) {
    //     sections.remove(section);
    //     section.setFormDefinition(null);
    // }
}
