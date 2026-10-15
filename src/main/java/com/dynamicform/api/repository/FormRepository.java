package com.dynamicform.api.repository;

import com.dynamicform.api.entity.FormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormRepository extends JpaRepository<FormEntity, Long> {

    Optional<FormEntity> findByCode(String code);
}
