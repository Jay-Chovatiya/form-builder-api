package com.dynamicform.api.modules.submission.domain;

import com.dynamicform.api.modules.form.domain.FormDefinitionEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_submissions", indexes = {
        @Index(name = "idx_submission_form", columnList = "form_definition_id"),
        @Index(name = "idx_submission_date", columnList = "submitted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private FormDefinitionEntity formDefinition;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "formSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormResponseEntity> responses = new ArrayList<>();

    // public void addResponse(FormResponseEntity response) {
    //     responses.add(response);
    //     response.setFormSubmission(this);
    // }

    // public void removeResponse(FormResponseEntity response) {
    //     responses.remove(response);
    //     response.setFormSubmission(null);
    // }
}
