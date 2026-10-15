package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_submissions")
@Getter
@Setter
@NoArgsConstructor
public class FormSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormId", nullable = false)
    private FormEntity form;

    @Column(name = "SubmittedAt", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormResponseEntity> responses = new ArrayList<>();
}
