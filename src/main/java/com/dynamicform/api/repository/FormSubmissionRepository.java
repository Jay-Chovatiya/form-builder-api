package com.dynamicform.api.repository;

import com.dynamicform.api.entity.FormSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmissionEntity, Long> {

    boolean existsByFormId(Long formId);
}
