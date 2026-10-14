package com.dynamicform.api.modules.submission.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "form_responses", indexes = {
        @Index(name = "idx_response_submission", columnList = "form_submission_id"),
        @Index(name = "idx_response_field", columnList = "field_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_submission_id", nullable = false)
    private FormSubmissionEntity formSubmission;

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "value_json", columnDefinition = "TEXT")
    private String valueJson;
}
