package com.dynamicform.api.repository;

import com.dynamicform.api.entity.FormSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmissionEntity, Long> {

    List<FormSubmissionEntity> findByFormIdOrderBySubmittedAtDesc(Long formId);

    Optional<FormSubmissionEntity> findByIdAndFormId(Long id, Long formId);


    boolean existsByFormId(Long formId);
}
