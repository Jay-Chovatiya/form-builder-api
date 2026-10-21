package com.dynamicform.api.entity;

import com.dynamicform.api.common.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forms")
@Getter
@Setter
@NoArgsConstructor
public class FormEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "Name", nullable = false, length = 200)
    private String name;

    @Column(name = "Code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "Category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 30)
    private FormStatus status = FormStatus.Draft;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @Column(name = "AllowMultipleSubmissions", nullable = false)
    private boolean allowMultipleSubmissions = true;

    @Column(name = "AllowSaveAsDraft", nullable = false)
    private boolean allowSaveAsDraft = true;

    @Column(name = "ConfirmationMessage", columnDefinition = "TEXT")
    private String confirmationMessage;

    @Column(name = "SubmitButtonText", length = 50)
    private String submitButtonText;

    @Column(name = "CancelButtonText", length = 50)
    private String cancelButtonText;

    @Column(name = "Theme", length = 50)
    private String theme;

    @Column(name = "LogoUrl", length = 500)
    private String logoUrl;

    @Column(name = "HeaderText", length = 255)
    private String headerText;

    @Column(name = "FooterText", length = 255)
    private String footerText;

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<FormSectionEntity> sections = new ArrayList<>();

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormSubmissionEntity> submissions = new ArrayList<>();
}
