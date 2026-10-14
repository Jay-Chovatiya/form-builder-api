package com.dynamicform.api.modules.form.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_type_configs", indexes = {
        @Index(name = "idx_file_config_field", columnList = "form_field_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTypeConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_field_id", nullable = false)
    private FormFieldEntity formField;

    @Column(name = "extension", nullable = false, length = 50)
    private String extension;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
}
