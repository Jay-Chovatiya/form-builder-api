package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_responses")
@Getter
@Setter
@NoArgsConstructor
public class FormResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormSubmissionId", nullable = false)
    private FormSubmissionEntity formSubmission;

    @Column(name = "FieldId", nullable = false)
    private Long fieldId;

    @Column(name = "ValueJson", columnDefinition = "TEXT")
    private String valueJson;
}
