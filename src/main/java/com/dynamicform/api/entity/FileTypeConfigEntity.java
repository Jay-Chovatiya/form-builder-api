package com.dynamicform.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_type_configs")
@Getter
@Setter
@NoArgsConstructor
public class FileTypeConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FormFieldId", nullable = false)
    private FormFieldEntity formField;

    @Column(name = "Extension", nullable = false, length = 50)
    private String extension;

    @Column(name = "MimeType", nullable = false, length = 100)
    private String mimeType;
}
